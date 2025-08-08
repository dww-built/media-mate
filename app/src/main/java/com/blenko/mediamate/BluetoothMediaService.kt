package com.blenko.mediamate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BluetoothMediaService : Service() {
    
    companion object {
        private const val TAG = "MediaService"
        private const val CHANNEL_ID = "MediaMateService"
        private const val NOTIFICATION_ID = 1
        private const val RECONNECT_DELAY_MS = 5000L
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var mediaController: BluetoothMediaController? = null
    private var targetDevice: BluetoothDevice? = null
    private var isAutoReconnecting = false
    
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device?.address == targetDevice?.address && !isAutoReconnecting) {
                        Log.d(TAG, "Target device disconnected, starting auto-reconnect")
                        startAutoReconnect()
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_ON && targetDevice != null && !isAutoReconnecting) {
                        Log.d(TAG, "Bluetooth turned on, attempting reconnection")
                        startAutoReconnect()
                    }
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        mediaController = BluetoothMediaController(this, bluetoothAdapter!!)
        
        // Register receiver for Bluetooth events
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        registerReceiver(bluetoothReceiver, filter)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        
        // Find target device from paired devices
        findTargetDevice()
        
        // Start initial connection if device is found
        targetDevice?.let {
            serviceScope.launch {
                delay(1000) // Small delay to ensure everything is initialized
                attemptConnection()
            }
        }
        
        return START_STICKY
    }
    
    private fun findTargetDevice() {
        try {
            val pairedDevices = bluetoothAdapter?.bondedDevices
            targetDevice = pairedDevices?.find { 
                it.name?.contains("Pixel", ignoreCase = true) == true 
            }
            Log.d(TAG, "Target device found: ${targetDevice?.name}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth permission", e)
        }
    }
    
    private fun startAutoReconnect() {
        if (isAutoReconnecting) return
        
        isAutoReconnecting = true
        serviceScope.launch {
            var attempts = 0
            val maxAttempts = 10
            
            while (isAutoReconnecting && attempts < maxAttempts) {
                attempts++
                Log.d(TAG, "Auto-reconnect attempt $attempts/$maxAttempts")
                
                if (attemptConnection()) {
                    Log.d(TAG, "Reconnection successful")
                    isAutoReconnecting = false
                    updateNotification("Connected to ${targetDevice?.name}")
                    break
                }
                
                delay(RECONNECT_DELAY_MS)
            }
            
            if (attempts >= maxAttempts) {
                Log.w(TAG, "Max reconnection attempts reached")
                updateNotification("Reconnection failed - open app to retry")
                isAutoReconnecting = false
            }
        }
    }
    
    private suspend fun attemptConnection(): Boolean {
        targetDevice?.let { device ->
            return mediaController?.connectToDevice(device) ?: false
        }
        return false
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Mate Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains Bluetooth connection for media control"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(contentText: String = "Ready to control media"): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Media Mate")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(text))
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        isAutoReconnecting = false
        mediaController?.disconnect()
        unregisterReceiver(bluetoothReceiver)
    }
}
package com.blenko.mediamate

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MediaMate"
        private const val REQUEST_PERMISSIONS = 1001
        private const val WIFE_DEVICE_NAME = "Wife's Pixel"  // Configure this
    }
    
    private lateinit var bluetoothManager: BluetoothManager
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var mediaController: BluetoothMediaController? = null
    private var targetDevice: BluetoothDevice? = null
    
    // UI Elements
    private lateinit var statusText: TextView
    private lateinit var deviceNameText: TextView
    private lateinit var connectionStatusText: TextView
    private lateinit var btnPlayPause: Button
    private lateinit var btnPrevious: Button
    private lateinit var btnNext: Button
    private lateinit var btnVolumeUp: Button
    private lateinit var btnVolumeDown: Button
    private lateinit var btnConnect: Button
    
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device?.address == targetDevice?.address) {
                        updateConnectionStatus(true)
                        Log.d(TAG, "Device connected: ${device.name}")
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device?.address == targetDevice?.address) {
                        updateConnectionStatus(false)
                        Log.d(TAG, "Device disconnected: ${device.name}")
                        // Auto-reconnect
                        attemptReconnection()
                    }
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        initializeBluetooth()
        checkPermissions()
        
        // Register Bluetooth state receiver
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothStateReceiver, filter)
    }
    
    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        deviceNameText = findViewById(R.id.deviceNameText)
        connectionStatusText = findViewById(R.id.connectionStatusText)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        btnVolumeUp = findViewById(R.id.btnVolumeUp)
        btnVolumeDown = findViewById(R.id.btnVolumeDown)
        btnConnect = findViewById(R.id.btnConnect)
        
        // Set up button click listeners
        btnPlayPause.setOnClickListener { sendMediaCommand(MediaCommand.PLAY_PAUSE) }
        btnPrevious.setOnClickListener { sendMediaCommand(MediaCommand.PREVIOUS) }
        btnNext.setOnClickListener { sendMediaCommand(MediaCommand.NEXT) }
        btnVolumeUp.setOnClickListener { sendMediaCommand(MediaCommand.VOLUME_UP) }
        btnVolumeDown.setOnClickListener { sendMediaCommand(MediaCommand.VOLUME_DOWN) }
        btnConnect.setOnClickListener { connectToWifeDevice() }
        
        // Initially disable media controls
        setMediaControlsEnabled(false)
    }
    
    private fun initializeBluetooth() {
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        if (bluetoothAdapter == null) {
            statusText.text = "Bluetooth not supported on this device"
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        if (!bluetoothAdapter!!.isEnabled) {
            statusText.text = "Please enable Bluetooth"
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableBtIntent)
        }
        
        mediaController = BluetoothMediaController(this, bluetoothAdapter!!)
    }
    
    private val bluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            statusText.text = "Bluetooth enabled"
            findTargetDevice()
        } else {
            statusText.text = "Bluetooth is required for this app"
        }
    }
    
    private fun checkPermissions() {
        val requiredPermissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) 
                != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (requiredPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                requiredPermissions.toTypedArray(),
                REQUEST_PERMISSIONS
            )
        } else {
            onPermissionsGranted()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onPermissionsGranted()
            } else {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_LONG).show()
                statusText.text = "Permissions denied"
            }
        }
    }
    
    private fun onPermissionsGranted() {
        statusText.text = "Ready to connect"
        findTargetDevice()
        
        // Start the foreground service for auto-reconnection
        val serviceIntent = Intent(this, BluetoothMediaService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
    
    private fun findTargetDevice() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        val pairedDevices = bluetoothAdapter?.bondedDevices
        targetDevice = pairedDevices?.find { it.name?.contains("Pixel", ignoreCase = true) == true }
        
        if (targetDevice != null) {
            deviceNameText.text = "Target: ${targetDevice!!.name}"
            statusText.text = "Device found. Tap Connect to begin."
            btnConnect.isEnabled = true
        } else {
            deviceNameText.text = "No Pixel device paired"
            statusText.text = "Please pair wife's Pixel first in Settings"
            btnConnect.isEnabled = false
        }
    }
    
    private fun connectToWifeDevice() {
        targetDevice?.let { device ->
            lifecycleScope.launch {
                statusText.text = "Connecting..."
                val connected = mediaController?.connectToDevice(device) ?: false
                if (connected) {
                    updateConnectionStatus(true)
                    statusText.text = "Connected successfully"
                } else {
                    updateConnectionStatus(false)
                    statusText.text = "Connection failed. Retrying..."
                    attemptReconnection()
                }
            }
        } ?: run {
            Toast.makeText(this, "No device selected", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun attemptReconnection() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                // Wait a moment before reconnecting
                kotlinx.coroutines.delay(2000)
                withContext(Dispatchers.Main) {
                    connectToWifeDevice()
                }
            }
        }
    }
    
    private fun sendMediaCommand(command: MediaCommand) {
        if (mediaController?.isConnected() == true) {
            lifecycleScope.launch {
                val success = mediaController?.sendCommand(command) ?: false
                if (success) {
                    statusText.text = "Command sent: ${command.name}"
                } else {
                    statusText.text = "Command failed"
                }
            }
        } else {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateConnectionStatus(connected: Boolean) {
        runOnUiThread {
            if (connected) {
                connectionStatusText.text = "● Connected"
                connectionStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
                setMediaControlsEnabled(true)
            } else {
                connectionStatusText.text = "○ Disconnected"
                connectionStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                setMediaControlsEnabled(false)
            }
        }
    }
    
    private fun setMediaControlsEnabled(enabled: Boolean) {
        btnPlayPause.isEnabled = enabled
        btnPrevious.isEnabled = enabled
        btnNext.isEnabled = enabled
        btnVolumeUp.isEnabled = enabled
        btnVolumeDown.isEnabled = enabled
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
        mediaController?.disconnect()
    }
}
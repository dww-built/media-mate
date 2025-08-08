package com.blenko.mediamate

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

enum class MediaCommand(val avrcpCode: Byte) {
    PLAY_PAUSE(0x46),
    NEXT(0x4B),
    PREVIOUS(0x4C),
    VOLUME_UP(0x41),
    VOLUME_DOWN(0x42),
    STOP(0x45),
    FAST_FORWARD(0x49),
    REWIND(0x48)
}

class BluetoothMediaController(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter
) {
    companion object {
        private const val TAG = "MediaController"
        // Standard Serial Port Profile UUID for AVRCP communication
        private val AVRCP_UUID = UUID.fromString("00000000-0000-1000-8000-00805F9B34FB")
        // Alternative UUIDs to try if primary fails
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val AVRCP_CONTROLLER_UUID = UUID.fromString("0000110E-0000-1000-8000-00805F9B34FB")
        private val AVRCP_TARGET_UUID = UUID.fromString("0000110C-0000-1000-8000-00805F9B34FB")
    }
    
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var connectedDevice: BluetoothDevice? = null
    private var isConnectedFlag = false
    
    suspend fun connectToDevice(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission")
            return@withContext false
        }
        
        try {
            // Close any existing connection
            disconnect()
            
            // Try different connection methods
            val connected = tryConnectWithUuid(device, AVRCP_CONTROLLER_UUID) ||
                          tryConnectWithUuid(device, AVRCP_TARGET_UUID) ||
                          tryConnectWithUuid(device, SPP_UUID) ||
                          tryConnectWithReflection(device)
            
            if (connected) {
                connectedDevice = device
                isConnectedFlag = true
                Log.d(TAG, "Successfully connected to ${device.name}")
            }
            
            return@withContext connected
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            return@withContext false
        }
    }
    
    private fun tryConnectWithUuid(device: BluetoothDevice, uuid: UUID): Boolean {
        return try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
                return false
            }
            
            Log.d(TAG, "Trying to connect with UUID: $uuid")
            val socket = device.createRfcommSocketToServiceRecord(uuid)
            socket.connect()
            bluetoothSocket = socket
            outputStream = socket.outputStream
            Log.d(TAG, "Connected successfully with UUID: $uuid")
            true
        } catch (e: IOException) {
            Log.w(TAG, "Failed to connect with UUID $uuid: ${e.message}")
            false
        }
    }
    
    private fun tryConnectWithReflection(device: BluetoothDevice): Boolean {
        return try {
            Log.d(TAG, "Trying reflection method for connection")
            // Use reflection to access createRfcommSocket
            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.java)
            val socket = method.invoke(device, 1) as BluetoothSocket
            
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
                return false
            }
            
            socket.connect()
            bluetoothSocket = socket
            outputStream = socket.outputStream
            Log.d(TAG, "Connected successfully with reflection method")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Reflection method failed", e)
            false
        }
    }
    
    suspend fun sendCommand(command: MediaCommand): Boolean = withContext(Dispatchers.IO) {
        if (!isConnectedFlag || outputStream == null) {
            Log.w(TAG, "Not connected, cannot send command")
            return@withContext false
        }
        
        try {
            // AVRCP command structure
            // For simplicity, we're sending basic HID-like commands
            // In production, you'd implement full AVRCP protocol
            val commandPacket = createMediaCommandPacket(command)
            
            outputStream?.write(commandPacket)
            outputStream?.flush()
            
            Log.d(TAG, "Sent command: ${command.name}")
            return@withContext true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to send command", e)
            isConnectedFlag = false
            return@withContext false
        }
    }
    
    private fun createMediaCommandPacket(command: MediaCommand): ByteArray {
        // Simplified AVRCP passthrough command structure
        // In a full implementation, this would follow AVRCP specification
        return byteArrayOf(
            0x00,  // Transaction label
            0x48,  // PDU ID for passthrough
            command.avrcpCode,  // Operation ID
            0x00,  // Operation data length
            0x00   // Subunit type & ID
        )
    }
    
    fun isConnected(): Boolean {
        return isConnectedFlag && bluetoothSocket?.isConnected == true
    }
    
    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error during disconnect", e)
        } finally {
            outputStream = null
            bluetoothSocket = null
            connectedDevice = null
            isConnectedFlag = false
        }
    }
    
    fun getConnectedDevice(): BluetoothDevice? = connectedDevice
}
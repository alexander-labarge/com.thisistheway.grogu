package com.thisistheway.grogu

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BluetoothScanner(
    private val context: Context,
    private val handler: BluetoothScanHandler,
    private val filterMacAddress: String?
) {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isClassicReceiverRegistered = false
    private var isLeScanActive = false
    private var targetDeviceFound = false // Track if target device has been found
    private val executorService = Executors.newFixedThreadPool(4) // Thread pool for background tasks

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothAdapter = bluetoothManager.adapter
    }

    fun startScanning(scanType: String) {
        Log.d(TAG, "startScanning: Starting Bluetooth scan")
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "startScanning: Bluetooth scan permission not granted")
            return
        }

        if (scanType == "Bluetooth LE") {
            bluetoothAdapter?.bluetoothLeScanner?.startScan(leScanCallback)
            isLeScanActive = true
        } else {
            bluetoothAdapter?.startDiscovery()
            context.registerReceiver(classicScanReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
            isClassicReceiverRegistered = true
        }
    }

    fun stopScanning() {
        Log.d(TAG, "stopScanning: Stopping Bluetooth scan")
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "stopScanning: Bluetooth scan permission not granted")
            return
        }

        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }

        if (isLeScanActive) {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
            isLeScanActive = false
        }

        if (isClassicReceiverRegistered) {
            context.unregisterReceiver(classicScanReceiver)
            isClassicReceiverRegistered = false
        }
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            executorService.execute {
                handleScanResult(result.device, result.rssi, result.scanRecord?.bytes, "Bluetooth LE")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "leScanCallback: Bluetooth LE scan failed with error code: $errorCode")
        }
    }

    private val classicScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                executorService.execute {
                    handleScanResult(device, rssi, null, "Bluetooth Classic")
                }
            }
        }
    }

    private fun handleScanResult(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?, scanType: String) {
        val macAddress = device?.address ?: return
        if (targetDeviceFound) return

        val deviceName = if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            device?.name ?: "Unknown"
        } else {
            "Permission not granted"
        }

        val packetString = scanRecord?.joinToString(separator = " ") { String.format("%02X", it) } ?: "N/A"
        val result = BluetoothConstants.PendingResult(deviceName, macAddress, rssi, packetString, scanType)

        if (!filterMacAddress.isNullOrEmpty() && macAddress.equals(filterMacAddress, ignoreCase = true)) {
            targetDeviceFound = true
            handler.obtainMessage(BluetoothConstants.STATE_MESSAGE_RECEIVED, result).sendToTarget()
            stopScanning()  // Stop the scan if a matching device is found
            sendL2CAPPacket(device)
        } else if (filterMacAddress.isNullOrEmpty()) {
            handler.obtainMessage(BluetoothConstants.STATE_MESSAGE_RECEIVED, result).sendToTarget()
            sendL2CAPPacket(device)
        }
    }

    private fun sendL2CAPPacket(device: BluetoothDevice?) {
        device ?: return
        val PSM = 0x0001 // PSM for GATT
        val maxRetries = 3
        var attempt = 0

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_BLUETOOTH_CONNECT
            )
            return
        }

        while (attempt < maxRetries) {
            try {
                Log.d(TAG, "Attempting to create L2CAP channel to device: ${device.address}, attempt: ${attempt + 1}")
                val socket = device.createInsecureL2capChannel(PSM)
                socket.connect()
                Log.d(TAG, "Connected to the remote device: ${device.address}")

                val outputStream = socket.outputStream
                val message = "Hello from Android"
                outputStream.write(message.toByteArray())
                outputStream.flush()
                Log.d(TAG, "Sent L2CAP packet: $message to ${device.address}")

                socket.close()
                Log.d(TAG, "Closed socket to ${device.address}")
                return // Exit after a successful connection
            } catch (e: IOException) {
                Log.e(TAG, "Failed to send L2CAP packet to ${device.address} on attempt ${attempt + 1}", e)
                attempt++
                if (attempt < maxRetries) {
                    // Delay before retrying
                    TimeUnit.SECONDS.sleep(1)
                } else {
                    Log.e(TAG, "Max retries reached. Giving up on ${device.address}")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException: Ensure Bluetooth permissions are granted", e)
                return
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected exception occurred", e)
                return
            }
        }
    }

    companion object {
        private const val TAG = "BluetoothScanner"
        private const val REQUEST_BLUETOOTH_CONNECT = 1
    }
}

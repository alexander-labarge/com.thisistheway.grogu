package com.thisistheway.grogu

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.thisistheway.grogu.databinding.ActivityRawSocketCheckerBinding

class RawSocketCheckerActivity : AppCompatActivity(), UsbPermissionCallback {

    companion object {
        init {
            System.loadLibrary("raw_usb_socket_test")
        }
        const val ACTION_USB_PERMISSION = "com.thisistheway.grogu.USB_PERMISSION"
        const val TAG = "RawSocketCheckerActivity"
        const val REQUEST_USB_PERMISSION = 1
    }

    private lateinit var binding: ActivityRawSocketCheckerBinding
    private lateinit var usbManager: UsbManager
    private lateinit var mPermissionIntent: PendingIntent
    private lateinit var usbScanHandler: UsbScanHandler

    private lateinit var usbPermissionReceiver: UsbPermissionReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRawSocketCheckerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        mPermissionIntent = PendingIntent.getBroadcast(
            this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
        )

        usbScanHandler = UsbScanHandler(this, binding)

        usbPermissionReceiver = UsbPermissionReceiver(this)
        val usbPermissionIntent = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbPermissionReceiver, usbPermissionIntent, RECEIVER_NOT_EXPORTED)

        val startButton = findViewById<Button>(R.id.start_usb_button)
        val returnHomeButton = findViewById<Button>(R.id.return_home_button)

        startButton.setOnClickListener {
            obtainUsbPermissionsAndStartTest()
        }

        returnHomeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbPermissionReceiver)
    }

    private fun obtainUsbPermissionsAndStartTest() {
        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
        for (usbDevice in deviceList.values) {
            if (usbManager.hasPermission(usbDevice)) {
                Log.d(TAG, "Obtained Permission for USB Device: $usbDevice")
                openUsbDeviceAndStartTest(usbDevice)
            } else {
                Log.d(TAG, "Requesting Permission for: $usbDevice")
                usbManager.requestPermission(usbDevice, mPermissionIntent)
            }
        }
    }

    override fun onUsbPermissionGranted(device: UsbDevice) {
        openUsbDeviceAndStartTest(device)
    }

    private fun openUsbDeviceAndStartTest(device: UsbDevice) {
        Log.d(TAG, "openUsbDeviceAndStartTest: Attempting to open USB device: $device")

        val usbDeviceConnection: UsbDeviceConnection? = usbManager.openDevice(device)

        if (usbDeviceConnection == null) {
            Log.e(TAG, "openUsbDeviceAndStartTest: Failed to open connection to USB device: $device")
            return
        }

        Log.d(TAG, "openUsbDeviceAndStartTest: Successfully opened connection to USB device: $device")

        val fileDescriptor: Int = usbDeviceConnection.fileDescriptor
        Log.d(TAG, "openUsbDeviceAndStartTest: Obtained file descriptor: $fileDescriptor")

        try {
            startRawSocketTest(fileDescriptor)
            Log.d(TAG, "openUsbDeviceAndStartTest: Called startRawSocketTest with file descriptor: $fileDescriptor")
        } catch (e: Exception) {
            Log.e(TAG, "openUsbDeviceAndStartTest: Error calling startRawSocketTest", e)
        } finally {
            usbDeviceConnection.close()
            Log.d(TAG, "openUsbDeviceAndStartTest: Closed connection to USB device: $device")
        }
    }

    private external fun startRawSocketTest(fileDescriptor: Int)
}

package com.thisistheway.grogu

import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log

class UsbDeviceChecker(private val context: Context) {

    companion object {
        init {
            System.loadLibrary("beskar_usb_validator")
        }
        const val ACTION_USB_PERMISSION = "com.thisistheway.beskar.USB_PERMISSION"
    }

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val mPermissionIntent: PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
    )

    fun obtainUsbPermissions() {
        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
        for (usbDevice in deviceList.values) {
            if (usbManager.hasPermission(usbDevice)) {
                Log.d(TAG, "Obtained Permission for USB Device: $usbDevice")
                openUsbDevice(usbDevice)
            } else {
                Log.d(TAG, "Requesting Permission for: $usbDevice")
                usbManager.requestPermission(usbDevice, mPermissionIntent)
            }
        }
    }

    private fun openUsbDevice(device: UsbDevice) {
        Log.d(TAG, "openUsbDevice: Attempting to open USB device: $device")

        val usbDeviceConnection: UsbDeviceConnection? = usbManager.openDevice(device)

        if (usbDeviceConnection == null) {
            Log.e(TAG, "openUsbDevice: Failed to open connection to USB device: $device")
            return
        }

        Log.d(TAG, "openUsbDevice: Successfully opened connection to USB device: $device")

        val fileDescriptor: Int = usbDeviceConnection.fileDescriptor
        Log.d(TAG, "openUsbDevice: Obtained file descriptor: $fileDescriptor")

        try {
            beskarUsbDescriptor(fileDescriptor)
            Log.d(TAG, "openUsbDevice: Called beskarUsbDescriptor with file descriptor: $fileDescriptor")
        } catch (e: Exception) {
            Log.e(TAG, "openUsbDevice: Error calling beskarUsbDescriptor", e)
        } finally {
            usbDeviceConnection.close() // Ensure the device connection is closed after use
            Log.d(TAG, "openUsbDevice: Closed connection to USB device: $device")
        }
    }

    private external fun beskarUsbDescriptor(fileDescriptor: Int)
}

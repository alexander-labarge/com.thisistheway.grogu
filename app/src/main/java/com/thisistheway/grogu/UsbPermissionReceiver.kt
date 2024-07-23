package com.thisistheway.grogu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class UsbPermissionReceiver(private val callback: UsbPermissionCallback) : BroadcastReceiver() {

    companion object {
        private const val TAG = "UsbPermissionReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return

        val action = intent.action
        if (RawSocketCheckerActivity.ACTION_USB_PERMISSION == action) {
            synchronized(this) {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
                        callback.onUsbPermissionGranted(device)
                    } else {
                        Log.e(TAG, "Received null device <- how ANNOYINNGGGG")
                    }
                } else {
                    Log.e(TAG, "Permission denied for device $device")
                }
            }
        }
    }
}

interface UsbPermissionCallback {
    fun onUsbPermissionGranted(device: UsbDevice)
}

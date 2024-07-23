package com.thisistheway.grogu

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.thisistheway.grogu.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothScanner: BluetoothScanner
    private lateinit var handler: BluetoothScanHandler
    private lateinit var usbDeviceChecker: UsbDeviceChecker

    companion object {
        const val REQUEST_BLUETOOTH_PERMISSIONS = 1
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity created")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: View binding completed")

        val scanForBluetoothDevicesButton: Button = findViewById(R.id.scanForBluetoothDevicesButton)
        val checkUsbBluetoothAdapterButton: Button = findViewById(R.id.checkUsbBluetoothAdapterButton)
        val stopScanningButton: Button = findViewById(R.id.stopScanningButton)
        val checkRawSocketButton: Button = findViewById(R.id.checkRawSocketButton)

        val macAddress = intent.getStringExtra("MAC_ADDRESS")
        val scanType = intent.getStringExtra("SCAN_TYPE") ?: "Bluetooth Classic"
        val startScanning = intent.getBooleanExtra("START_SCANNING", false)

        handler = BluetoothScanHandler(this, binding, macAddress)
        bluetoothScanner = BluetoothScanner(this, handler, macAddress)
        usbDeviceChecker = UsbDeviceChecker(this) // Pass context to UsbDeviceChecker

        checkPermissions()

        scanForBluetoothDevicesButton.setOnClickListener {
            val intent = Intent(this, MacAddressInputActivity::class.java)
            startActivity(intent)
        }

        checkUsbBluetoothAdapterButton.setOnClickListener {
            if (checkBluetoothPermissions()) {
                checkUsbDevices()
            } else {
                requestBluetoothPermissions()
            }
        }

        stopScanningButton.setOnClickListener {
            bluetoothScanner.stopScanning()
            val intent = Intent(this, MacAddressInputActivity::class.java)
            startActivity(intent)
        }

        checkRawSocketButton.setOnClickListener {
            if (checkBluetoothPermissions()) {
                val intent = Intent(this, RawSocketCheckerActivity::class.java)
                startActivity(intent)
            } else {
                requestBluetoothPermissions()
            }
        }

        if (startScanning) {
            scanForBluetoothDevicesButton.visibility = View.GONE
            checkUsbBluetoothAdapterButton.visibility = View.GONE
            stopScanningButton.visibility = View.VISIBLE
            bluetoothScanner.startScanning(scanType)
        }
    }

    private fun checkPermissions(onPermissionsGranted: () -> Unit = {}) {
        Log.d(TAG, "checkPermissions: Checking permissions")
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsNeeded.isNotEmpty()) {
            Log.d(TAG, "checkPermissions: Requesting permissions: $permissionsNeeded")
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
        } else {
            onPermissionsGranted()
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_BLUETOOTH_PERMISSIONS
        )
    }

    private fun checkUsbDevices() {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val usbDevices = usbManager.deviceList

        if (usbDevices.isNotEmpty()) {
            for (device in usbDevices.values) {
                usbDeviceChecker.obtainUsbPermissions()
            }
        } else {
            Log.d(TAG, "checkUsbDevices: No USB devices found")
        }
    }
}

package com.thisistheway.grogu

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

class MacAddressInputActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mac_address_input)

        val macAddressInput = findViewById<EditText>(R.id.mac_address_input)
        val startButton = findViewById<Button>(R.id.start_usb_button)
        val returnHomeButton = findViewById<Button>(R.id.return_home_button)
        val spinnerScanType = findViewById<Spinner>(R.id.spinner_scan_type)

        val scanTypes = resources.getStringArray(R.array.scan_types)
        val adapter = ArrayAdapter(this, R.layout.spinner_item, scanTypes)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerScanType.adapter = adapter
        spinnerScanType.setSelection(1) // Set default selection to "Bluetooth Classic"

        startButton.setOnClickListener {
            val macAddress = macAddressInput.text.toString().trim()
            val scanType = spinnerScanType.selectedItem.toString()
            val intent = Intent(this, MainActivity::class.java)
            if (isValidMacAddress(macAddress)) {
                intent.putExtra("MAC_ADDRESS", macAddress)
            }
            intent.putExtra("SCAN_TYPE", scanType)
            intent.putExtra("START_SCANNING", true)
            startActivity(intent)
        }

        returnHomeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun isValidMacAddress(macAddress: String): Boolean {
        val macAddressRegex = Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}")
        return macAddress.matches(macAddressRegex)
    }
}

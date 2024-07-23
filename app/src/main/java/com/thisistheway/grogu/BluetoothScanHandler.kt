package com.thisistheway.grogu

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import com.thisistheway.grogu.databinding.ActivityMainBinding
import android.widget.ScrollView

class BluetoothScanHandler(
    private val context: Context,
    private val binding: ActivityMainBinding,
    private val filterMacAddress: String?
) : Handler(Looper.getMainLooper()) {

    companion object {
        private const val TAG = "BluetoothScanHandler"
    }

    override fun handleMessage(msg: Message) {
        if (msg.what == BluetoothConstants.STATE_MESSAGE_RECEIVED) {
            val result = msg.obj as BluetoothConstants.PendingResult
            val spannableStringBuilder = SpannableStringBuilder()

            val headerSpannable = SpannableString(
                if (!filterMacAddress.isNullOrEmpty() && result.macAddress.equals(filterMacAddress, ignoreCase = true)) {
                    "\n--------------------------------------\nTARGET DEVICE FOUND (${result.scanType}):\n--------------------------------------\n"
                } else {
                    "\nDevice Found (${result.scanType}):\n"
                }
            )
            headerSpannable.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD),
                0,
                headerSpannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableStringBuilder.append(headerSpannable)

            val deviceNameSpannable = SpannableString("Device Name: ${result.deviceName}\n")
            deviceNameSpannable.setSpan(
                ForegroundColorSpan(ResourcesCompat.getColor(context.resources, android.R.color.holo_blue_light, null)),
                12,
                deviceNameSpannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            deviceNameSpannable.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD),
                12,
                deviceNameSpannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            val macAddressSpannable = SpannableString("Device MAC: ${result.macAddress}\n")
            macAddressSpannable.setSpan(
                ForegroundColorSpan(ResourcesCompat.getColor(context.resources, android.R.color.holo_red_light, null)),
                12,
                macAddressSpannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            macAddressSpannable.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD),
                12,
                macAddressSpannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            val rssiSpannable = SpannableString("RSSI: ${result.rssi}\n")
            rssiSpannable.setSpan(
                ForegroundColorSpan(ResourcesCompat.getColor(context.resources, android.R.color.holo_green_light, null)),
                6,
                rssiSpannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            rssiSpannable.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD),
                6,
                rssiSpannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            val packetSpannable = SpannableString("Packet: ${result.packetString}\n")
            packetSpannable.setSpan(
                ForegroundColorSpan(ResourcesCompat.getColor(context.resources, android.R.color.holo_orange_light, null)),
                8,
                packetSpannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            packetSpannable.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD),
                8,
                packetSpannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spannableStringBuilder.append(deviceNameSpannable)
            spannableStringBuilder.append(macAddressSpannable)
            spannableStringBuilder.append(rssiSpannable)
            spannableStringBuilder.append(packetSpannable)

            binding.sampleText.append(spannableStringBuilder)
            scrollToBottom()
        }
    }

    private fun scrollToBottom() {
        binding.scrollView.post {
            binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            Log.d(TAG, "scrollToBottom: Scrolled to bottom")
        }
    }
}

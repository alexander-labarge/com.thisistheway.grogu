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
import com.thisistheway.grogu.databinding.ActivityRawSocketCheckerBinding
import android.widget.ScrollView

class UsbScanHandler(
    private val context: Context,
    private val binding: ActivityRawSocketCheckerBinding
) : Handler(Looper.getMainLooper()) {

    companion object {
        private const val TAG = "UsbScanHandler"
        const val STATE_USB_DEVICE_FOUND = 1

        @JvmStatic
        fun sendMessageToHandler(message: String, unused: String) {
            val result = UsbDeviceResult(message.trim())
            val msg = Message.obtain()
            msg.what = STATE_USB_DEVICE_FOUND
            msg.obj = result
            instance?.sendMessage(msg)
        }

        private var instance: UsbScanHandler? = null

        fun setInstance(handler: UsbScanHandler) {
            instance = handler
        }
    }

    init {
        setInstance(this)
    }

    override fun handleMessage(msg: Message) {
        if (msg.what == STATE_USB_DEVICE_FOUND) {
            val result = msg.obj as UsbDeviceResult
            val spannableStringBuilder = SpannableStringBuilder()

            // INFO: in yellow
            val infoSpannable = SpannableString("INFO: ")
            infoSpannable.setSpan(
                ForegroundColorSpan(ResourcesCompat.getColor(context.resources, android.R.color.holo_orange_light, null)),
                0,
                infoSpannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            infoSpannable.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD),
                0,
                infoSpannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Result in blue
            val messageSpannable = SpannableString(result.message + "\n")
            messageSpannable.setSpan(
                ForegroundColorSpan(ResourcesCompat.getColor(context.resources, android.R.color.holo_blue_light, null)),
                0,
                messageSpannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spannableStringBuilder.append(infoSpannable)
            spannableStringBuilder.append(messageSpannable)

            binding.rawSocketResults.append(spannableStringBuilder)
            scrollToBottom()
        }
    }

    private fun scrollToBottom() {
        binding.scrollView.post {
            binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            Log.d(TAG, "scrollToBottom: Scrolled to bottom")
        }
    }

    data class UsbDeviceResult(
        val message: String
    )
}

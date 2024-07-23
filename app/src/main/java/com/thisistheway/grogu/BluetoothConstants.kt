package com.thisistheway.grogu

object BluetoothConstants {
    const val STATE_MESSAGE_RECEIVED = 1

    data class PendingResult(
        val deviceName: String,
        val macAddress: String,
        val rssi: Int,
        val packetString: String,
        val scanType: String
    )
}

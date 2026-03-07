package com.tutozz.blespam

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.ParcelUuid
import android.util.Log
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class VzhuhSpam : Spammer {

    private var blinkRunnable: Runnable? = null
    @Volatile
    private var isSpamming = false
    private var loop = 0
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var currentCallback: AdvertiseCallback? = null
    private var currentAdvertiser: android.bluetooth.le.BluetoothLeAdvertiser? = null
    private val isStopping = AtomicBoolean(false)

    companion object {
        private const val TAG = "VzhuhSpam"
        private const val COMPANY_ID = 0x0000
        private const val MANUFACTURER_DATA_HEX = "4C517648557676524C546734"
        private val SERVICE_UUID: UUID = UUID.fromString("00001840-0000-1000-8000-00805f9b34fb")
    }

    override fun start() {
        executor.execute {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                Log.e(TAG, "Bluetooth not available or not enabled")
                return@execute
            }

            val advertiser = bluetoothAdapter.bluetoothLeAdvertiser
            if (advertiser == null) {
                Log.e(TAG, "BLE Advertising not supported")
                return@execute
            }

            isSpamming = true
            isStopping.set(false)
            loop = 0

            val manufacturerBytes = Helper.convertHexToByteArray(MANUFACTURER_DATA_HEX)
            Log.d(TAG, "Manufacturer data size: ${manufacturerBytes.size} bytes")

            startContinuousAdvertising(advertiser, manufacturerBytes)
        }
    }

    private fun startContinuousAdvertising(
        advertiser: android.bluetooth.le.BluetoothLeAdvertiser,
        manufacturerBytes: ByteArray
    ) {
        currentAdvertiser = advertiser

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        // Advertising Data - основные данные, видны всем сканерам
        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(true)
            .addManufacturerData(COMPANY_ID, manufacturerBytes)
            .addServiceUuid(ParcelUuid(SERVICE_UUID)) // Добавляем сюда для всех сканеров
            .build()

        // Scan Response - дополнительные данные (не все сканеры запрашивают)
        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .build()

        val latch = CountDownLatch(1)

        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.d(TAG, "✓ Advertising started successfully")
                Log.d(TAG, "  Mode: LOW_LATENCY")
                Log.d(TAG, "  TxPower: HIGH")
                Log.d(TAG, "  Connectable: true")
                blinkRunnable?.run()
                latch.countDown()
            }

            override fun onStartFailure(errorCode: Int) {
                val errorMsg = when (errorCode) {
                    ADVERTISE_FAILED_DATA_TOO_LARGE -> "DATA_TOO_LARGE"
                    ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "TOO_MANY_ADVERTISERS"
                    ADVERTISE_FAILED_ALREADY_STARTED -> "ALREADY_STARTED"
                    ADVERTISE_FAILED_INTERNAL_ERROR -> "INTERNAL_ERROR"
                    ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "FEATURE_UNSUPPORTED"
                    else -> "UNKNOWN($errorCode)"
                }
                Log.e(TAG, "✗ Advertising failed: $errorMsg")
                isSpamming = false
                latch.countDown()
            }
        }

        currentCallback = callback

        try {
            advertiser.startAdvertising(settings, advertiseData, scanResponse, callback)

            if (!latch.await(2, TimeUnit.SECONDS)) {
                Log.e(TAG, "Timeout waiting for advertising to start")
                isSpamming = false
                return
            }

            Log.d(TAG, "Advertising loop started")

            // Непрерывная реклама
            while (isSpamming && loop <= Helper.MAX_LOOP) {
                try {
                    Thread.sleep(Helper.delay.toLong())
                    loop++

                    if (loop % 10 == 0) {
                        Log.d(TAG, "Advertising active, loop: $loop")
                        blinkRunnable?.run()
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    Log.d(TAG, "Advertising loop interrupted")
                    break
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during advertising: ${e.message}", e)
        } finally {
            // Останавливаем рекламу только если не было вызвано stop()
            stopAdvertisingInternal()
        }
    }

    private fun stopAdvertisingInternal() {
        // Используем атомарную операцию чтобы избежать двойной остановки
        if (!isStopping.compareAndSet(false, true)) {
            Log.d(TAG, "Already stopping, skipping")
            return
        }

        try {
            val callback = currentCallback
            val advertiser = currentAdvertiser

            if (callback != null && advertiser != null) {
                Log.d(TAG, "Stopping advertising...")
                advertiser.stopAdvertising(callback)
                Log.d(TAG, "✓ Advertising stopped successfully")
            } else {
                Log.d(TAG, "No active advertising to stop")
            }
        } catch (e: IllegalArgumentException) {
            // Callback уже был удален - это нормально
            Log.d(TAG, "Callback already removed (this is OK)")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping advertising: ${e.message}")
        } finally {
            isSpamming = false
            currentCallback = null
            currentAdvertiser = null
        }
    }

    override fun stop() {
        Log.d(TAG, "Stop requested")
        isSpamming = false
        stopAdvertisingInternal()
    }

    override fun isSpamming(): Boolean = isSpamming

    override fun setBlinkRunnable(blinkRunnable: Runnable?) {
        this.blinkRunnable = blinkRunnable
    }

    override fun getBlinkRunnable(): Runnable? = blinkRunnable
}
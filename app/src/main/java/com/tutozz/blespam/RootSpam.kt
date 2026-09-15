package com.tutozz.blespam

import android.content.Context
import android.util.Log
import com.tutozz.blespam.security.RootChecker
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class RootSpam(
    private val context: Context,
    private val companyId: Int = 0xFFFF,
    private val payloadHex: String = "4C517648557676524C546734",
    private val minInterval: Int = 0x20,
    private val maxInterval: Int = 0x20,
    private val loopDelayMs: Long = 100L,
    private val rotateMacEveryMs: Long = 3000L
) : Spammer {
    private companion object { const val TAG = "RootSpam" }
    private var blinkRunnable: Runnable? = null
    @Volatile private var isSpamming = false
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val isStopping = AtomicBoolean(false)
    private var helperPath: String? = null

    override fun start() {
        executor.execute {
            if (!RootChecker.isRootAvailable()) {
                Log.e(TAG, "root unavailable"); return@execute
            }
            helperPath = RootChecker.prepareHciHelper(context)
            if (helperPath == null) {
                Log.e(TAG, "Failed to prepare hci_helper"); return@execute
            }

            isSpamming = true
            isStopping.set(false)

            try {
                // Пример: установка параметров рекламы
                runHciCmd("08", "0006", "2000200003000000000000000000000007")
                Thread.sleep(100)

                val data = Helper.convertHexToByteArray(payloadHex)
                val payload = HciCommands.buildManufacturerPayload(companyId, data)
                // В HCI команду длина данных идет первым байтом
                val advDataHex = "%02x%s".format(payload.size, bytesToHex(payload))
                runHciCmd("08", "0008", advDataHex)
                Thread.sleep(100)

                // Включение рекламы
                runHciCmd("08", "000A", "01")
                blinkRunnable?.run()
                Log.d(TAG, "Advertising ON via HCI Helper")

                var lastMacRotate = System.currentTimeMillis()
                while (isSpamming && !Thread.currentThread().isInterrupted) {
                    Thread.sleep(loopDelayMs)
                    blinkRunnable?.run()
                    val now = System.currentTimeMillis()
                    if (now - lastMacRotate > rotateMacEveryMs) {
                        // Выключить, сменить MAC, включить
                        runHciCmd("08", "000A", "00")
                        val mac = HciCommands.randomMac()
                        val macHex = bytesToHex(mac.reversedArray())
                        runHciCmd("08", "0005", macHex)
                        runHciCmd("08", "000A", "01")
                        lastMacRotate = now
                    }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (e: Exception) {
                Log.e(TAG, "loop error", e)
            } finally {
                cleanup()
            }
        }
    }

    private fun runHciCmd(ogf: String, ocf: String, params: String) {
        val path = helperPath ?: return
        // HCI device id обычно 0
        val cmd = "$path 0 $ogf $ocf $params"
        RootChecker.runAsRoot(cmd, timeoutSec = 3)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun cleanup() {
        if (!isStopping.compareAndSet(false, true)) return
        try { runHciCmd("08", "000A", "00") } catch (_: Exception) {}
        isSpamming = false
    }

    override fun stop() {
        isSpamming = false
        cleanup()
    }
    override fun isSpamming() = isSpamming
    override fun setBlinkRunnable(r: Runnable?) { blinkRunnable = r }
    override fun getBlinkRunnable(): Runnable? = blinkRunnable
}

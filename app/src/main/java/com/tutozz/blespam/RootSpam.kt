package com.tutozz.blespam

import android.util.Log
import com.tutozz.blespam.security.RootChecker
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class RootSpam(
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

    override fun start() {
        executor.execute {
            if (!RootChecker.isRootAvailable()) {
                Log.e(TAG, "root unavailable"); return@execute
            }
            val probe = RootChecker.probeHcitool()
            if (probe == null || !probe.first) {
                Log.e(TAG, "hcitool not available"); return@execute
            }
            val devId = probe.second
            isSpamming = true
            isStopping.set(false)

            try {
                runCmd(devId, HciCommands.leSetAdvParams(minInterval, maxInterval))
                Thread.sleep(80)

                val data = Helper.convertHexToByteArray(payloadHex)
                val payload = HciCommands.buildManufacturerPayload(companyId, data)
                Log.d(TAG, "payload size: ${payload.size}")

                runCmd(devId, HciCommands.leSetAdvData(payload))
                Thread.sleep(80)

                runCmd(devId, HciCommands.leSetRandomAddr(HciCommands.randomMac()))
                Thread.sleep(80)

                runCmd(devId, HciCommands.leSetAdvEnable(true))
                blinkRunnable?.run()
                Log.d(TAG, "Advertising ON via HCI")

                var lastMacRotate = System.currentTimeMillis()
                while (isSpamming && !Thread.currentThread().isInterrupted) {
                    Thread.sleep(loopDelayMs)
                    blinkRunnable?.run()
                    val now = System.currentTimeMillis()
                    if (now - lastMacRotate > rotateMacEveryMs) {
                        runCmd(devId, HciCommands.leSetAdvEnable(false))
                        runCmd(devId, HciCommands.leSetRandomAddr(HciCommands.randomMac()))
                        runCmd(devId, HciCommands.leSetAdvEnable(true))
                        lastMacRotate = now
                    }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (e: Exception) {
                Log.e(TAG, "loop error", e)
            } finally { cleanup(devId) }
        }
    }

    private fun runCmd(devId: Int, args: String) {
        RootChecker.runAsRoot("hcitool -i hci$devId cmd $args", timeoutSec = 3)
    }

    private fun cleanup(devId: Int) {
        if (!isStopping.compareAndSet(false, true)) return
        try { runCmd(devId, HciCommands.leSetAdvEnable(false)) } catch (_: Exception) {}
        isSpamming = false
    }

    override fun stop() { isSpamming = false; cleanup(0) }
    override fun isSpamming() = isSpamming
    override fun setBlinkRunnable(r: Runnable?) { blinkRunnable = r }
    override fun getBlinkRunnable(): Runnable? = blinkRunnable
}

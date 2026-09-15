package com.tutozz.blespam.security

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object RootChecker {
    private const val TAG = "RootChecker"

    @Volatile private var cached: Boolean? = null

    fun isRootAvailable(force: Boolean = false): Boolean {
        cached?.let { if (!force) return it }
        val result = try { checkSu() } catch (e: Exception) {
            Log.e(TAG, "root check failed", e); false
        }
        cached = result
        return result
    }

    private fun checkSu(): Boolean = try {
        val p = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
        if (!p.waitFor(3, TimeUnit.SECONDS)) { p.destroy(); return false }
        val out = BufferedReader(InputStreamReader(p.inputStream)).use { it.readText() }
        Log.d(TAG, "su output: $out")
        out.contains("uid=0")
    } catch (e: Exception) {
        Log.w(TAG, "su not found: ${e.message}"); false
    }

    /** Запускает команду под root, возвращает stdout (или null при ошибке). */
    fun runAsRoot(command: String, timeoutSec: Long = 5): String? = try {
        val p = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
        if (!p.waitFor(timeoutSec, TimeUnit.SECONDS)) { p.destroy(); null }
        else BufferedReader(InputStreamReader(p.inputStream)).use { it.readText() }
    } catch (e: Exception) {
        Log.e(TAG, "runAsRoot fail: ${e.message}"); null
    }

    /** Проверяет наличие hcitool и определяет индекс HCI-устройства. */
    fun probeHcitool(): Pair<Boolean, Int>? {
        val out = runAsRoot("which hcitool; hcitool dev") ?: return null
        val hasBinary = !out.contains("not found") && !out.contains("No such")
        val match = Regex("hci(\\d+)").find(out)
        val devId = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
        return hasBinary to devId
    }
}

package com.tutozz.blespam.security

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
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

    private fun checkSu(): Boolean {
    return try {
        val p = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
        if (!p.waitFor(3, TimeUnit.SECONDS)) {
            p.destroy()
            return false
        }
        val out = BufferedReader(InputStreamReader(p.inputStream)).use { it.readText() }
        Log.d(TAG, "su output: $out")
        out.contains("uid=0")
    } catch (e: Exception) {
        Log.w(TAG, "su not found: ${e.message}")
        false
    }
}
    fun runAsRoot(command: String, timeoutSec: Long = 5): String? = try {
        val p = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
        if (!p.waitFor(timeoutSec, TimeUnit.SECONDS)) { p.destroy(); null }
        else BufferedReader(InputStreamReader(p.inputStream)).use { it.readText() }
    } catch (e: Exception) {
        Log.e(TAG, "runAsRoot fail: ${e.message}"); null
    }

    /**
     * Копирует нативный helper из APK во внутреннюю директорию и делает его исполняемым.
     * @return Путь к исполняемому файлу или null в случае ошибки.
     */
    fun prepareHciHelper(context: Context): String? {
        val helperFile = File(context.filesDir, "hci_helper")
        if (!helperFile.exists()) {
            try {
                context.assets.open("hci_helper").use { input ->
                    helperFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy hci_helper from assets", e)
                return null
            }
        }

        // Делаем файл исполняемым
        val chmodResult = runAsRoot("chmod 700 ${helperFile.absolutePath}")
        if (chmodResult == null) {
            Log.e(TAG, "Failed to chmod hci_helper")
            // Файл может быть уже исполняемым, поэтому не возвращаем null сразу
        }
        return helperFile.absolutePath
    }
}

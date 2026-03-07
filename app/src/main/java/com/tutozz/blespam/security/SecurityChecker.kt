package com.tutozz.blespam.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Debug
import java.io.File

object SecurityChecker {

    data class SecurityStatus(
        val isSecure: Boolean,
        val isDebuggerAttached: Boolean,
        val isEmulator: Boolean,
        val isRooted: Boolean,
        val isDebuggable: Boolean,
        val threats: List<String>
    )

    fun checkSecurity(context: Context): SecurityStatus {
        val threats = mutableListOf<String>()

        val isDebuggerAttached = Debug.isDebuggerConnected() || Debug.waitingForDebugger()
        if (isDebuggerAttached) threats.add("debugger")

        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) threats.add("debuggable")

        val isEmulator = detectEmulator()
        if (isEmulator) threats.add("emulator")

        val isRooted = detectRoot()
        if (isRooted) threats.add("root")

        if (NativeLib.isAvailable() && !NativeLib.isEnvironmentSafe()) {
            threats.add("native_failed")
        }

        return SecurityStatus(
            isSecure = threats.isEmpty(),
            isDebuggerAttached = isDebuggerAttached,
            isEmulator = isEmulator,
            isRooted = isRooted,
            isDebuggable = isDebuggable,
            threats = threats
        )
    }

    fun isDebuggerAttached(): Boolean = Debug.isDebuggerConnected() || Debug.waitingForDebugger()

    private fun detectEmulator(): Boolean {
        return Build.FINGERPRINT.contains("generic", true) ||
                Build.FINGERPRINT.contains("sdk", true) ||
                Build.MODEL.contains("Emulator", true) ||
                Build.MODEL.contains("Android SDK", true) ||
                Build.MANUFACTURER.contains("Genymotion", true) ||
                Build.HARDWARE.contains("goldfish", true) ||
                Build.HARDWARE.contains("ranchu", true) ||
                Build.PRODUCT.contains("sdk", true) ||
                arrayOf("/dev/socket/qemud", "/dev/qemu_pipe").any { File(it).exists() }
    }

    private fun detectRoot(): Boolean {
        return arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
            "/system/xbin/su", "/data/local/xbin/su", "/su/bin/su"
        ).any { File(it).exists() } || Build.TAGS?.contains("test-keys") == true
    }
}
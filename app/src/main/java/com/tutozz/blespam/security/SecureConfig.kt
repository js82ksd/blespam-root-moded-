package com.tutozz.blespam.security

import android.content.Context

object SecureConfig {

    fun getTokenApi(context: Context): String {
        if (SecurityChecker.isDebuggerAttached()) return ""
        return if (NativeLib.isAvailable()) NativeLib.getTokenApi(context)
        else StringProtector.decrypt(StringProtector.Encrypted.TOKEN_API)
    }

    fun getReportApi(context: Context): String {
        if (SecurityChecker.isDebuggerAttached()) return ""
        return if (NativeLib.isAvailable()) NativeLib.getReportApi(context)
        else StringProtector.decrypt(StringProtector.Encrypted.REPORT_API)
    }

    fun getVersionApi(context: Context): String {
        if (SecurityChecker.isDebuggerAttached()) return ""
        return if (NativeLib.isAvailable()) NativeLib.getVersionApi(context)
        else StringProtector.decrypt(StringProtector.Encrypted.VERSION_API)
    }

    fun getSocialLink(): String {
        return if (NativeLib.isAvailable()) NativeLib.getSocialLink()
        else StringProtector.decrypt(StringProtector.Encrypted.SOCIAL)
    }

    fun getIconClick(): String {
        return if (NativeLib.isAvailable()) NativeLib.getIconClick()
        else StringProtector.decrypt(StringProtector.Encrypted.ICONCLICK)
    }

    fun getFirebaseUrl(): String {
        return if (NativeLib.isAvailable()) NativeLib.getFirebaseUrl()
        else StringProtector.decrypt(StringProtector.Encrypted.FIREBASE)
    }

    fun getAntifraudSecret(context: Context): String {
        if (SecurityChecker.isDebuggerAttached()) return ""
        return if (NativeLib.isAvailable()) NativeLib.getAntifraudSecret(context)
        else StringProtector.decrypt(StringProtector.Encrypted.ANTIFRAUD)
    }
}
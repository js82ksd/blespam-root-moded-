package com.tutozz.blespam.security

import android.content.Context

object NativeLib {

    private var isLoaded = false

    init {
        try {
            System.loadLibrary("blespam-secure")
            isLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            isLoaded = false
        }
    }

    fun isAvailable(): Boolean = isLoaded

    external fun getTokenApi(context: Context): String
    external fun getReportApi(context: Context): String
    external fun getVersionApi(context: Context): String
    external fun getSocialLink(): String
    external fun getIconClick(): String
    external fun getFirebaseUrl(): String
    external fun getAntifraudSecret(context: Context): String
    external fun isEnvironmentSafe(): Boolean
}
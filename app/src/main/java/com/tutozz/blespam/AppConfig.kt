package com.tutozz.blespam

import android.annotation.SuppressLint
import android.content.Context
import com.tutozz.blespam.security.SecureConfig
import com.tutozz.blespam.security.SecurityChecker

@SuppressLint("StaticFieldLeak")
object AppConfig {

    private var appContext: Context? = null
    @Volatile private var cachedTokenApi: String? = null
    @Volatile private var cachedReportApi: String? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun requireContext(): Context = appContext
        ?: throw IllegalStateException("AppConfig not initialized")

    val TOKEN_API: String
        get() = cachedTokenApi ?: SecureConfig.getTokenApi(requireContext()).also { cachedTokenApi = it }

    val BUG_REPORT_API: String
        get() = cachedReportApi ?: SecureConfig.getReportApi(requireContext()).also { cachedReportApi = it }

    val VERSION_CHECK_API: String
        get() = SecureConfig.getVersionApi(requireContext())

    val SOCIAL_LINK: String
        get() = SecureConfig.getSocialLink()

    val ICONCLICK: String
        get() = SecureConfig.getIconClick()

    val FIREBASE_DB_URL: String
        get() = SecureConfig.getFirebaseUrl()

    fun getTokenApi(context: Context): String = SecureConfig.getTokenApi(context)
    fun getReportApi(context: Context): String = SecureConfig.getReportApi(context)
    fun getAntifraudSecret(context: Context): String = SecureConfig.getAntifraudSecret(context)
    fun isSecure(): Boolean = appContext?.let { SecurityChecker.checkSecurity(it).isSecure } ?: false
}
package com.tutozz.blespam

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tutozz.blespam.security.SecurityChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

class BugReportActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BugReportActivity"
        private const val MAX_DESCRIPTION_LENGTH = 500
        private const val SPAM_COOLDOWN_MS = 60_000L
        private const val PREFS_NAME = "BugReportPrefs"
        private const val PREF_LAST_REPORT_TIME = "lastReportTime"
    }

    private lateinit var sharedPref: SharedPreferences
    private lateinit var sendButton: Button
    private lateinit var errorText: TextView
    private lateinit var reportEditText: EditText
    private lateinit var charCountText: TextView

    private val handler = Handler(Looper.getMainLooper())

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!performSecurityCheck()) {
            finish()
            return
        }

        sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        setContentView(R.layout.activity_bug)

        initViews()
        applyCustomColor()
        setupListeners()

        title = getString(R.string.bug_report_title)
    }

    private fun initViews() {
        reportEditText = findViewById(R.id.editText)
        sendButton = findViewById(R.id.sendButton)
        charCountText = findViewById(R.id.charCountText)
        errorText = findViewById(R.id.errorText)

        charCountText.text = getString(R.string.char_counter_format, 0, MAX_DESCRIPTION_LENGTH)
        updateButtonState()
        startCooldownTimer()
    }

    private fun setupListeners() {
        reportEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentLength = s?.length ?: 0
                charCountText.text = getString(R.string.char_counter_format, currentLength, MAX_DESCRIPTION_LENGTH)

                when {
                    currentLength > MAX_DESCRIPTION_LENGTH -> {
                        charCountText.setTextColor(Color.RED)
                        sendButton.isEnabled = false
                        sendButton.alpha = 0.5f
                    }
                    currentLength > MAX_DESCRIPTION_LENGTH * 0.9 -> {
                        charCountText.setTextColor(Color.parseColor("#FF6600"))
                        updateButtonState()
                    }
                    else -> {
                        charCountText.setTextColor(Color.parseColor("#666666"))
                        updateButtonState()
                    }
                }
                clearError()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        sendButton.setOnClickListener { handleSendReport() }
    }

    private fun handleSendReport() {
        val rawDescription = reportEditText.text.toString().trim()

        if (rawDescription.isEmpty()) {
            showError(getString(R.string.error_empty_description))
            return
        }

        val description = sanitizeInput(rawDescription)

        if (description.length > MAX_DESCRIPTION_LENGTH) {
            showError(getString(R.string.char_limit_exceeded, MAX_DESCRIPTION_LENGTH))
            return
        }

        if (isCooldownActive()) {
            showError(getString(R.string.cooldown_error, getRemainingCooldownSeconds()))
            return
        }

        lifecycleScope.launch { sendReport(description) }
    }

    private suspend fun sendReport(description: String) {
        if (isFinishing || isDestroyed) return

        try {
            withContext(Dispatchers.Main) {
                sendButton.isEnabled = false
                sendButton.text = getString(R.string.sending_button_text)
                clearError()
            }

            val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".take(100)
            val language = getAppLanguage()
            val appVersion = getAppVersion()
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

            val token = getTokenFromServer(deviceModel, language, appVersion, timestamp)
            if (token == null) return

            val success = sendReportToServer(description, token)

            if (success) {
                saveLastReportTime()
                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) {
                        showSuccess(getString(R.string.report_sent_success))
                        reportEditText.text.clear()
                        handler.postDelayed({
                            if (!isFinishing && !isDestroyed) finish()
                        }, 2000)
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) {
                    showError(getString(R.string.connection_error_generic))
                }
            }
        } finally {
            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) {
                    sendButton.text = getString(R.string.send_button_text)
                    updateButtonState()
                }
            }
        }
    }

    private suspend fun getTokenFromServer(
        model: String,
        language: String,
        version: String,
        timestamp: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
            val deviceFingerprint = Build.FINGERPRINT
            val board = Build.BOARD
            val brand = Build.BRAND
            val device = Build.DEVICE
            val nonce = UUID.randomUUID().toString()

            val antifraudSecret = try {
                AppConfig.getAntifraudSecret(this@BugReportActivity)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) showError("Security error")
                }
                return@withContext null
            }

            if (antifraudSecret.isEmpty()) {
                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) showError("Security error")
                }
                return@withContext null
            }

            val androidIdForHash = androidId.take(16)
            val modelForHash = model.take(100)
            val languageForHash = language.take(10)
            val versionForHash = version.take(50)
            val fingerprintForHash = deviceFingerprint.take(200)

            val combined = "$androidIdForHash|$modelForHash|$languageForHash|$versionForHash|$timestamp|$nonce|$fingerprintForHash|$antifraudSecret"
            val hash = sha256(combined)

            val json = JSONObject().apply {
                put("model", modelForHash)
                put("language", languageForHash)
                put("version", versionForHash)
                put("timestamp", timestamp)
                put("android_id", androidIdForHash)
                put("fingerprint", fingerprintForHash)
                put("board", board.take(50))
                put("brand", brand.take(50))
                put("device", device.take(50))
                put("nonce", nonce)
                put("hash", hash)
            }

            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val tokenApiUrl = AppConfig.getTokenApi(this@BugReportActivity)
            if (tokenApiUrl.isEmpty()) {
                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) showError("Configuration error")
                }
                return@withContext null
            }

            val safeVersion = version.replace(Regex("[^a-zA-Z0-9._-]"), "")
            val safeModel = Build.MODEL.replace(Regex("[^a-zA-Z0-9 ._-]"), "")

            val request = Request.Builder()
                .url(tokenApiUrl)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "BleSpamApp/$safeVersion ($safeModel; Android ${Build.VERSION.RELEASE})")
                .addHeader("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val jsonResponse = try { JSONObject(responseBody) } catch (e: Exception) {
                    JSONObject().put("status", "error").put("message", "Invalid response")
                }

                if (response.isSuccessful && jsonResponse.optString("status") == "success") {
                    jsonResponse.optString("token", null)
                } else {
                    val errorMsg = jsonResponse.optString("message", "Unknown error")
                    withContext(Dispatchers.Main) {
                        if (!isFinishing && !isDestroyed) showError("Error: $errorMsg")
                    }
                    null
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) showError(getString(R.string.connection_error_generic))
            }
            null
        }
    }

    private suspend fun sendReportToServer(description: String, token: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().put("description", description)
                val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                val reportApiUrl = AppConfig.getReportApi(this@BugReportActivity)
                if (reportApiUrl.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        if (!isFinishing && !isDestroyed) showError("Configuration error")
                    }
                    return@withContext false
                }

                val request = Request.Builder()
                    .url(reportApiUrl)
                    .post(requestBody)
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: ""
                        val jsonResponse = try { JSONObject(responseBody) } catch (e: Exception) { JSONObject() }
                        jsonResponse.optString("status") == "success"
                    } else {
                        withContext(Dispatchers.Main) {
                            if (!isFinishing && !isDestroyed) showError(getString(R.string.server_error_generic))
                        }
                        false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) showError(getString(R.string.connection_error_generic))
                }
                false
            }
        }

    private fun performSecurityCheck(): Boolean {
        return !SecurityChecker.isDebuggerAttached()
    }

    private fun sha256(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) { "invalid_hash" }
    }

    private fun sanitizeInput(input: String): String {
        return input
            .replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#x27;")
            .replace("\n", " ").replace("\r", " ").trim()
    }

    private fun getAppLanguage(): String {
        val language = sharedPref.getString("language", "en") ?: "en"
        return if (language.matches(Regex("^[a-z]{2,3}$"))) language else "en"
    }

    private fun getAppVersion(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: BuildConfig.VERSION_NAME
    } catch (e: Exception) { BuildConfig.VERSION_NAME }

    private fun saveLastReportTime() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(PREF_LAST_REPORT_TIME, System.currentTimeMillis()).apply()
    }

    private fun getLastReportTime(): Long =
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(PREF_LAST_REPORT_TIME, 0L)

    private fun isCooldownActive(): Boolean =
        (System.currentTimeMillis() - getLastReportTime()) < SPAM_COOLDOWN_MS

    private fun getRemainingCooldownSeconds(): Long {
        val elapsed = System.currentTimeMillis() - getLastReportTime()
        return if (elapsed < SPAM_COOLDOWN_MS) ((SPAM_COOLDOWN_MS - elapsed) / 1000).coerceAtLeast(1) else 0
    }

    private fun updateButtonState() {
        if (isFinishing || isDestroyed) return
        val isValid = reportEditText.text.length in 1..MAX_DESCRIPTION_LENGTH && !isCooldownActive()
        sendButton.isEnabled = isValid
        sendButton.alpha = if (isValid) 1f else 0.5f
    }

    private fun startCooldownTimer() {
        if (isCooldownActive()) {
            handler.postDelayed({
                if (!isFinishing && !isDestroyed) updateButtonState()
            }, SPAM_COOLDOWN_MS - (System.currentTimeMillis() - getLastReportTime()))
        }
    }

    private fun showError(message: String) {
        if (isFinishing || isDestroyed) return
        errorText.text = message
        errorText.setTextColor(Color.RED)
        errorText.visibility = View.VISIBLE
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ if (!isFinishing && !isDestroyed) clearError() }, 5000)
    }

    private fun showSuccess(message: String) {
        if (isFinishing || isDestroyed) return
        errorText.text = message
        errorText.setTextColor(Color.parseColor("#008000"))
        errorText.visibility = View.VISIBLE
    }

    private fun clearError() {
        if (isFinishing || isDestroyed) return
        errorText.text = ""
        errorText.visibility = View.GONE
    }

    private fun applyCustomColor() {
        try {
            if (sharedPref.getString("color_mode", "material") == "custom") {
                val hex = sharedPref.getString("custom_color", "FF6200EE") ?: "FF6200EE"
                val color = Color.parseColor("#$hex")
                val textColor = if ((Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000 > 128) Color.BLACK else Color.WHITE

                findViewById<ImageView>(R.id.logo)?.imageTintList = ColorStateList.valueOf(color)
                findViewById<TextView>(R.id.titleText)?.setTextColor(color)
                charCountText.setTextColor(color)
                sendButton.backgroundTintList = ColorStateList.valueOf(color)
                sendButton.setTextColor(textColor)
            }
        } catch (e: Exception) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
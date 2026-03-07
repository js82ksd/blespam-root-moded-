package com.tutozz.blespam

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.AnimationDrawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import androidx.core.app.ActivityCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import android.util.TypedValue
import androidx.annotation.RequiresApi
import com.tutozz.blespam.R
import com.google.android.material.button.MaterialButton
import android.graphics.Color
import androidx.annotation.AttrRes
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.DatabaseException


class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val spammerList = mutableListOf<Spammer>()
    private lateinit var sharedPref: android.content.SharedPreferences
    private lateinit var downloadProgressBar: ProgressBar
    private val progressHandler = Handler(Looper.getMainLooper())
    private val socialLink get() = AppConfig.SOCIAL_LINK
    private val versionCheckApi get() = AppConfig.VERSION_CHECK_API
    private val ruStoreAppUrl = "https://www.rustore.ru/catalog/app/com.tutozz.blespam"
    private val useRuStoreForUpdates = false
    private val noOpRunnable = Runnable {}

    private var vibrator: Vibrator? = null
    private var isBluetoothRequestPending = false

    private val blinkHandler = Handler(Looper.getMainLooper())
    private lateinit var logo: ImageView
    private lateinit var ios17CrashButton: MaterialButton
    private lateinit var ios17CrashCircle: ImageView
    private lateinit var appleActionModalButton: MaterialButton
    private lateinit var appleActionModalCircle: ImageView
    private lateinit var appleDevicePopupButton: MaterialButton
    private lateinit var appleDevicePopupCircle: ImageView
    private lateinit var appleNotYourDevicePopupButton: MaterialButton
    private lateinit var appleNotYourDevicePopupCircle: ImageView
    private lateinit var vzhuhSpamButton: MaterialButton
    private lateinit var vzhuhSpamCircle: ImageView
    private lateinit var androidFastPairButton: MaterialButton
    private lateinit var androidFastPairCircle: ImageView
    private lateinit var xiaomiQuickConnectButton: MaterialButton
    private lateinit var xiaomiQuickConnectCircle: ImageView
    private lateinit var samsungEasyPairBudsButton: MaterialButton
    private lateinit var samsungEasyPairBudsCircle: ImageView
    private lateinit var samsungEasyPairWatchButton: MaterialButton
    private lateinit var samsungEasyPairWatchCircle: ImageView
    private lateinit var windowsSwiftPairButton: MaterialButton
    private lateinit var windowsSwiftPairCircle: ImageView
    private lateinit var YandexButton: MaterialButton
    private lateinit var YandexCircle: ImageView
    private lateinit var minusDelayButton: MaterialButton
    private lateinit var plusDelayButton: MaterialButton
    private lateinit var delayText: TextView

    @Volatile
    private var uiLockedAfterStop = false
    private var isBetaDialogShownInSession = false

    private val handlers = mutableListOf<Handler>()

    private fun createHandler(): Handler {
        return Handler(Looper.getMainLooper()).also { handlers.add(it) }
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("BLESpam", "Bluetooth enable result: ${result.resultCode}")

        firebaseAnalytics.logEvent("bluetooth_enable_result") {
            param("granted", if (result.resultCode == RESULT_OK) "yes" else "no")
        }

        isBluetoothRequestPending = false
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, getString(R.string.bluetoothon), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.bluetootherror), Toast.LENGTH_SHORT).show()
        }
    }

    private val installPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.canRequestPackageInstalls()) {
            val uriString = sharedPref.getString("pending_apk_uri", null)
            if (uriString != null) {
                val uri = Uri.parse(uriString)
                installApk(uri)
                sharedPref.edit().remove("pending_apk_uri").apply()
            } else {
                Toast.makeText(this, getString(R.string.invalid_file_uri), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.install_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        NotificationAudienceHelper.syncLanguageAndCountry(
            sharedPref,
            sharedPref.getString("language", null)
        )
        val theme = sharedPref.getString("theme", "auto") ?: "auto"
        setAppTheme(theme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. ИНИЦИАЛИЗАЦИЯ UI ЭЛЕМЕНТОВ СРАЗУ ПОСЛЕ setContentView
        logo = findViewById(R.id.logo)

        firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN) {
            param(FirebaseAnalytics.Param.METHOD, "normal_launch")
        }

        setUserProperties()
        Log.d("BLESpam", "MainActivity onCreate - Analytics initialized")

        createHandler().postDelayed({
            initializeFirebaseMessaging()
        }, 2000)

        val bugButton = findViewById<ImageView>(R.id.settingsButton)
        bugButton.setOnClickListener {
            firebaseAnalytics.logEvent("settings_opened") {
                param(FirebaseAnalytics.Param.SCREEN_NAME, "Settings")
            }
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val requiredPermissions = getRequiredPermissions().toMutableList()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            Log.d("BLESpam", "Запрашиваем разрешения: ${permissionsToRequest.contentToString()}")
            ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_ALL_PERMISSIONS)
        } else {
            checkForNewVersion()
            completeInitialization()
        }

        createHandler().postDelayed({
        }, 500)

        applyThemeColor()
    }

    private fun initializeFirebaseMessaging() {
        if (!isNetworkAvailable()) {
            Log.w("BLESpam", "No internet connection, token will be fetched later")
            return
        }

        fetchFCMToken()
    }

    private fun fetchFCMToken(retryCount: Int = 0) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                val exception = task.exception
                Log.w("BLESpam", "FCM token fetch failed (attempt ${retryCount + 1})", exception)

                // Analytics: Token fetch failed
                firebaseAnalytics.logEvent("fcm_token_fetch_failed") {
                    param("attempt", retryCount.toLong())
                    param("error", exception?.message ?: "unknown")
                }

                // Повторная попытка через 5 секунд (максимум 3 попытки)
                if (retryCount < 3) {
                    createHandler().postDelayed({
                        fetchFCMToken(retryCount + 1)
                    }, 5000)
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Не удалось получить FCM токен. Проверьте интернет и Google Play Services",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("BLESpam", "FCM Token: $token")

            // Analytics: Token received
            firebaseAnalytics.logEvent("fcm_token_received") {
                param("success", "yes")
            }

            sharedPref.edit().putString("fcm_token", token).apply()
            saveTokenToFirebase(token)

            runOnUiThread {
                Toast.makeText(this, "Push уведомления активированы", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveTokenToFirebase(token: String) {
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val language = NotificationAudienceHelper.getSelectedLanguage(this)
        val country = NotificationAudienceHelper.getSelectedCountry(this)

        try {
            val database = FirebaseDatabase.getInstance(AppConfig.FIREBASE_DB_URL)
            val tokensRef = database.getReference("fcm_tokens")

            val tokenData = mapOf(
                "token" to token,
                "device_id" to deviceId,
                "timestamp" to System.currentTimeMillis(),
                "app_version" to getAppVersion(),
                "device_model" to Build.MODEL,
                "android_version" to Build.VERSION.RELEASE,
                "language" to language,
                "country" to country
            )

            tokensRef.child(deviceId).setValue(tokenData)
                .addOnSuccessListener {
                    Log.d("BLESpam", "Token saved to Firebase Database")

                    // Analytics: Token saved
                    firebaseAnalytics.logEvent("fcm_token_saved") {
                        param("database", "firebase")
                    }
                }
                .addOnFailureListener { exception: Exception ->
                    Log.e("BLESpam", "Failed to save token", exception)

                    // Analytics: Token save failed
                    firebaseAnalytics.logEvent("fcm_token_save_failed") {
                        param("error", exception.message ?: "unknown")
                    }
                }
        } catch (e: Exception) {
            Log.e("BLESpam", "Firebase Database error: ${e.message}", e)

            firebaseAnalytics.logEvent("firebase_db_error") {
                param("error", e.message ?: "unknown")
            }
        }
    }



    private fun setUserProperties() {
        firebaseAnalytics.setUserProperty("app_version", getAppVersion())
        firebaseAnalytics.setUserProperty("device_model", Build.MODEL)
        firebaseAnalytics.setUserProperty("android_version", Build.VERSION.RELEASE)

        val vibrationEnabled = sharedPref.getBoolean("vibration_enabled", true)
        firebaseAnalytics.setUserProperty("vibration_setting", vibrationEnabled.toString())

        val colorMode = sharedPref.getString("color_mode", "material") ?: "material"
        firebaseAnalytics.setUserProperty("color_mode", colorMode)

        val logoAnimation = sharedPref.getBoolean("logo_animation", false)
        firebaseAnalytics.setUserProperty("logo_animation", logoAnimation.toString())

        Log.d("BLESpam", "User properties set")
    }

    override fun onPause() {
        super.onPause()

        if (::logo.isInitialized) {
            (logo.drawable as? AnimationDrawable)?.stop()
        }

        // Остановить мигание
        blinkHandler.removeCallbacksAndMessages(null)
    }

    private fun logAppOpen() {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN) {
            param(FirebaseAnalytics.Param.METHOD, "normal_launch")
        }
        Log.d("BLESpam", "Analytics: App opened")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == StopSpamReceiver.ACTION_UI_STOPPED) {
            Log.d("BLESpam", "onNewIntent: ACTION_UI_STOPPED received")
            createHandler().postDelayed({
                forceResetAllUI()
            }, 100)
        }
    }

    private fun forceResetAllUI() {
        Log.d("BLESpam", "forceResetAllUI: starting")
        blinkHandler.removeCallbacksAndMessages(null)
        spammerList.forEach {
            it.getBlinkRunnable()?.let { r -> blinkHandler.removeCallbacks(r) }
            it.setBlinkRunnable(null)
        }
        spammerList.clear()

        val buttons = listOf(
            ios17CrashButton to ios17CrashCircle,
            appleActionModalButton to appleActionModalCircle,
            appleDevicePopupButton to appleDevicePopupCircle,
            appleNotYourDevicePopupButton to appleNotYourDevicePopupCircle,
            vzhuhSpamButton to vzhuhSpamCircle,
            androidFastPairButton to androidFastPairCircle,
            xiaomiQuickConnectButton to xiaomiQuickConnectCircle,
            samsungEasyPairBudsButton to samsungEasyPairBudsCircle,
            samsungEasyPairWatchButton to samsungEasyPairWatchCircle,
            windowsSwiftPairButton to windowsSwiftPairCircle,
            YandexButton to YandexCircle
        )

        buttons.forEach { (button, circle) ->
            circle.setImageResource(R.drawable.grey_circle)
            circle.imageTintList = null
            circle.clearColorFilter()
            circle.visibility = View.VISIBLE
            val strokeColor = resolveAttrColor(android.R.attr.textColorSecondary)
            button.icon = null
            button.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            button.setTextColor(strokeColor)
            try {
                button.strokeWidth = 3
                button.setStrokeColor(ColorStateList.valueOf(strokeColor))
            } catch (e: Throwable) {
                Log.e("BLESpam", "Error setting stroke: ${e.message}")
            }
        }
        updateLogoAnimation()
        Log.d("BLESpam", "forceResetAllUI: complete")
    }

    private fun resetButtonUI(button: MaterialButton, circle: ImageView) {
        circle.setImageResource(R.drawable.grey_circle)
        circle.imageTintList = null
        circle.clearColorFilter()
        circle.visibility = View.VISIBLE
        val strokeColor = resolveAttrColor(android.R.attr.textColorSecondary)
        button.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        button.setTextColor(strokeColor)
        try {
            button.setStrokeColor(ColorStateList.valueOf(strokeColor))
            button.strokeWidth = resources.getDimensionPixelSize(R.dimen.button_stroke_width).takeIf { it > 0 } ?: 3
        } catch (_: Throwable) {}
        button.invalidate()
        button.requestLayout()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    private fun applyInactiveStyle(button: MaterialButton, circle: ImageView) {
        circle.setImageResource(R.drawable.grey_circle)
        circle.imageTintList = null
        circle.clearColorFilter()
        circle.visibility = View.VISIBLE
        val strokeColor = resolveAttrColor(android.R.attr.textColorSecondary)
        button.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        try { button.setStrokeColor(ColorStateList.valueOf(strokeColor)) } catch (_: Throwable) {}
        button.setTextColor(strokeColor)
    }

    private fun resetAllSpammerButtonsUi() {
        blinkHandler.removeCallbacksAndMessages(null)
        spammerList.forEach {
            it.getBlinkRunnable()?.let { r -> blinkHandler.removeCallbacks(r) }
            it.setBlinkRunnable(null)
        }
        spammerList.clear()
        applyInactiveStyle(ios17CrashButton, ios17CrashCircle)
        applyInactiveStyle(appleActionModalButton, appleActionModalCircle)
        applyInactiveStyle(appleDevicePopupButton, appleDevicePopupCircle)
        applyInactiveStyle(appleNotYourDevicePopupButton, appleNotYourDevicePopupCircle)
        applyInactiveStyle(vzhuhSpamButton, vzhuhSpamCircle)
        applyInactiveStyle(androidFastPairButton, androidFastPairCircle)
        applyInactiveStyle(xiaomiQuickConnectButton, xiaomiQuickConnectCircle)
        applyInactiveStyle(samsungEasyPairBudsButton, samsungEasyPairBudsCircle)
        applyInactiveStyle(samsungEasyPairWatchButton, samsungEasyPairWatchCircle)
        applyInactiveStyle(windowsSwiftPairButton, windowsSwiftPairCircle)
        applyInactiveStyle(YandexButton, YandexCircle)
        updateLogoAnimation()
    }

    private fun initializeViews() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        logo = findViewById(R.id.logo)
        ios17CrashButton = findViewById(R.id.ios17CrashButton)
        ios17CrashCircle = findViewById(R.id.ios17CrashCircle)
        appleActionModalButton = findViewById(R.id.appleActionModalButton)
        appleActionModalCircle = findViewById(R.id.appleActionModalCircle)
        appleDevicePopupButton = findViewById(R.id.appleDevicePopupButton)
        appleDevicePopupCircle = findViewById(R.id.appleDevicePopupCircle)
        appleNotYourDevicePopupButton = findViewById(R.id.appleNotYourDevicePopupButton)
        appleNotYourDevicePopupCircle = findViewById(R.id.appleNotYourDevicePopupCircle)
        vzhuhSpamButton = findViewById(R.id.vzhuhSpamButton)
        vzhuhSpamCircle = findViewById(R.id.vzhuhSpamCircle)
        androidFastPairButton = findViewById(R.id.androidFastPairButton)
        androidFastPairCircle = findViewById(R.id.androidFastPairCircle)
        samsungEasyPairBudsButton = findViewById(R.id.samsungEasyPairBudsButton)
        samsungEasyPairBudsCircle = findViewById(R.id.samsungEasyPairBudsCircle)
        xiaomiQuickConnectButton = findViewById(R.id.XiaomiQuickConnectButton)
        xiaomiQuickConnectCircle = findViewById(R.id.XiaomiQuickConnectCircle)
        samsungEasyPairWatchButton = findViewById(R.id.samsungEasyPairWatchButton)
        samsungEasyPairWatchCircle = findViewById(R.id.samsungEasyPairWatchCircle)
        windowsSwiftPairButton = findViewById(R.id.windowsSwiftPairButton)
        windowsSwiftPairCircle = findViewById(R.id.windowsSwiftPairCircle)
        YandexButton = findViewById(R.id.YandexButton)
        YandexCircle = findViewById(R.id.YandexCircle)
        minusDelayButton = findViewById(R.id.minusDelayButton)
        plusDelayButton = findViewById(R.id.plusDelayButton)
        delayText = findViewById(R.id.delayText)
    }

    private fun getRequiredPermissions(): Array<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
            list.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list.toTypedArray()
    }

    private fun completeInitialization() {
        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Helper.isPermissionGranted(this)
            } else {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
        ) {
            // Restore persisted delay and concurrent mode
            Helper.loadDelay(this)
            initializeViews()
            initializeSpamButtons()
            restoreSpammerUiState()
            setupDelayButtons()
        }
    }

    private fun restoreSpammerUiState() {
        try {
            val mapping = listOf(
                Triple("iOS Crash", ios17CrashButton to ios17CrashCircle, { ContinuitySpam(ContinuityType.ACTION, true) }),
                Triple("Apple Action Modal", appleActionModalButton to appleActionModalCircle, { ContinuitySpam(ContinuityType.ACTION, false) }),
                Triple("Apple Device Popup", appleDevicePopupButton to appleDevicePopupCircle, { ContinuitySpam(ContinuityType.DEVICE, false) }),
                Triple("Apple 'Not Your Device'", appleNotYourDevicePopupButton to appleNotYourDevicePopupCircle, { ContinuitySpam(ContinuityType.NOTYOURDEVICE, false) }),
                Triple("Vzhuh Spam", vzhuhSpamButton to vzhuhSpamCircle, { VzhuhSpam() }),
                Triple("Android Fast Pair", androidFastPairButton to androidFastPairCircle, { FastPairSpam() }),
                Triple("Xiaomi Quick Connect", xiaomiQuickConnectButton to xiaomiQuickConnectCircle, { XiaomiQuickConnect() }),
                Triple("Samsung Buds", samsungEasyPairBudsButton to samsungEasyPairBudsCircle, { EasySetupSpam(EasySetupDevice.type.BUDS) }),
                Triple("Samsung Watch", samsungEasyPairWatchButton to samsungEasyPairWatchCircle, { EasySetupSpam(EasySetupDevice.type.WATCH) }),
                Triple("Windows Swift Pair", windowsSwiftPairButton to windowsSwiftPairCircle, { SwiftPairSpam() }),
                Triple("Yandex", YandexButton to YandexCircle, { YandexSpam() })
            )

            for (item in mapping) {
                val (name, views, factory) = item
                val (button, circle) = views
                val isRunning = try {
                    SpamService.isSpammerRunning(name)
                } catch (e: Exception) {
                    Log.w("BLESpam", "isSpammerRunning error for $name: ${e.message}")
                    false
                }

                if (isRunning) {
                    val spammer = try {
                        factory.invoke()
                    } catch (e: Exception) {
                        Log.e("BLESpam", "Failed to create spammer $name", e)
                        continue
                    }

                    if (!spammerList.contains(spammer)) spammerList.add(spammer)
                    runOnUiThread {
                        circle.setImageResource(R.drawable.active_circle)
                        circle.visibility = View.VISIBLE
                        applyActiveCircleColor(circle)
                        val buttonColor = getButtonColor()
                        val textColor = getContrastColor(buttonColor)
                        button.backgroundTintList = ColorStateList.valueOf(buttonColor)
                        try { button.setStrokeColor(ColorStateList.valueOf(buttonColor)) } catch (_: Throwable) {}
                        button.setTextColor(textColor)
                        val blink = startBlinking(circle, spammer, button)
                        spammer.setBlinkRunnable(blink)
                    }
                }
            }
            applyThemeColor()
        } catch (e: Exception) {
            Log.e("BLESpam", "restoreSpammerUiState failed", e)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4)
            }
        }
    }

    private fun setAppTheme(theme: String) {
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun isAppInDarkTheme(): Boolean {
        return when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> {
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }
            else -> {
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val bugButton: ImageView = findViewById(R.id.settingsButton)
        val isDarkTheme = (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        bugButton.setImageResource(if (isDarkTheme) R.mipmap.ic_menu_night else R.mipmap.ic_menu)
        applyThemeColor()
    }

    private fun initializeSpamButtons() {
        try {
            onClickSpamButton(ContinuitySpam(ContinuityType.ACTION, true), "iOS Crash", ios17CrashButton, ios17CrashCircle)
            onClickSpamButton(ContinuitySpam(ContinuityType.ACTION, false), "Apple Action Modal", appleActionModalButton, appleActionModalCircle)
            onClickSpamButton(ContinuitySpam(ContinuityType.DEVICE, false), "Apple Device Popup", appleDevicePopupButton, appleDevicePopupCircle)
            onClickSpamButton(ContinuitySpam(ContinuityType.NOTYOURDEVICE, false), "Apple 'Not Your Device'", appleNotYourDevicePopupButton, appleNotYourDevicePopupCircle)
            onClickSpamButton(VzhuhSpam(), "Vzhuh Spam", vzhuhSpamButton, vzhuhSpamCircle)
            onClickSpamButton(FastPairSpam(), "Android Fast Pair", androidFastPairButton, androidFastPairCircle)
            onClickSpamButton(XiaomiQuickConnect(), "Xiaomi Quick Connect", xiaomiQuickConnectButton, xiaomiQuickConnectCircle)
            onClickSpamButton(EasySetupSpam(EasySetupDevice.type.BUDS), "Samsung Buds", samsungEasyPairBudsButton, samsungEasyPairBudsCircle)
            onClickSpamButton(EasySetupSpam(EasySetupDevice.type.WATCH), "Samsung Watch", samsungEasyPairWatchButton, samsungEasyPairWatchCircle)
            onClickSpamButton(SwiftPairSpam(), "Windows Swift Pair", windowsSwiftPairButton, windowsSwiftPairCircle)
            onClickSpamButton(YandexSpam(), "Yandex", YandexButton, YandexCircle)
        } catch (@Suppress("UNUSED_PARAMETER") e: IOException) {
            Toast.makeText(this, getString(R.string.swiftpair), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDelayButtons() {
        minusDelayButton.setOnClickListener {
            val i = Helper.delays.indexOf(Helper.delay)
            if (i > 0) {
                Helper.delay = Helper.delays[i - 1]
                delayText.text = getString(R.string.delay_text, Helper.delay)
                Helper.saveDelay(this)

                // Analytics: Delay changed
                firebaseAnalytics.logEvent("delay_changed") {
                    param("new_delay_ms", Helper.delay.toLong())
                    param("direction", "decreased")
                }
            }
        }

        plusDelayButton.setOnClickListener {
            val i = Helper.delays.indexOf(Helper.delay)
            if (i < Helper.delays.size - 1) {
                Helper.delay = Helper.delays[i + 1]
                delayText.text = getString(R.string.delay_text, Helper.delay)
                Helper.saveDelay(this)

                // Analytics: Delay changed
                firebaseAnalytics.logEvent("delay_changed") {
                    param("new_delay_ms", Helper.delay.toLong())
                    param("direction", "increased")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_ALL_PERMISSIONS -> {
                val deniedPermissions = mutableListOf<String>()
                permissions.forEachIndexed { index, permission ->
                    if (grantResults.getOrNull(index) != PackageManager.PERMISSION_GRANTED) {
                        deniedPermissions.add(permission)
                    }
                }

                if (deniedPermissions.isEmpty()) {
                    Log.d("BLESpam", "Все разрешения получены.")

                    // Analytics: All permissions granted
                    firebaseAnalytics.logEvent("all_permissions_granted") {
                        param("count", permissions.size.toLong())
                    }

                    createHandler().postDelayed({
                    }, 500)
                    checkForNewVersion()
                    completeInitialization()
                } else {
                    Log.w("BLESpam", "Отклонены следующие разрешения: ${deniedPermissions.joinToString(", ")}")

                    // Analytics: Permissions denied
                    firebaseAnalytics.logEvent("permissions_denied") {
                        param("denied_count", deniedPermissions.size.toLong())
                        param("denied_list", deniedPermissions.joinToString(","))
                    }

                    Toast.makeText(
                        this,
                        if (deniedPermissions.any { it == Manifest.permission.POST_NOTIFICATIONS }) {
                            getString(R.string.notifications_permission_required)
                        } else {
                            getString(R.string.permissions_denied)
                        },
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            2 -> {
                val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

                // Analytics: Bluetooth permission result
                firebaseAnalytics.logEvent("permission_result") {
                    param("permission_type", "bluetooth")
                    param("granted", if (granted) "yes" else "no")
                }

                if (granted) {
                    Log.d("BLESpam", "Bluetooth permission granted, checking if enabled")
                    if (!checkBluetoothEnabled()) {
                        createHandler().postDelayed({
                            promptToEnableBluetooth()
                        }, 200)
                    }
                } else {
                    Log.w("BLESpam", "Bluetooth permission denied")
                    Toast.makeText(this, getString(R.string.bluetooth_permission_required), Toast.LENGTH_SHORT).show()
                }
            }
            3 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("BLESpam", "Storage permission granted")
                    Toast.makeText(this, getString(R.string.storage_permission_granted), Toast.LENGTH_SHORT).show()
                } else {
                    Log.w("BLESpam", "Storage permission denied")
                    Toast.makeText(this, getString(R.string.storage_permission_denied), Toast.LENGTH_SHORT).show()
                }
            }
            4 -> {
                val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

                // Analytics: Notification permission result
                firebaseAnalytics.logEvent("permission_result") {
                    param("permission_type", "notification")
                    param("granted", if (granted) "yes" else "no")
                }

                if (granted) {
                    Log.d("BLESpam", "Notification permission granted")
                    fetchFCMToken()
                    restoreSpammerUiState()
                } else {
                    Log.w("BLESpam", "Notification permission denied")
                    Toast.makeText(this, getString(R.string.notifications_permission_denied), Toast.LENGTH_SHORT).show()
                    if (!canSpammerWork()) {
                        SpamService.stopAllSpammers(this)
                        updateLogoAnimation()
                    }
                }
            }
        }
    }

    private fun stopAllSpammers() {
        spammerList.forEach { spammer ->
            if (spammer.isSpamming()) {
                spammer.stop()
            }
        }

        if (spammerList.none { it.isSpamming() }) {
            SpamService.stopAllSpammers(this)

            // Analytics: All spammers stopped
            firebaseAnalytics.logEvent("all_spammers_stopped") {
                param("method", "manual")
            }
        }

        if (!isFinishing && !isDestroyed) {
            logo.postDelayed({
                updateLogoAnimation()
            }, 100)
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            packageInfo.versionName ?: "0.0"
        } catch (@Suppress("UNUSED_PARAMETER") e: PackageManager.NameNotFoundException) {
            "0.0"
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("StringFormatMatches")
    private fun checkForNewVersion() {
        if (!isNetworkAvailable()) {
            runOnUiThread {
                Toast.makeText(this, R.string.no_network_connection, Toast.LENGTH_SHORT).show()
            }
            return
        }

        Thread {
            try {
                val request = Request.Builder()
                    .url(versionCheckApi)
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("HTTP error: ${response.code}")
                    }

                    val jsonResponse = response.body?.string() ?: throw IOException("Empty response")
                    val jsonObject = JSONObject(jsonResponse)

                    val latestVersion = jsonObject.getString("version")
                    val releaseNotes = jsonObject.getString("release_notes")
                    val downloadUrl = jsonObject.optString("download_url", "")
                    val openInRuStore = useRuStoreForUpdates
                    val ruStoreUrl = ruStoreAppUrl
                    val minSupportedVersion = jsonObject.optString("min_supported_version", "0.0")
                    val blockedVersions = jsonObject.optJSONArray("blocked_versions")?.let { jsonArray ->
                        (0 until jsonArray.length()).map { jsonArray.getString(it) }
                    } ?: emptyList()

                    val currentVersion = getAppVersion()

                    runOnUiThread {
                        when {
                            blockedVersions.contains(currentVersion) -> {
                                showUpdateDialog(
                                    title = getString(R.string.update_required_title),
                                    message = getString(R.string.blocked_version_message, currentVersion, releaseNotes),
                                    isForced = true,
                                    downloadUrl = downloadUrl,
                                    openInRuStore = openInRuStore,
                                    ruStoreUrl = ruStoreUrl
                                )
                            }
                            isVersionNewer(minSupportedVersion, currentVersion) -> {
                                showUpdateDialog(
                                    title = getString(R.string.update_required_title),
                                    message = getString(R.string.update_message, latestVersion, releaseNotes),
                                    isForced = true,
                                    downloadUrl = downloadUrl,
                                    openInRuStore = openInRuStore,
                                    ruStoreUrl = ruStoreUrl
                                )
                            }
                            isVersionNewer(latestVersion, currentVersion) -> {
                                showUpdateDialog(
                                    title = getString(R.string.update_available_title),
                                    message = getString(R.string.update_available_message, latestVersion, releaseNotes),
                                    isForced = false,
                                    downloadUrl = downloadUrl,
                                    openInRuStore = openInRuStore,
                                    ruStoreUrl = ruStoreUrl
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BLESpam", "Version check failed", e)
                runOnUiThread {
                    Toast.makeText(this, R.string.update_check_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun openRuStore(ruStoreUrl: String, dialog: AlertDialog) {
        val fallbackUrl = ruStoreAppUrl
        val targetUrl = if (isValidUrl(ruStoreUrl)) ruStoreUrl else fallbackUrl

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
            dialog.dismiss()
        } catch (e: ActivityNotFoundException) {
            Log.e("BLESpam", "No app to open RuStore URL: $targetUrl", e)
            Toast.makeText(this, getString(R.string.invalid_download_url), Toast.LENGTH_LONG).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.activeNetwork != null
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }

    private fun hasEnoughStorage(): Boolean {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val freeSpace = downloadsDir.freeSpace
        return freeSpace > 100 * 1024 * 1024
    }

    private fun isValidUrl(urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            url.protocol == "https"
        } catch (e: Exception) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun downloadApk(downloadUrl: String, dialog: AlertDialog) {
        if (!isNetworkAvailable()) {
            runOnUiThread {
                Toast.makeText(this, getString(R.string.no_network_connection), Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
            return
        }

        if (!hasEnoughStorage()) {
            runOnUiThread {
                Toast.makeText(this, getString(R.string.insufficient_storage), Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
            return
        }

        if (!isValidUrl(downloadUrl)) {
            runOnUiThread {
                Toast.makeText(this, getString(R.string.invalid_download_url), Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            !hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 3)
            return
        }

        runOnUiThread {
            val progressContainer = dialog.findViewById<LinearLayout>(R.id.progress_container)
            if (progressContainer != null) {
                progressContainer.visibility = View.VISIBLE
                downloadProgressBar.visibility = View.VISIBLE
                downloadProgressBar.isIndeterminate = false
            } else {
                Toast.makeText(this, getString(R.string.error_progress_bar), Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
        }

        Thread {
            var outputStream: FileOutputStream? = null
            try {
                val fileName = "BLESpam-${System.currentTimeMillis()}.apk"
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val apkFile = File(downloadsDir, fileName)
                outputStream = FileOutputStream(apkFile)

                val request = Request.Builder()
                    .url(downloadUrl)
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("HTTP error code: ${response.code}")
                    }

                    val body = response.body ?: throw IOException("Empty response body")
                    val fileLength = body.contentLength()
                    body.byteStream().use { inputStream ->
                        val buffer = ByteArray(4096)
                        var totalBytesRead = 0L
                        var lastUpdateTime = System.currentTimeMillis()
                        var lastBytesRead = 0L

                        while (true) {
                            val bytesRead = inputStream.read(buffer)
                            if (bytesRead == -1) break
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastUpdateTime >= 200) {
                                val progress = if (fileLength > 0) ((totalBytesRead * 100) / fileLength).toInt() else 0
                                val speed = if (currentTime > lastUpdateTime) {
                                    ((totalBytesRead - lastBytesRead) * 1000 / (currentTime - lastUpdateTime) / 1024).toInt()
                                } else 0

                                runOnUiThread {
                                    downloadProgressBar.progress = progress
                                    dialog.findViewById<TextView>(R.id.progress_text)?.text =
                                        getString(R.string.progress_text, progress, speed)
                                }
                                lastBytesRead = totalBytesRead
                                lastUpdateTime = currentTime
                            }
                        }
                    }
                }

                outputStream.flush()

                runOnUiThread {
                    dialog.findViewById<LinearLayout>(R.id.progress_container)?.visibility = View.GONE
                    Toast.makeText(this, getString(R.string.download_completed), Toast.LENGTH_SHORT).show()

                    val contentUri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        apkFile
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        !packageManager.canRequestPackageInstalls()
                    ) {
                        val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                            .setData(Uri.parse("package:$packageName"))
                        installPermissionLauncher.launch(permissionIntent)
                        sharedPref.edit().putString("pending_apk_uri", contentUri.toString()).apply()
                    } else {
                        installApk(contentUri)
                    }
                }
            } catch (e: Exception) {
                Log.e("BLESpam", "Download failed", e)
                runOnUiThread {
                    dialog.findViewById<LinearLayout>(R.id.progress_container)?.visibility = View.GONE
                    Toast.makeText(
                        this,
                        getString(R.string.download_failed, e.message ?: getString(R.string.unknown_error)),
                        Toast.LENGTH_LONG
                    ).show()
                    dialog.dismiss()
                }
            } finally {
                outputStream?.close()
                progressHandler.removeCallbacksAndMessages(null)
            }
        }.start()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showUpdateDialog(
        title: String,
        message: String,
        isForced: Boolean,
        downloadUrl: String,
        openInRuStore: Boolean,
        ruStoreUrl: String
    ) {
        val layoutRes = R.layout.activity_update

        val dialogView = layoutInflater.inflate(layoutRes, null)
        downloadProgressBar = dialogView.findViewById<ProgressBar>(R.id.download_progress)
            ?: throw IllegalStateException(getString(R.string.progress_bar_not_found))
        dialogView.findViewById<LinearLayout>(R.id.progress_container)
            ?: throw IllegalStateException(getString(R.string.progress_container_not_found))
        dialogView.findViewById<TextView>(R.id.progress_text)
            ?: throw IllegalStateException(getString(R.string.progress_text_not_found))

        dialogView.findViewById<TextView>(R.id.update_title).text = title
        dialogView.findViewById<TextView>(R.id.update_notes).text = message

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(!isForced)
            .create()

        dialogView.findViewById<Button>(R.id.btn_update).setOnClickListener {
            if (openInRuStore) {
                openRuStore(ruStoreUrl, dialog)
            } else {
                downloadApk(downloadUrl, dialog)
            }
        }

        dialogView.findViewById<Button>(R.id.btn_later).setOnClickListener {
            if (!isForced) {
                dialog.dismiss()
            }
        }

        if (isForced) {
            dialogView.findViewById<Button>(R.id.btn_later).visibility = View.GONE
        }

        dialog.setOnDismissListener {
            progressHandler.removeCallbacksAndMessages(null)
        }

        try {
            dialog.show()
            dialog.window?.apply {
                setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.rounded_dialog_background))
                val params = attributes
                params.width = WindowManager.LayoutParams.MATCH_PARENT
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                params.gravity = Gravity.CENTER
                attributes = params
                decorView.setPadding(0, 0, 0, 0)
            }
        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
            runOnUiThread {
                Toast.makeText(this, getString(R.string.update_dialog_error), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Analytics: Screen view
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, "MainActivity")
            param(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
        }

        resetAllSpammerButtonsUi()
        restoreSpammerUiState()
        updateLogoAnimation()
    }

    override fun onDestroy() {
        if (isChangingConfigurations) {
            Log.d("BLESpam", "onDestroy: configuration change, keeping spammers running")
        }

        // Очистить ВСЕ Handler
        handlers.forEach { it.removeCallbacksAndMessages(null) }
        handlers.clear()

        blinkHandler.removeCallbacksAndMessages(null)
        progressHandler.removeCallbacksAndMessages(null)

        // Закрыть OkHttpClient
        try {
            val executorService = okHttpClient.dispatcher.executorService
            executorService.execute {
                try {
                    okHttpClient.connectionPool.evictAll()
                } catch (e: Exception) {
                    Log.e("BLESpam", "Error evicting connection pool", e)
                }
            }
            executorService.shutdown()
        } catch (e: Exception) {
            Log.e("BLESpam", "Error closing OkHttpClient", e)
        }

        // Остановить AnimationDrawable
        if (::logo.isInitialized) {
            (logo.drawable as? AnimationDrawable)?.stop()
        }

        try {
            if (!isDestroyed && !isFinishing) {
                Glide.with(this).clear(logo)
            } else {
                Glide.with(applicationContext).clear(logo)
            }
        } catch (e: Exception) {
            Log.e("BLESpam", "Error clearing Glide in onDestroy", e)
        }

        super.onDestroy()
    }

    @SuppressLint("StringFormatInvalid")
    private fun installApk(uri: Uri) {
        Log.d("BLESpam", "Installing APK with URI: $uri, Scheme: ${uri.scheme}")
        try {
            if (uri.scheme != "content") {
                throw IllegalArgumentException("Expected content URI, got: ${uri.scheme}")
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val externalApkFile = File(downloadsDir, "temp_installer_${System.currentTimeMillis()}.apk")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(externalApkFile).use { output ->
                    val bytesCopied = input.copyTo(output)
                    Log.d("BLESpam", "Bytes copied to external file: $bytesCopied")
                }
            } ?: throw IOException("Failed to open input stream for URI: $uri")

            if (!externalApkFile.exists() || externalApkFile.length() == 0L) {
                throw IllegalStateException("Invalid APK file: exists=${externalApkFile.exists()}, size=${externalApkFile.length()}")
            }

            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageArchiveInfo(externalApkFile.absolutePath, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageArchiveInfo(externalApkFile.absolutePath, 0)
            } ?: throw IllegalStateException("Invalid APK file: Unable to read package info")

            Log.d("BLESpam", "APK package: ${packageInfo.packageName}, version: ${packageInfo.versionName}")

            val apkUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                externalApkFile
            )
            Log.d("BLESpam", "FileProvider URI: $apkUri, File path: ${externalApkFile.absolutePath}")

            @Suppress("DEPRECATION")
            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                startActivity(installIntent)
                Log.d("BLESpam", "Install intent started successfully")
            } catch (e: ActivityNotFoundException) {
                Log.e("BLESpam", "No app to handle install intent: ${e.message}")
                showInstallError(getString(R.string.no_install_app))
            } catch (e: SecurityException) {
                Log.e("BLESpam", "Security exception during install: ${e.message}")
                showInstallError(getString(R.string.install_permission_denied))
            }
        } catch (e: Exception) {
            Log.e("BLESpam", "Error preparing APK file: ${e.message}", e)
            showInstallError(getString(R.string.installation_error, e.message ?: getString(R.string.unknown_error)))
        }
    }

    private fun showInstallError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun isVersionNewer(newVersion: String, currentVersion: String): Boolean {
        fun normalize(v: String) = v.replace("[^\\d.]".toRegex(), "")
            .split(".")
            .mapNotNull { it.toIntOrNull() }

        val newParts = normalize(newVersion)
        val currParts = normalize(currentVersion)

        val maxLength = maxOf(newParts.size, currParts.size)
        val paddedNew = newParts + List(maxLength - newParts.size) { 0 }
        val paddedCurr = currParts + List(maxLength - currParts.size) { 0 }

        for (i in 0 until maxLength) {
            if (paddedNew[i] > paddedCurr[i]) return true
            if (paddedNew[i] < paddedCurr[i]) return false
        }
        return false
    }

    private fun checkBluetoothEnabled(): Boolean {
        val bluetoothAdapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bluetoothManager = getSystemService(android.bluetooth.BluetoothManager::class.java)
            bluetoothManager?.adapter
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
        }
        if (bluetoothAdapter == null) {
            Toast.makeText(this, getString(R.string.bluetooth_not_supported), Toast.LENGTH_SHORT).show()
            return false
        }
        return bluetoothAdapter.isEnabled
    }

    private fun isBluetoothEnabledSilent(): Boolean {
        val bluetoothAdapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bluetoothManager = getSystemService(android.bluetooth.BluetoothManager::class.java)
            bluetoothManager?.adapter
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
        }
        return bluetoothAdapter?.isEnabled ?: false
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun canSpammerWork(): Boolean {
        val hasNotification = hasNotificationPermission()
        val hasBluetooth = isBluetoothEnabledSilent()
        return hasNotification || hasBluetooth
    }

    private fun vibrate(pattern: LongArray) {
        val isVibrationEnabled = sharedPref.getBoolean("vibration_enabled", true)
        if (!isVibrationEnabled || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.w("BLESpam", "Vibration failed", e)
        }
    }

    private fun vibrateStart() {
        vibrate(longArrayOf(0, 50, 50, 50))
    }

    private fun vibrateStop() {
        vibrate(longArrayOf(0, 30))
    }

    private fun promptToEnableBluetooth() {
        if (isBluetoothRequestPending) {
            Log.d("BLESpam", "Bluetooth request already pending")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                Log.d("BLESpam", "Requesting BLUETOOTH_CONNECT permission")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    2
                )
                return
            }
        } else {
            if (!hasPermission(Manifest.permission.BLUETOOTH)) {
                Log.d("BLESpam", "Requesting BLUETOOTH permission")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH),
                    2
                )
                return
            }
        }

        Log.d("BLESpam", "Launching Bluetooth enable request")
        isBluetoothRequestPending = true
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        try {
            enableBluetoothLauncher.launch(enableBtIntent)
        } catch (e: Exception) {
            Log.e("BLESpam", "Failed to launch Bluetooth enable request", e)
            isBluetoothRequestPending = false
            Toast.makeText(this, getString(R.string.bluetootherror), Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateLogoAnimation() {
        if (!::logo.isInitialized) return

        if (isFinishing || isDestroyed) return

        val isAnimationEnabled = sharedPref.getBoolean("logo_animation", false)
        val isAnySpammerActive = SpamService.getActiveSpammers().isNotEmpty()

        try {
            if (isAnimationEnabled && isAnySpammerActive) {
                val isDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                val animResource = if (isDarkTheme) {
                    R.drawable.anim_frames_white
                } else {
                    R.drawable.anim_frames_black
                }

                if (logo.drawable !is AnimationDrawable || logo.tag != animResource) {
                    logo.setImageResource(animResource)
                    logo.tag = animResource
                }

                logo.post {
                    (logo.drawable as? AnimationDrawable)?.let { anim ->
                        if (!anim.isRunning) {
                            anim.start()
                        }
                    }
                }
            } else {
                (logo.drawable as? AnimationDrawable)?.stop()
                if (logo.tag != "static") {
                    logo.setImageResource(R.drawable.logo)
                    logo.tag = "static"
                }
            }
        } catch (e: Exception) {
            Log.e("BLESpam", "Error in updateLogoAnimation", e)
            logo.setImageResource(R.drawable.logo)
            logo.tag = "static"
        }
    }


    private fun isColorTooDark(color: Int): Boolean {
        val brightness = (Color.red(color) * 299 +
                Color.green(color) * 587 +
                Color.blue(color) * 114) / 1000
        return brightness < 40
    }

    private fun isColorTooLight(color: Int): Boolean {
        val brightness = (Color.red(color) * 299 +
                Color.green(color) * 587 +
                Color.blue(color) * 114) / 1000
        return brightness > 215
    }

    private fun getCustomHexColor(): Int? {
        val mode = sharedPref.getString("color_mode", "material") ?: "material"
        if (mode != "custom") return null

        val hex = sharedPref.getString("custom_color", null) ?: return null
        return try {
            Color.parseColor("#$hex")
        } catch (_: Exception) {
            null
        }
    }

    private fun applyActiveCircleColor(circle: ImageView) {
        val customColor = getCustomHexColor()

        if (customColor == null) {
            circle.imageTintList = null
            circle.clearColorFilter()
            return
        }

        // When the custom color is too light (e.g. white), tinting the circle black makes it
        // invisible on the white button. Instead tint it with a contrasting theme color.
        // When too dark, tint with white so it stays visible.
        val tintColor = when {
            isColorTooDark(customColor) -> Color.WHITE
            isColorTooLight(customColor) -> {
                // Use the dark theme secondary text color as a visible alternative to pure black
                resolveAttrColor(android.R.attr.textColorSecondary)
            }
            else -> null
        }

        if (tintColor != null) {
            circle.imageTintList = ColorStateList.valueOf(tintColor)
        } else {
            circle.imageTintList = null
            circle.clearColorFilter()
        }
    }

    private fun onClickSpamButton(
        spammer: Spammer,
        spammerName: String,
        button: MaterialButton,
        circle: ImageView
    ) {
        if (!spammerList.contains(spammer)) spammerList.add(spammer)

        button.setOnClickListener {
            val isActuallyRunning = try {
                SpamService.isSpammerRunning(spammerName)
            } catch (e: Exception) {
                Log.w("BLESpam", "Failed to check spammer state: ${e.message}")
                false
            }

            if (isActuallyRunning) {
                // Analytics: Spammer stopped
                firebaseAnalytics.logEvent("spammer_stopped") {
                    param("spammer_name", spammerName)
                    param("duration", System.currentTimeMillis())
                }

                try {
                    val blinkRunnable = spammer.getBlinkRunnable()
                    if (blinkRunnable != null) {
                        blinkHandler.removeCallbacks(blinkRunnable)
                        spammer.setBlinkRunnable(noOpRunnable)
                    }
                    SpamService.stopSpammer(this, spammerName)
                    vibrateStop()

                    applyInactiveStyle(button, circle)

                    updateLogoAnimation()

                    if (SpamService.getActiveSpammers().isEmpty()) {
                        SpamService.stopAllSpammers(this)
                    }
                } catch (e: Exception) {
                    Log.e("BLESpam", "Failed to stop spammer $spammerName", e)

                    // Analytics: Spammer stop error
                    firebaseAnalytics.logEvent("spammer_stop_error") {
                        param("spammer_name", spammerName)
                        param("error", e.message ?: "unknown")
                    }
                }
                return@setOnClickListener
            }

            val hasNotification = hasNotificationPermission()
            val hasBluetooth = checkBluetoothEnabled()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotification) {
                requestNotificationPermission()
                Toast.makeText(this, getString(R.string.notifications_permission_required), Toast.LENGTH_SHORT).show()

                // Analytics: Permission required
                firebaseAnalytics.logEvent("permission_required") {
                    param("type", "notification")
                    param("spammer_name", spammerName)
                }

                return@setOnClickListener
            }

            if (!hasBluetooth) {
                promptToEnableBluetooth()

                // Analytics: Bluetooth required
                firebaseAnalytics.logEvent("bluetooth_required") {
                    param("spammer_name", spammerName)
                }

                return@setOnClickListener
            }

            if (!canSpammerWork()) {
                Toast.makeText(this, getString(R.string.notifications_permission_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createHandler().postDelayed({
                try {
                    circle.setImageResource(R.drawable.active_circle)
                    circle.visibility = View.VISIBLE
                    applyActiveCircleColor(circle)
                    val buttonColor = getButtonColor()
                    val textColor = getContrastColor(buttonColor)
                    button.backgroundTintList = ColorStateList.valueOf(buttonColor)
                    try { button.setStrokeColor(ColorStateList.valueOf(buttonColor)) } catch (_: Throwable) {}
                    button.setTextColor(textColor)

                    val blinkRunnable = startBlinking(circle, spammer, button)
                    spammer.setBlinkRunnable(blinkRunnable)

                    SpamService.startSpammer(this, spammerName)
                    vibrateStart()
                    updateLogoAnimation()
                    sharedPref.edit()
                        .putBoolean("force_ui_stopped", false)
                        .apply()

                    // Analytics: Spammer started
                    firebaseAnalytics.logEvent("spammer_started") {
                        param("spammer_name", spammerName)
                        param("timestamp", System.currentTimeMillis())
                    }

                } catch (e: Exception) {
                    Log.e("BLESpam", "Failed to start spammer $spammerName", e)

                    // Analytics: Spammer start error
                    firebaseAnalytics.logEvent("spammer_start_error") {
                        param("spammer_name", spammerName)
                        param("error", e.message ?: "unknown")
                    }

                    runOnUiThread {
                        circle.setImageResource(R.drawable.grey_circle)
                        circle.visibility = View.VISIBLE
                        val strokeColor = resolveAttrColor(android.R.attr.textColorSecondary)
                        button.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                        try { button.setStrokeColor(ColorStateList.valueOf(strokeColor)) } catch (_: Throwable) {}
                        button.setTextColor(strokeColor)
                        updateLogoAnimation()
                    }
                }
            }, 50)
        }
    }

    private fun resolveAttrColor(@AttrRes attr: Int): Int {
        val tv = TypedValue()
        theme.resolveAttribute(attr, tv, true)
        return if (tv.resourceId != 0) {
            ContextCompat.getColor(this, tv.resourceId)
        } else {
            tv.data
        }
    }

    private fun getButtonColor(): Int {
        val colorMode = sharedPref.getString("color_mode", "material") ?: "material"
        return if (colorMode == "custom") {
            val customColorHex = sharedPref.getString("custom_color", "FF6200EE") ?: "FF6200EE"
            try {
                Color.parseColor("#$customColorHex")
            } catch (e: Exception) {
                Log.w("BLESpam", "Invalid custom color: $customColorHex", e)
                resolveAttrColor(com.google.android.material.R.attr.colorTertiary)
            }
        } else {
            // On pre-Android 12 devices, Material You is unavailable — use orange fallback
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                resolveAttrColor(com.google.android.material.R.attr.colorTertiary)
            } else {
                Color.parseColor("#FF6600")
            }
        }
    }

    private fun getContrastColor(color: Int): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val brightness = (red * 299 + green * 587 + blue * 114) / 1000
        return if (brightness > 128) Color.BLACK else Color.WHITE
    }

    private fun applyThemeColor() {
        try {
            val buttonColor = getButtonColor()
            val textColor = getContrastColor(buttonColor)

            plusDelayButton.backgroundTintList = ColorStateList.valueOf(buttonColor)
            plusDelayButton.setTextColor(textColor)

            minusDelayButton.backgroundTintList = ColorStateList.valueOf(buttonColor)
            minusDelayButton.setTextColor(textColor)

            val colorMode = sharedPref.getString("color_mode", "material") ?: "material"

            if (colorMode == "custom") {
                logo.imageTintList = ColorStateList.valueOf(buttonColor)

                val settingsButton: android.widget.ImageView? = findViewById(R.id.settingsButton)
                settingsButton?.imageTintList = ColorStateList.valueOf(buttonColor)
            } else {
                val materialColor = resolveAttrColor(android.R.attr.colorPrimary)
                logo.imageTintList = ColorStateList.valueOf(materialColor)

                val settingsButton: android.widget.ImageView? = findViewById(R.id.settingsButton)
                settingsButton?.imageTintList = ColorStateList.valueOf(materialColor)
            }

        } catch (e: Exception) {
            Log.e("BLESpam", "Error in applyThemeColor", e)
        }
    }

    private fun startBlinking(imageView: ImageView, spammer: Spammer, button: MaterialButton): Runnable {
        imageView.setImageResource(R.drawable.active_circle)
        imageView.visibility = View.VISIBLE
        applyActiveCircleColor(imageView)

        val blinkRunnable = object : Runnable {
            override fun run() {
                val spammerName = getSpammerNameForButton(button)
                val isActuallyRunning = if (spammerName != null) {
                    try {
                        SpamService.isSpammerRunning(spammerName)
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    false
                }

                if (isActuallyRunning && !canSpammerWork()) {
                    SpamService.stopSpammer(this@MainActivity, spammerName ?: "")
                    blinkHandler.removeCallbacks(this)
                    runOnUiThread {
                        applyInactiveStyle(button, imageView)

                        val hasNotification = hasNotificationPermission()
                        val hasBluetooth = isBluetoothEnabledSilent()

                        val message = when {
                            !hasNotification && !hasBluetooth -> getString(R.string.notifications_permission_required)
                            !hasBluetooth -> getString(R.string.bluetoothoff_spammeroff)
                            else -> getString(R.string.notifications_permission_required)
                        }

                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                        updateLogoAnimation()
                    }
                    return
                }

                if (isActuallyRunning) {
                    imageView.visibility = if (imageView.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
                    val delay = if (imageView.visibility == View.VISIBLE) {
                        (Helper.delay / 10).coerceAtLeast(20)
                    } else {
                        Helper.delay
                    }
                    blinkHandler.postDelayed(this, delay.toLong())
                } else {
                    blinkHandler.removeCallbacks(this)
                    runOnUiThread {
                        applyInactiveStyle(button, imageView)
                    }
                }
            }
        }

        blinkHandler.post(blinkRunnable)
        return blinkRunnable
    }

    private fun getSpammerNameForButton(button: MaterialButton): String? {
        return when (button) {
            ios17CrashButton -> "iOS Crash"
            appleActionModalButton -> "Apple Action Modal"
            appleDevicePopupButton -> "Apple Device Popup"
            appleNotYourDevicePopupButton -> "Apple 'Not Your Device'"
            vzhuhSpamButton -> "Vzhuh Spam"
            androidFastPairButton -> "Android Fast Pair"
            xiaomiQuickConnectButton -> "Xiaomi Quick Connect"
            samsungEasyPairBudsButton -> "Samsung Buds"
            samsungEasyPairWatchButton -> "Samsung Watch"
            windowsSwiftPairButton -> "Windows Swift Pair"
            YandexButton -> "Yandex"
            else -> null
        }
    }

    companion object {
        private const val REQUEST_ALL_PERMISSIONS = 100
    }
}

package com.tutozz.blespam

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import java.util.Locale
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.AttrRes
import android.util.Log


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences

    private val permissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionsResult(permissions)
    }

    private fun applyCustomColorToSplash() {
        try {
            val gifImageView = findViewById<ImageView>(R.id.gifImageView)

            val colorMode = sharedPref.getString("color_mode", "material") ?: "material"

            if (colorMode == "custom") {
                val customColorHex = sharedPref.getString("custom_color", "FF6200EE") ?: "FF6200EE"
                try {
                    val customColor = Color.parseColor("#$customColorHex")
                    gifImageView.imageTintList = ColorStateList.valueOf(customColor)
                } catch (e: Exception) {
                    val materialColor = resolveAttrColor(android.R.attr.colorPrimary)
                    gifImageView.imageTintList = ColorStateList.valueOf(materialColor)
                }
            }

        } catch (e: Exception) {
            Log.e("SplashActivity", "Error applying custom color", e)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        val activeSpammers = try {
            SpamService.getActiveSpammers()
        } catch (e: Exception) {
            emptySet()
        }

        if (activeSpammers.isNotEmpty()) {
            super.onCreate(savedInstanceState)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val theme = sharedPref.getString("theme", "auto") ?: "auto"
        setAppTheme(theme)

        val languageCode = sharedPref.getString("language", Locale.getDefault().language) ?: "en"
        NotificationAudienceHelper.syncLanguageAndCountry(sharedPref, languageCode)
        setAppLanguage(languageCode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)
        applyCustomColorToSplash()
        initViews()

        if (hasPermissions()) {
            proceedToMainActivity()
        } else {
            requestPermissions()
        }
    }

    private fun initViews() {
        val gifImageView: ImageView = findViewById(R.id.gifImageView)
        val versionTextView: TextView = findViewById(R.id.versionTextView)

        val isDarkTheme = (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        val animResource = if (isDarkTheme) {
            R.drawable.anim_frames_white
        } else {
            R.drawable.anim_frames_black
        }

        gifImageView.setImageResource(animResource)
        gifImageView.post {
            (gifImageView.drawable as? android.graphics.drawable.AnimationDrawable)?.start()
        }

        versionTextView.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)
    }

    private fun setAppTheme(theme: String) {
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun setAppLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        createConfigurationContext(config)
    }

    private fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val bluetoothScanGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[Manifest.permission.BLUETOOTH_SCAN] == true
        } else {
            true
        }

        when {
            fineLocationGranted && bluetoothScanGranted -> proceedToMainActivity()
            else -> {
                Toast.makeText(this, getString(R.string.perscan), Toast.LENGTH_LONG).show()
                Handler(Looper.getMainLooper()).postDelayed({ finish() }, 2000)
            }
        }
    }

    private fun hasPermissions(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val bluetoothScanPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return fineLocationPermission && bluetoothScanPermission
    }

    private fun requestPermissions() {
        val permissionsNeeded = mutableListOf<String>().apply {
            if (ContextCompat.checkSelfPermission(
                    this@SplashActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(
                    this@SplashActivity,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            permissionRequestLauncher.launch(permissionsNeeded.toTypedArray())
        } else {
            proceedToMainActivity()
        }
    }

    private fun proceedToMainActivity() {
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1600)
    }
}

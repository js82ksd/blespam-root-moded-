package com.tutozz.blespam

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import android.util.TypedValue
import android.widget.ImageView
import androidx.annotation.AttrRes
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private var isSettingsChanged = false
    private lateinit var saveButton: Button

    private var pendingLanguage: String? = null
    private var pendingAnimationEnabled: Boolean? = null
    private var pendingTheme: String? = null
    private var pendingColorMode: String? = null
    private var pendingCustomColor: String? = null
    private var pendingVibrationEnabled: Boolean? = null
    private var pendingExtendedAdvertising: Boolean? = null

    private var originalLanguage: String = "en"
    private var originalAnimationEnabled: Boolean = false
    private var originalTheme: String = "auto"
    private var originalColorMode: String = "material"
    private var originalCustomColor: String = "#FF6200EE"
    private var originalVibrationEnabled: Boolean = true
    private var originalExtendedAdvertising: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)

        originalLanguage = sharedPref.getString("language", Locale.getDefault().language) ?: "en"
        originalAnimationEnabled = sharedPref.getBoolean("logo_animation", false)
        originalTheme = sharedPref.getString("theme", "auto") ?: "auto"
        originalColorMode = sharedPref.getString("color_mode", "material") ?: "material"
        originalCustomColor = sharedPref.getString("custom_color", "#FF6200EE") ?: "#FF6200EE"
        originalVibrationEnabled = sharedPref.getBoolean("vibration_enabled", true)
        originalExtendedAdvertising = sharedPref.getBoolean("extended_advertising_enabled", true)

        setAppLanguage(originalLanguage)
        setAppTheme(originalTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val versionTextView = findViewById<TextView>(R.id.text_app_version)
        versionTextView.text = getString(R.string.app_version, getAppVersion())

        updateDynamicColorsOnStartup()

        saveButton = findViewById(R.id.save_button)
        saveButton.visibility = View.GONE

        saveButton.setOnClickListener {
            if (isSettingsChanged) {
                if (pendingColorMode == "custom" ||
                    (originalColorMode == "custom" && pendingColorMode == null)) {

                    val colorToValidate = pendingCustomColor ?: originalCustomColor
                    val isDark = isDarkTheme()

                    try {
                        val color = Color.parseColor("#" + normalizeHex(colorToValidate))

                        if (hasLowContrast(color, isDark)) {
                            showContrastWarningDialog(
                                color = color,
                                onConfirm = { performSave() },
                                onCancel = { }
                            )
                            return@setOnClickListener
                        }
                    } catch (e: Exception) {
                        Log.e("SettingsActivity", "Error validating color", e)
                    }
                }

                performSave()
            }
        }

        initViews()
    }

    private fun performSave() {
        pendingLanguage?.let { lang ->
            NotificationAudienceHelper.syncLanguageAndCountry(sharedPref, lang)
        }

        pendingAnimationEnabled?.let { isEnabled ->
            sharedPref.edit { putBoolean("logo_animation", isEnabled) }
        }

        pendingTheme?.let { theme ->
            sharedPref.edit { putString("theme", theme) }
        }

        pendingColorMode?.let { mode ->
            sharedPref.edit { putString("color_mode", mode) }
        }

        pendingCustomColor?.let { color ->
            sharedPref.edit { putString("custom_color", color) }
        }

        pendingVibrationEnabled?.let { isEnabled ->
            sharedPref.edit { putBoolean("vibration_enabled", isEnabled) }
        }

        pendingExtendedAdvertising?.let { isEnabled ->
            sharedPref.edit { putBoolean("extended_advertising_enabled", isEnabled) }
        }

        isSettingsChanged = false
        pendingLanguage = null
        pendingAnimationEnabled = null
        pendingTheme = null
        pendingColorMode = null
        pendingCustomColor = null
        pendingVibrationEnabled = null
        pendingExtendedAdvertising = null

        saveButton.visibility = View.GONE

        restartApp()
    }

    private fun normalizeHex(hex: String): String {
        return hex.removePrefix("#").uppercase()
    }


    private fun updateDynamicColorsOnStartup() {
        try {
            val colorMode = sharedPref.getString("color_mode", "material") ?: "material"

            if (colorMode == "custom") {
                val customColorHex = sharedPref.getString("custom_color", "FF6200EE") ?: "FF6200EE"
                try {
                    Color.parseColor("#" + normalizeHex(customColorHex))
                    applyThemeColorsToSettingsUI()
                } catch (e: Exception) {
                    Log.w("SettingsActivity", "Invalid custom color: $customColorHex", e)
                }
            }
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error on startup", e)
        }
    }

    private fun applyThemeColorsToSettingsUI() {
        try {
            val buttonColor = getButtonColor()
            val textColor = getContrastColor(buttonColor)
            val colorMode = sharedPref.getString("color_mode", "material") ?: "material"

            if (colorMode == "custom") {
                findViewById<ImageView>(R.id.logo).imageTintList = ColorStateList.valueOf(buttonColor)

                val bugButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.button_report_bug)
                bugButton.backgroundTintList = ColorStateList.valueOf(buttonColor)
                bugButton.setTextColor(textColor)

                val languageLayout = findViewById<TextInputLayout>(R.id.language_input_layout)
                languageLayout.setBoxStrokeColor(buttonColor)
                languageLayout.setEndIconTintList(ColorStateList.valueOf(buttonColor))

                val themeLayout = findViewById<TextInputLayout>(R.id.theme_input_layout)
                themeLayout.setBoxStrokeColor(buttonColor)
                themeLayout.setEndIconTintList(ColorStateList.valueOf(buttonColor))

                val colorModeLayout = findViewById<TextInputLayout>(R.id.color_mode_input_layout)
                colorModeLayout.setBoxStrokeColor(buttonColor)
                colorModeLayout.setEndIconTintList(ColorStateList.valueOf(buttonColor))

                val customColorLayout = findViewById<TextInputLayout>(R.id.custom_color_layout)
                customColorLayout.setBoxStrokeColor(buttonColor)

                val saveBtnView = findViewById<com.google.android.material.button.MaterialButton>(R.id.save_button)
                saveBtnView.backgroundTintList = ColorStateList.valueOf(buttonColor)
                saveBtnView.setTextColor(textColor)

                val switchLogoAnimation = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_logo_animation)
                switchLogoAnimation.thumbTintList = ColorStateList.valueOf(buttonColor)

                val trackColor = Color.argb(
                    128,
                    Color.red(buttonColor),
                    Color.green(buttonColor),
                    Color.blue(buttonColor)
                )
                switchLogoAnimation.trackTintList = ColorStateList.valueOf(trackColor)

                val switchVibration = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_vibration)
                switchVibration.thumbTintList = ColorStateList.valueOf(buttonColor)
                switchVibration.trackTintList = ColorStateList.valueOf(trackColor)

                val switchExtendedAdv = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_extended_advertising)
                switchExtendedAdv.thumbTintList = ColorStateList.valueOf(buttonColor)
                switchExtendedAdv.trackTintList = ColorStateList.valueOf(trackColor)
            }
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error applying theme colors", e)
        }
    }

    private fun getButtonColor(): Int {
        val colorMode = sharedPref.getString("color_mode", "material") ?: "material"
        return if (colorMode == "custom") {
            val customColorHex = sharedPref.getString("custom_color", "FF6200EE") ?: "FF6200EE"
            try {
                Color.parseColor("#" + normalizeHex(customColorHex))
            } catch (e: Exception) {
                resolveAttrColor(android.R.attr.colorPrimary)
            }
        } else {
            // On pre-Android 12 devices, Material You is unavailable — use orange fallback
            if (isMaterialYouSupported()) {
                resolveAttrColor(android.R.attr.colorPrimary)
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

    private fun resolveAttrColor(@AttrRes attr: Int): Int {
        val tv = TypedValue()
        theme.resolveAttribute(attr, tv, true)
        return if (tv.resourceId != 0) {
            ContextCompat.getColor(this, tv.resourceId)
        } else {
            tv.data
        }
    }

    private fun hasLowContrast(color: Int, isDark: Boolean = isDarkTheme()): Boolean {
        val brightness = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
        // In dark theme warn only if color is too dark (invisible on dark bg)
        // In light theme warn only if color is too light (invisible on light bg)
        return if (isDark) brightness < 30 else brightness > 225
    }

    private fun isMaterialYouSupported(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    }

    private fun isDarkTheme(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    private fun showContrastWarningDialog(
        color: Int,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ) {
        val isDark = isDarkTheme()
        val themeName = getString(if (isDark) R.string.theme_dark_name else R.string.theme_light_name)
        val colorType = getString(if (isDark) R.string.color_type_black else R.string.color_type_white)

        val message = getString(R.string.low_contrast_message, colorType, themeName)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.low_contrast_title))
            .setMessage(message)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(getString(R.string.low_contrast_yes)) { _, _ ->
                onConfirm()
            }
            .setNegativeButton(getString(R.string.low_contrast_no)) { dialog, _ ->
                onCancel()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun validateColorContrastAndApply(
        colorHex: String,
        onApproved: () -> Unit
    ) {
        try {
            val color = Color.parseColor("#" + normalizeHex(colorHex))

            if (hasLowContrast(color)) {
                showContrastWarningDialog(
                    color = color,
                    onConfirm = {
                        Log.i("SettingsActivity", "User approved low contrast color")
                        onApproved()
                    },
                    onCancel = {
                        Log.i("SettingsActivity", "User rejected low contrast color")
                        val customColorInput = findViewById<TextInputEditText>(R.id.custom_color_input)
                        customColorInput.setText(originalCustomColor)
                    }
                )
            } else {
                onApproved()
            }
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error validating color contrast", e)
            onApproved()
        }
    }

    private fun updateDynamicColors() {
        try {
            recreate()
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error updating colors", e)
        }
    }

    @SuppressLint("WrongViewCast")
    private fun initViews() {
        val languageAutoComplete = findViewById<MaterialAutoCompleteTextView>(R.id.language_spinner)
        setupLanguageSpinner(languageAutoComplete)

        val themeAutoComplete = findViewById<MaterialAutoCompleteTextView>(R.id.theme_spinner)
        setupThemeSpinner(themeAutoComplete)

        val colorModeAutoComplete = findViewById<MaterialAutoCompleteTextView>(R.id.color_mode_spinner)
        val customColorLayout = findViewById<TextInputLayout>(R.id.custom_color_layout)
        val customColorInput = findViewById<TextInputEditText>(R.id.custom_color_input)
        setupColorModeSpinner(colorModeAutoComplete, customColorLayout, customColorInput)

        val logoImageView = findViewById<ImageView>(R.id.logo)
        val animationSwitch = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_logo_animation)
        val vibrationSwitch = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_vibration)

        val isAnimationEnabled = sharedPref.getBoolean("logo_animation", false)
        animationSwitch.isChecked = isAnimationEnabled
        updateLogoImage(logoImageView, isAnimationEnabled)

        val isVibrationEnabled = originalVibrationEnabled
        vibrationSwitch.isChecked = isVibrationEnabled

        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != originalVibrationEnabled) {
                pendingVibrationEnabled = isChecked
                isSettingsChanged = true
                saveButton.visibility = View.VISIBLE
            } else {
                pendingVibrationEnabled = null
                checkIfSettingsChanged()
            }
        }

        val switchExtendedAdv = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_extended_advertising)
        switchExtendedAdv.isChecked = originalExtendedAdvertising
        switchExtendedAdv.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != originalExtendedAdvertising) {
                pendingExtendedAdvertising = isChecked
                isSettingsChanged = true
                saveButton.visibility = View.VISIBLE
            } else {
                pendingExtendedAdvertising = null
                checkIfSettingsChanged()
            }
        }

        setupLogoClickListener(logoImageView)
        setupAnimationSwitch(animationSwitch)
        setupCustomColorInput(customColorInput)
    }

    private fun setupColorModeSpinner(
        autoComplete: MaterialAutoCompleteTextView,
        customColorLayout: TextInputLayout,
        customColorInput: TextInputEditText
    ) {
        val colorModes = arrayOf("material", "custom")
        val colorModeNames = arrayOf(
            // Show 'Classic Color' on pre-Android 12 where Material You (dynamic colors) is unavailable
            if (isMaterialYouSupported()) getString(R.string.color_mode_material) else getString(R.string.color_mode_classic),
            getString(R.string.color_mode_custom)
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, colorModeNames)
        autoComplete.setAdapter(adapter)
        autoComplete.inputType = 0
        autoComplete.keyListener = null

        val position = colorModes.indexOf(originalColorMode)
        if (position >= 0) {
            autoComplete.setText(colorModeNames[position], false)
        }

        customColorLayout.visibility = if (originalColorMode == "custom") View.VISIBLE else View.GONE
        if (originalColorMode == "custom") {
            customColorInput.setText(originalCustomColor)
        }

        autoComplete.setOnClickListener {
            autoComplete.showDropDown()
        }

        autoComplete.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            val selectedMode = colorModes[pos]
            val selectedName = colorModeNames[pos]
            customColorLayout.visibility = if (selectedMode == "custom") View.VISIBLE else View.GONE

            if (selectedMode != originalColorMode) {
                pendingColorMode = selectedMode
                isSettingsChanged = true
                saveButton.visibility = View.VISIBLE

                if (selectedMode == "custom") {
                    applyThemeColorsToSettingsUI()
                }
            } else {
                pendingColorMode = null
                checkIfSettingsChanged()
            }

            autoComplete.setText(selectedName, false)
            autoComplete.dismissDropDown()
        }
    }


    private fun setupCustomColorInput(input: TextInputEditText) {
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val colorHex = s.toString().trim()

                if (colorHex.isNotEmpty() && isValidHexColor(colorHex)) {
                    if (colorHex != originalCustomColor) {
                        pendingCustomColor = colorHex
                        isSettingsChanged = true
                        saveButton.visibility = View.VISIBLE

                        try {
                            val color = Color.parseColor("#" + normalizeHex(colorHex))
                            input.setTextColor(color)
                            applyThemeColorsToSettingsUI()
                        } catch (e: Exception) {
                            Log.e("SettingsActivity", "Error parsing color", e)
                        }
                    } else {
                        pendingCustomColor = null
                        checkIfSettingsChanged()
                    }
                }
            }
        })
    }

    private fun isValidHexColor(hex: String): Boolean {
        return try {
            val normalized = if (hex.startsWith("#")) hex else "#$hex"
            Color.parseColor(normalized)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun setupAnimationSwitch(switch: com.google.android.material.materialswitch.MaterialSwitch) {
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != originalAnimationEnabled) {
                pendingAnimationEnabled = isChecked
                isSettingsChanged = true
                saveButton.visibility = View.VISIBLE
            } else {
                pendingAnimationEnabled = null
                checkIfSettingsChanged()
            }
        }
    }

    private fun setupLogoClickListener(logoImageView: ImageView) {
        var lastClickTime = 0L
        logoImageView.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 500) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(AppConfig.ICONCLICK))
                startActivity(intent)
            }
            lastClickTime = currentTime
        }
    }

    private fun setupLanguageSpinner(autoComplete: MaterialAutoCompleteTextView) {
        val languageCodes = resources.getStringArray(R.array.available_language_codes)
        val languageNames = resources.getStringArray(R.array.available_language_names)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, languageNames)
        autoComplete.setAdapter(adapter)
        autoComplete.inputType = 0
        autoComplete.keyListener = null

        val position = languageCodes.indexOf(originalLanguage)
        if (position >= 0) {
            autoComplete.setText(languageNames[position], false)
        }

        autoComplete.setOnClickListener {
            autoComplete.showDropDown()
        }

        autoComplete.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedLang = languageCodes[position]
            val selectedName = languageNames[position]

            if (selectedLang != originalLanguage) {
                pendingLanguage = selectedLang
                isSettingsChanged = true
                saveButton.visibility = View.VISIBLE
            } else {
                pendingLanguage = null
                checkIfSettingsChanged()
            }

            autoComplete.setText(selectedName, false)
            autoComplete.dismissDropDown()
        }
    }

    private fun setupThemeSpinner(autoComplete: MaterialAutoCompleteTextView) {
        val themeCodes = resources.getStringArray(R.array.theme_codes)
        val themeNames = resources.getStringArray(R.array.theme_names)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, themeNames)
        autoComplete.setAdapter(adapter)
        autoComplete.inputType = 0
        autoComplete.keyListener = null

        val position = themeCodes.indexOf(originalTheme)
        if (position >= 0) {
            autoComplete.setText(themeNames[position], false)
        }

        autoComplete.setOnClickListener {
            autoComplete.showDropDown()
        }

        autoComplete.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedTheme = themeCodes[position]
            val selectedName = themeNames[position]

            if (selectedTheme != originalTheme) {
                pendingTheme = selectedTheme
                isSettingsChanged = true
                saveButton.visibility = View.VISIBLE
            } else {
                pendingTheme = null
                checkIfSettingsChanged()
            }

            autoComplete.setText(selectedName, false)
            autoComplete.dismissDropDown()
        }
    }

    private fun checkIfSettingsChanged() {
        isSettingsChanged = pendingLanguage != null ||
                pendingAnimationEnabled != null ||
                pendingTheme != null ||
                pendingColorMode != null ||
                pendingCustomColor != null ||
                pendingVibrationEnabled != null ||
                pendingExtendedAdvertising != null

        saveButton.visibility = if (isSettingsChanged) View.VISIBLE else View.GONE
    }

    private fun setAppTheme(theme: String) {
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun updateLogoImage(imageView: ImageView, isAnimationEnabled: Boolean) {
        if (isAnimationEnabled) {
            val isDarkTheme = (resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val animResource = if (isDarkTheme) {
                R.drawable.anim_frames_white
            } else {
                R.drawable.anim_frames_black
            }

            imageView.setImageResource(animResource)
            imageView.post {
                (imageView.drawable as? android.graphics.drawable.AnimationDrawable)?.start()
            }
        } else {
            imageView.setImageResource(R.drawable.logo)
        }
    }

    private fun setAppLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun openBugReportActivity(view: View) {
        val intent = Intent(this, BugReportActivity::class.java)
        startActivity(intent)
    }

    private fun getAppVersion(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "0.0"
        }
    }

    private fun restartApp() {
        updateDynamicColors()
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}


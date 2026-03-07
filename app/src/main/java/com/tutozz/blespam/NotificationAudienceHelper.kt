package com.tutozz.blespam

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Locale

object NotificationAudienceHelper {
    private const val PREFS_NAME = "AppSettings"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_COUNTRY = "country"

    private val languagePattern = Regex("^[a-z]{2,3}$")

    private val languageToCountry = mapOf(
        "ar" to "SA",
        "be" to "BY",
        "de" to "DE",
        "en" to "US",
        "es" to "ES",
        "fa" to "IR",
        "fr" to "FR",
        "hi" to "IN",
        "it" to "IT",
        "ja" to "JP",
        "kk" to "KZ",
        "ko" to "KR",
        "nl" to "NL",
        "pl" to "PL",
        "pt" to "BR",
        "ru" to "RU",
        "tr" to "TR",
        "uk" to "UA",
        "zh" to "CN"
    )

    private val countryFilterKeys = listOf(
        "country",
        "countries",
        "target_country",
        "target_countries",
        "country_code",
        "country_codes",
        "region",
        "regions"
    )

    private val languageFilterKeys = listOf(
        "lang",
        "langs",
        "language",
        "languages",
        "target_lang",
        "target_language",
        "target_languages"
    )

    fun syncLanguageAndCountry(sharedPref: SharedPreferences, requestedLanguage: String?) {
        val language = normalizeLanguage(requestedLanguage)
        val country = resolveCountryForLanguage(language, Locale.getDefault().country)
        sharedPref.edit {
            putString(KEY_LANGUAGE, language)
            putString(KEY_COUNTRY, country)
        }
    }

    fun getSelectedLanguage(context: Context): String {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return normalizeLanguage(sharedPref.getString(KEY_LANGUAGE, null))
    }

    fun getSelectedCountry(context: Context): String {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedCountry = normalizeCountry(sharedPref.getString(KEY_COUNTRY, null))
        if (storedCountry != null) return storedCountry

        val language = getSelectedLanguage(context)
        val resolvedCountry = resolveCountryForLanguage(language, Locale.getDefault().country)
        sharedPref.edit { putString(KEY_COUNTRY, resolvedCountry) }
        return resolvedCountry
    }

    fun shouldDeliverToCurrentUser(context: Context, data: Map<String, String>): Boolean {
        if (data.isEmpty()) return true

        val countryTargets = extractTargets(data, countryFilterKeys) { it.uppercase(Locale.ROOT) }
        val languageTargets = extractTargets(data, languageFilterKeys) { it.lowercase(Locale.ROOT) }

        if (countryTargets.isEmpty() && languageTargets.isEmpty()) return true

        val userCountry = getSelectedCountry(context)
        val userLanguage = getSelectedLanguage(context)

        val countryAllowed = countryTargets.isEmpty() || matchesCountry(userCountry, countryTargets)
        val languageAllowed = languageTargets.isEmpty() || matchesLanguage(userLanguage, languageTargets)
        return countryAllowed && languageAllowed
    }

    private fun resolveCountryForLanguage(language: String, fallbackCountry: String?): String {
        languageToCountry[language]?.let { return it }
        normalizeCountry(fallbackCountry)?.let { return it }
        return "US"
    }

    private fun normalizeLanguage(language: String?): String {
        val raw = language
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.substringBefore('-')
            ?.substringBefore('_')
            .orEmpty()

        if (raw.matches(languagePattern)) return raw

        val system = Locale.getDefault().language
            .trim()
            .lowercase(Locale.ROOT)
            .substringBefore('-')
            .substringBefore('_')

        return if (system.matches(languagePattern)) system else "en"
    }

    private fun normalizeCountry(country: String?): String? {
        val normalized = country?.trim()?.uppercase(Locale.ROOT).orEmpty()
        return normalized.takeIf { it.matches(Regex("^[A-Z]{2}$")) }
    }

    private fun extractTargets(
        data: Map<String, String>,
        keys: List<String>,
        normalize: (String) -> String
    ): Set<String> {
        return keys.asSequence()
            .mapNotNull { data[it] }
            .flatMap { splitTargets(it).asSequence() }
            .map(normalize)
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun splitTargets(raw: String): List<String> {
        return raw.split(',', ';', '|', ' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun isWildcard(value: String): Boolean {
        return value == "*" || value == "all" || value == "any"
    }

    private fun matchesCountry(userCountry: String, targets: Set<String>): Boolean {
        return targets.any(::isWildcard) || targets.contains(userCountry)
    }

    private fun matchesLanguage(userLanguage: String, targets: Set<String>): Boolean {
        if (targets.any(::isWildcard)) return true
        return targets.any { target ->
            val normalizedTarget = target.substringBefore('-').substringBefore('_')
            normalizedTarget == userLanguage
        }
    }
}

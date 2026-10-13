package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    SYSTEM("", "Predeterminado", "🌐"),
    SPANISH("es", "Español", "🇪🇸"),
    ENGLISH("en", "English", "🇺🇸"),
    PORTUGUESE("pt", "Português", "🇧🇷"),
    FRENCH("fr", "Français", "🇫🇷");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
        }
    }
}

object LocaleManager {
    private const val PREFS_NAME = "app_locale_prefs"
    private const val KEY_LANGUAGE = "selected_language_code"

    private val _currentLanguage = MutableStateFlow(AppLanguage.SYSTEM)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCode = prefs.getString(KEY_LANGUAGE, "") ?: ""
        val language = AppLanguage.fromCode(savedCode)
        _currentLanguage.value = language
        applyLocale(context, language)
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        _currentLanguage.value = language
        applyLocale(context, language)

        val activity = findActivity(context)
        activity?.recreate()
    }

    fun applyLocale(context: Context, language: AppLanguage) {
        val locale = if (language.code.isEmpty()) {
            Locale.getDefault()
        } else {
            Locale.forLanguageTag(language.code)
        }
        Locale.setDefault(locale)

        val resources = context.resources
        val config = Configuration(resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val systemLocaleManager = context.getSystemService(android.app.LocaleManager::class.java)
                if (systemLocaleManager != null) {
                    val localeList = if (language.code.isEmpty()) {
                        LocaleList.getEmptyLocaleList()
                    } else {
                        LocaleList.forLanguageTags(language.code)
                    }
                    systemLocaleManager.applicationLocales = localeList
                }
            } catch (_: Exception) {}
        }
    }

    private fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}

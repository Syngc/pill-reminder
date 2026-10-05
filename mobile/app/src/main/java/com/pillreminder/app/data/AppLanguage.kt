package com.pillreminder.app.data

import android.content.Context
import android.content.res.Configuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppLanguage(val tag: String, val nativeName: String) {
    SPANISH("es", "Español"),
    ENGLISH("en", "English");

    val locale: Locale get() = Locale.forLanguageTag(tag)

    companion object {
        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }

        /** English if the phone is in English; otherwise Spanish, the app's primary audience. */
        fun deviceDefault(): AppLanguage =
            if (Locale.getDefault().language == ENGLISH.tag) ENGLISH else SPANISH
    }
}

/**
 * The language the person chose in the app. Kept separate from the system locale so the
 * alarm service, which can start with no activity open, always knows which language to speak.
 */
class LanguageSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _language = MutableStateFlow(
        AppLanguage.fromTag(prefs.getString(KEY_LANGUAGE, null)) ?: AppLanguage.deviceDefault()
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun set(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.tag).apply()
        _language.value = language
    }

    private companion object {
        const val KEY_LANGUAGE = "language"
    }
}

/** A context whose resources resolve strings in [language]. */
fun Context.withLanguage(language: AppLanguage): Context {
    val config = Configuration(resources.configuration)
    config.setLocale(language.locale)
    return createConfigurationContext(config)
}

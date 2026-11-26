package com.clenova.app.core.localization

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/** Native Android app locales on API 33+, isolated activity locale on older devices. */
object AppLanguages {
    data class Language(val tag: String, val nativeName: String, val englishName: String)
    val supported = listOf(
        Language("en", "English", "English"),
        Language("zh-Hans", "简体中文", "Chinese (Simplified)"),
        Language("hi", "हिन्दी", "Hindi"),
        Language("es", "Español", "Spanish"),
        Language("ar", "العربية", "Arabic"),
        Language("fr", "Français", "French"),
        Language("bn", "বাংলা", "Bengali"),
        Language("pt", "Português", "Portuguese"),
        Language("ru", "Русский", "Russian"),
        Language("ur", "اردو", "Urdu"),
        Language("id", "Bahasa Indonesia", "Indonesian"),
        Language("de", "Deutsch", "German"),
        Language("ja", "日本語", "Japanese"),
        Language("ko", "한국어", "Korean"),
        Language("tr", "Türkçe", "Turkish"),
    )
    private const val PREFS = "clenova_locale"
    private const val KEY = "language_tag"

    fun current(context: Context): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val selected = context.getSystemService(LocaleManager::class.java)?.applicationLocales
            if (selected != null && !selected.isEmpty) {
                return supported.firstOrNull { it.tag == selected[0].toLanguageTag() }?.tag
                    ?: supported.firstOrNull { Locale.forLanguageTag(it.tag).language == selected[0].language }?.tag ?: "en"
            }
            return ""
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
    }

    fun wrapLegacy(context: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return context
        val tag = current(context)
        if (tag.isEmpty() || supported.none { it.tag == tag }) return context
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun select(context: Context, tag: String): Boolean {
        if (tag.isNotEmpty() && supported.none { it.tag == tag }) return false
        if (current(context) == tag) return true
        return runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                val manager = context.getSystemService(LocaleManager::class.java) ?: return false
                manager.applicationLocales = LocaleList.forLanguageTags(tag)
            } else {
                val activity = findActivity(context) ?: return false
                val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).commit()
                if (!saved) return false
                activity.recreate()
            }
            true
        }.getOrDefault(false)
    }

    private fun findActivity(context: Context): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> if (context.baseContext !== context) findActivity(context.baseContext) else null
        else -> null
    }
}

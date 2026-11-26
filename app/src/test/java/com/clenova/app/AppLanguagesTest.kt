package com.clenova.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clenova.app.core.localization.AppLanguages
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class AppLanguagesTest {
    @Test fun supportedLanguagesAreUniqueAndLegacyLocaleUsesTranslatedResources() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(15, AppLanguages.supported.size)
        assertEquals(15, AppLanguages.supported.map { it.tag }.distinct().size)
        val preferences = context.getSharedPreferences("clenova_locale", Context.MODE_PRIVATE)
        try {
            preferences.edit().putString("language_tag", "hi").commit()
            val localized = AppLanguages.wrapLegacy(context)
            assertEquals("hi", localized.resources.configuration.locales[0].language)
            assertEquals("सेटिंग्स", localized.getString(com.example.R.string.settings_title))
            preferences.edit().putString("language_tag", "ar").commit()
            val rtl = AppLanguages.wrapLegacy(context)
            assertEquals(android.view.View.LAYOUT_DIRECTION_RTL, rtl.resources.configuration.layoutDirection)
            assertEquals("الإعدادات", rtl.getString(com.example.R.string.settings_title))
        } finally {
            preferences.edit().clear().commit()
        }
    }
}

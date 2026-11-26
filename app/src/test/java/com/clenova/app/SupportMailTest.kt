package com.clenova.app

import android.content.Intent
import com.clenova.app.feature.settings.SupportMail
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [32])
class SupportMailTest {
    @Test fun mailUsesOnlyOfficialAddressAndEncodesInput() {
        val intent = SupportMail.intent("A & B", "line 1\n? & line 2")
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        val uri = intent.data.toString()
        assertTrue(uri.startsWith("mailto:hello.skyorigin@gmail.com?"))
        assertTrue(uri.contains("subject=A%20%26%20B"))
        assertTrue(uri.contains("body=line%201%0A%3F%20%26%20line%202"))
    }
}

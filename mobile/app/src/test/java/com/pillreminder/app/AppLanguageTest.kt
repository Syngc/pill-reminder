package com.pillreminder.app

import com.pillreminder.app.alarm.Speaker
import com.pillreminder.app.data.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {
    @Test fun parsesStoredTags() {
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromTag("es"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertNull(AppLanguage.fromTag("fr"))
        assertNull(AppLanguage.fromTag(null))
    }

    @Test fun voicesMatchTheChosenLanguage() {
        for (language in AppLanguage.entries) {
            val voices = Speaker.candidateLocales(language)
            assertTrue(voices.isNotEmpty())
            assertTrue(voices.all { it.language == language.tag })
        }
    }
}

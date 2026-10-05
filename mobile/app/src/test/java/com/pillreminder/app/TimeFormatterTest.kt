package com.pillreminder.app

import com.pillreminder.app.ui.TimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TimeFormatterTest {
    @Test fun twentyFourHourPhonesSeeTwentyFourHourTimes() {
        val format = TimeFormatter(Locale.ENGLISH, is24Hour = true)
        assertEquals("05:39", format.format("05:39"))
        assertEquals("17:39", format.format("17:39"))
    }

    @Test fun twelveHourPhonesSeeAmAndPm() {
        val format = TimeFormatter(Locale.ENGLISH, is24Hour = false)
        assertEquals("5:39 AM", format.format("05:39"))
        assertEquals("5:39 PM", format.format("17:39"))
        assertEquals("12:00 AM", format.format("00:00"))
        assertEquals("12:00 PM", format.format("12:00"))
    }

    @Test fun twelveHourSpanishDistinguishesMorningFromAfternoon() {
        val format = TimeFormatter(Locale.forLanguageTag("es"), is24Hour = false)
        val morning = format.format("05:39")
        val afternoon = format.format("17:39")
        assertTrue(morning.startsWith("5:39"))
        assertTrue(afternoon.startsWith("5:39"))
        assertNotEquals(morning, afternoon)
    }
}

package com.pillreminder.app

import com.pillreminder.app.ui.replaceTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TimesEditorTest {
    @Test fun replacingKeepsTheListSorted() {
        assertEquals(listOf("07:30", "20:00"), listOf("08:00", "20:00").replaceTime("08:00", "07:30"))
        assertEquals(listOf("08:00", "22:00"), listOf("08:00", "20:00").replaceTime("20:00", "22:00"))
    }

    @Test fun replacingWithAnExistingTimeMergesThem() {
        assertEquals(listOf("20:00"), listOf("08:00", "20:00").replaceTime("08:00", "20:00"))
    }
}

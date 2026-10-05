package com.pillreminder.app.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shows stored "HH:mm" times the way the phone shows its clock: "17:39", or "5:39 PM"
 * ("5:39 p. m." in Spanish) when the phone uses the 12-hour format. Times are always
 * stored as 24-hour "HH:mm"; only the display changes.
 */
class TimeFormatter(locale: Locale, is24Hour: Boolean) {
    private val formatter = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", locale)

    fun format(time: String): String = format(LocalTime.parse(time))

    fun format(time: LocalTime): String = time.format(formatter)
}

@Composable
fun rememberTimeFormatter(): TimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    return remember(locale, is24Hour) { TimeFormatter(locale, is24Hour) }
}

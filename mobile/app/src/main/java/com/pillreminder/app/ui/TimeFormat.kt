package com.pillreminder.app.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shows stored "HH:mm" times the way the phone shows its clock: "17:39", or "5:39 PM"
 * ("5:39 p. m." in Spanish) when the phone uses the 12-hour format. Times are always
 * stored as 24-hour "HH:mm"; only the display changes.
 */
class TimeFormatter(locale: Locale, private val is24Hour: Boolean) {
    private val formatter = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", locale)
    private val clockPart = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm", locale)
    private val dayPart = DateTimeFormatter.ofPattern("a", locale)

    fun format(time: String): String = format(LocalTime.parse(time))

    fun format(time: LocalTime): String = time.format(formatter)

    /** "2:00" and "PM" separately, for layouts that show the AM/PM smaller; no suffix on 24-hour phones. */
    fun parts(time: String): Pair<String, String?> {
        val parsed = LocalTime.parse(time)
        return parsed.format(clockPart) to (if (is24Hour) null else parsed.format(dayPart))
    }
}

@Composable
fun rememberTimeFormatter(): TimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    return remember(locale, is24Hour) { TimeFormatter(locale, is24Hour) }
}

/**
 * "Monday, October 5" / "lunes, 5 de octubre", in the order each language uses. [capitalize] for
 * a date standing alone; off mid-sentence, where Spanish keeps weekdays lowercase ("Hasta el viernes").
 */
@Composable
fun rememberLongDate(date: LocalDate, capitalize: Boolean = true): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd"), locale)
    }
    val text = date.format(formatter)
    return if (capitalize) text.replaceFirstChar { it.titlecase(locale) } else text
}

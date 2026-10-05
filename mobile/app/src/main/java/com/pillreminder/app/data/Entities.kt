package com.pillreminder.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dose: String,
    /** Times of day as "HH:mm", sorted. */
    val times: List<String>,
    val instructions: String,
    val startDate: LocalDate,
    /** Null means no end date was written on the prescription. */
    val durationDays: Int?,
    /**
     * When the current [times] took effect: when the medicine was added, or its times last edited.
     * Null for medicines saved before this was recorded.
     */
    val scheduleSince: LocalDateTime? = null,
) {
    fun isActiveOn(date: LocalDate): Boolean {
        if (date.isBefore(startDate)) return false
        val days = durationDays ?: return true
        return date.isBefore(startDate.plusDays(days.toLong()))
    }

    /**
     * Whether a dose at [time] on [date] is part of the schedule. A time that had already passed
     * when the schedule took effect doesn't count that day, so it never shows as pending.
     */
    fun isDueAt(date: LocalDate, time: String): Boolean {
        if (time !in times || !isActiveOn(date)) return false
        val since = scheduleSince ?: return true
        return date != since.toLocalDate() ||
            !LocalTime.parse(time).isBefore(since.toLocalTime().withSecond(0).withNano(0))
    }
}

enum class DoseStatus { PENDING, TAKEN, MISSED }

/** One row per medicine per scheduled dose, created when the alarm rings. */
@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["medicationId", "date", "time"], unique = true)],
)
data class DoseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val date: LocalDate,
    val time: String,
    val status: DoseStatus,
    val confirmedAtMillis: Long? = null,
)

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
    /** When it was saved. Null for medicines saved before this was recorded. */
    val addedAt: LocalDateTime? = null,
) {
    fun isActiveOn(date: LocalDate): Boolean {
        if (date.isBefore(startDate)) return false
        val days = durationDays ?: return true
        return date.isBefore(startDate.plusDays(days.toLong()))
    }

    /**
     * Whether a dose at [time] on [date] is part of the schedule. Doses whose time had already
     * passed when the medicine was added don't count, so they never show as pending.
     */
    fun isDueAt(date: LocalDate, time: String): Boolean {
        if (time !in times || !isActiveOn(date)) return false
        val added = addedAt ?: return true
        return date != added.toLocalDate() || !LocalTime.parse(time).isBefore(added.toLocalTime().withSecond(0).withNano(0))
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

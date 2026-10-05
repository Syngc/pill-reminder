package com.pillreminder.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

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
) {
    fun isActiveOn(date: LocalDate): Boolean {
        if (date.isBefore(startDate)) return false
        val days = durationDays ?: return true
        return date.isBefore(startDate.plusDays(days.toLong()))
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

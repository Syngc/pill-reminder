package com.pillreminder.app.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

class MedicationRepository(private val db: AppDatabase) {
    private val medications = db.medicationDao()
    private val doseLogs = db.doseLogDao()

    fun observeMedications(): Flow<List<Medication>> = medications.observeAll()

    fun observeDoseLogs(date: LocalDate): Flow<List<DoseLog>> = doseLogs.observeForDate(date)

    suspend fun all(): List<Medication> = medications.getAll()

    suspend fun dueAt(date: LocalDate, time: String): List<Medication> =
        medications.getAll().filter { it.isDueAt(date, time) }

    suspend fun add(newMedications: List<Medication>) = medications.insertAll(newMedications)

    suspend fun updateTimes(id: Long, times: List<String>, since: LocalDateTime = LocalDateTime.now()) =
        medications.updateTimes(id, times.distinct().sorted(), since)

    suspend fun delete(id: Long) = medications.delete(id)

    suspend fun markRinging(date: LocalDate, time: String, due: List<Medication>) =
        doseLogs.insertIfAbsent(
            due.map { DoseLog(medicationId = it.id, date = date, time = time, status = DoseStatus.PENDING) }
        )

    suspend fun markTaken(date: LocalDate, time: String) =
        doseLogs.resolvePending(date, time, DoseStatus.TAKEN, System.currentTimeMillis())

    suspend fun markMissed(date: LocalDate, time: String) =
        doseLogs.resolvePending(date, time, DoseStatus.MISSED, null)
}

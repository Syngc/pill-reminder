package com.pillreminder.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class Converters {
    @TypeConverter fun fromTimes(times: List<String>): String = times.joinToString(",")
    @TypeConverter fun toTimes(value: String): List<String> = value.split(",").filter { it.isNotBlank() }
    @TypeConverter fun fromDate(date: LocalDate): Long = date.toEpochDay()
    @TypeConverter fun toDate(value: Long): LocalDate = LocalDate.ofEpochDay(value)
    @TypeConverter fun fromStatus(status: DoseStatus): String = status.name
    @TypeConverter fun toStatus(value: String): DoseStatus = DoseStatus.valueOf(value)
}

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY name")
    fun observeAll(): Flow<List<Medication>>

    @Query("SELECT * FROM medications")
    suspend fun getAll(): List<Medication>

    @Insert
    suspend fun insertAll(medications: List<Medication>)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface DoseLogDao {
    @Query("SELECT * FROM dose_logs WHERE date = :date")
    fun observeForDate(date: LocalDate): Flow<List<DoseLog>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(logs: List<DoseLog>)

    @Query(
        "UPDATE dose_logs SET status = :status, confirmedAtMillis = :confirmedAt " +
            "WHERE date = :date AND time = :time AND status = 'PENDING'"
    )
    suspend fun resolvePending(date: LocalDate, time: String, status: DoseStatus, confirmedAt: Long?)
}

@Database(entities = [Medication::class, DoseLog::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun doseLogDao(): DoseLogDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "pill-reminder.db").build()
    }
}

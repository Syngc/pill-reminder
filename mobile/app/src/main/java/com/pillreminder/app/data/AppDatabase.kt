package com.pillreminder.app.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

class Converters {
    @TypeConverter fun fromTimes(times: List<String>): String = times.joinToString(",")
    @TypeConverter fun toTimes(value: String): List<String> = value.split(",").filter { it.isNotBlank() }
    @TypeConverter fun fromDate(date: LocalDate): Long = date.toEpochDay()
    @TypeConverter fun toDate(value: Long): LocalDate = LocalDate.ofEpochDay(value)
    @TypeConverter fun fromDateTime(value: LocalDateTime?): String? = value?.toString()
    @TypeConverter fun toDateTime(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)
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

    @Query("UPDATE medications SET times = :times, scheduleSince = :since WHERE id = :id")
    suspend fun updateTimes(id: Long, times: List<String>, since: LocalDateTime)

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

@Database(
    entities = [Medication::class, DoseLog::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = AppDatabase.RenameAddedAt::class),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun doseLogDao(): DoseLogDao

    @RenameColumn(tableName = "medications", fromColumnName = "addedAt", toColumnName = "scheduleSince")
    class RenameAddedAt : AutoMigrationSpec

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "pill-reminder.db").build()
    }
}

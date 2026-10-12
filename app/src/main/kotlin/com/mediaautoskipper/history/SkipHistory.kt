package com.mediaautoskipper.history

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "skip_record")
data class SkipRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeMillis: Long,
    val pkg: String,
    val title: String,
    val artist: String,
    val reason: String,
)

@Dao
abstract class SkipHistoryDao {
    @Insert
    protected abstract suspend fun insert(record: SkipRecord)

    /** Newest first. An empty [packages] list yields no records. */
    @Query("SELECT * FROM skip_record WHERE pkg IN (:packages) ORDER BY timeMillis DESC, id DESC")
    abstract fun observeByPackages(packages: List<String>): Flow<List<SkipRecord>>

    @Query("SELECT * FROM skip_record ORDER BY timeMillis DESC, id DESC")
    abstract fun observeAll(): Flow<List<SkipRecord>>

    @Query("SELECT COUNT(*) FROM skip_record")
    abstract suspend fun count(): Int

    @Query("DELETE FROM skip_record")
    abstract suspend fun clear()

    @Query(
        "DELETE FROM skip_record WHERE id NOT IN " +
            "(SELECT id FROM skip_record ORDER BY timeMillis DESC, id DESC LIMIT :keep)",
    )
    abstract suspend fun trimTo(keep: Int)

    /** Inserts one record, then deletes the oldest records beyond [MAX_RECORDS]. */
    @Transaction
    open suspend fun record(record: SkipRecord) {
        insert(record)
        trimTo(MAX_RECORDS)
    }

    companion object {
        // Design parameter, not a measurement.
        const val MAX_RECORDS = 1000
    }
}

@Database(entities = [SkipRecord::class], version = 1, exportSchema = false)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun dao(): SkipHistoryDao

    companion object {
        fun open(context: Context): HistoryDatabase =
            Room.databaseBuilder(context.applicationContext, HistoryDatabase::class.java, "history.db").build()
    }
}

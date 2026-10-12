package com.mediaautoskipper.history

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SkipHistoryTest {
    private lateinit var db: HistoryDatabase
    private val dao get() = db.dao()

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HistoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @AfterTest
    fun tearDown() = db.close()

    private fun rec(time: Long, pkg: String = "com.spotify.music", title: String = "t$time") =
        SkipRecord(timeMillis = time, pkg = pkg, title = title, artist = "a", reason = "simplified-title")

    @Test
    fun recordedSkipHasAllFields() = runBlocking {
        dao.record(SkipRecord(timeMillis = 5, pkg = "p", title = "华语精选", artist = "x", reason = "keyword:精选"))
        val r = dao.observeAll().first().single()
        assertEquals(5L, r.timeMillis)
        assertEquals("p", r.pkg)
        assertEquals("华语精选", r.title)
        assertEquals("x", r.artist)
        assertEquals("keyword:精选", r.reason)
    }

    @Test
    fun the1001stRecordDeletesTheOldest() = runBlocking {
        for (i in 1..1001) dao.record(rec(i.toLong()))
        assertEquals(1000, dao.count())
        val all = dao.observeAll().first()
        assertEquals(1001L, all.first().timeMillis)
        assertEquals(2L, all.last().timeMillis)
    }

    @Test
    fun newestFirst() = runBlocking {
        dao.record(rec(1000))
        dao.record(rec(1005))
        assertEquals(listOf(1005L, 1000L), dao.observeAll().first().map { it.timeMillis })
    }

    @Test
    fun filterByPlayer() = runBlocking {
        dao.record(rec(1, pkg = "com.google.android.apps.youtube.music"))
        dao.record(rec(2, pkg = "com.spotify.music"))
        val spotify = dao.observeByPackages(listOf("com.spotify.music")).first()
        assertEquals(listOf("com.spotify.music"), spotify.map { it.pkg })
        assertTrue(dao.observeByPackages(emptyList()).first().isEmpty())
    }

    @Test
    fun clearRemovesEverything() = runBlocking {
        dao.record(rec(1))
        dao.clear()
        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun dislikedFlagIsStoredAndDefaultsToFalse() = runBlocking {
        dao.record(SkipRecord(timeMillis = 1, pkg = "p", title = "a", artist = "x", reason = "r", disliked = true))
        dao.record(SkipRecord(timeMillis = 2, pkg = "p", title = "b", artist = "x", reason = "r"))
        val byTitle = dao.observeAll().first().associateBy { it.title }
        assertTrue(byTitle.getValue("a").disliked)
        assertEquals(false, byTitle.getValue("b").disliked)
    }

    @Test
    fun upgradingFromVersion1KeepsOldRecordsAsNotDisliked() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath("old-history.db").also { it.parentFile?.mkdirs(); it.delete() }
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL(
                "CREATE TABLE skip_record (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "timeMillis INTEGER NOT NULL, pkg TEXT NOT NULL, title TEXT NOT NULL, " +
                    "artist TEXT NOT NULL, reason TEXT NOT NULL)",
            )
            old.execSQL("INSERT INTO skip_record (timeMillis, pkg, title, artist, reason) VALUES (7, 'p', 'old', 'x', 'keyword:Mix')")
            old.version = 1
        }
        val upgraded = Room.databaseBuilder(context, HistoryDatabase::class.java, "old-history.db")
            .addMigrations(HistoryDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val r = upgraded.dao().observeAll().first().single()
            assertEquals("old", r.title)
            assertEquals(false, r.disliked)
            upgraded.dao().record(SkipRecord(timeMillis = 8, pkg = "p", title = "new", artist = "x", reason = "r", disliked = true))
            assertEquals(2, upgraded.dao().count())
        } finally {
            upgraded.close()
        }
    }
}

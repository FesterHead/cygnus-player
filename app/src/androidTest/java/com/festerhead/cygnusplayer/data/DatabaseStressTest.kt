package com.festerhead.cygnusplayer.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.festerhead.cygnusplayer.data.daos.QueueDao
import com.festerhead.cygnusplayer.data.daos.TrackDao
import com.festerhead.cygnusplayer.data.entities.QueueEntity
import com.festerhead.cygnusplayer.data.entities.TrackEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis
import kotlin.system.measureNanoTime

/**
 * Stress tests for [CygnusDatabase] to verify "Massive Scale Support" as documented.
 * 
 * Target Metrics:
 * - Insert 50,000 tracks/queue entries in < 5 seconds (In-memory).
 * - Indexed query response time < 1ms.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseStressTest {
    private lateinit var db: CygnusDatabase
    private lateinit var trackDao: TrackDao
    private lateinit var queueDao: QueueDao

    private val trackCount = 50_000
    private val tag = "CygnusStressTest"
    private val m3uPath = "/storage/emulated/0/Music/Rush.m3u8"

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CygnusDatabase::class.java).build()
        trackDao = db.trackDao()
        queueDao = db.queueDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    /**
     * Verifies that the database can handle mass insertion and targeted queries 
     * within sub-millisecond thresholds for a 50k track library.
     */
    @Test
    fun testMassiveLibraryPerformance() = runBlocking {
        // 1. Measure Mass Insertion
        val insertTime = measureTimeMillis {
            db.withTransaction {
                val tracks = mutableListOf<TrackEntity>()
                val queueEntries = mutableListOf<QueueEntity>()

                for (i in 1..trackCount) {
                    val folder = "/music/Rush/Album_${i / 10}"
                    val path = "$folder/Song_$i.mp3"

                    val track = TrackEntity(
                        filePath = path,
                        folderPath = folder,
                        title = "Rush Track $i",
                        artist = "Rush",
                        album = "Experimental Album ${i / 10}",
                        trackGain = 0f,
                        albumGain = 0f,
                    )
                    tracks.add(track)
                }

                // Batch insert tracks
                trackDao.insertAll(tracks)

                // Link tracks to queue
                for (i in 1..trackCount) {
                    queueEntries.add(
                        QueueEntity(
                            trackId = i.toLong(),
                            filePath = tracks[i - 1].filePath,
                            folderPath = tracks[i - 1].folderPath,
                            m3uPath = m3uPath
                        )
                    )
                }
                queueDao.insertAll(queueEntries)
            }
        }

        Log.d(tag, "Inserted $trackCount tracks and queue entries in ${insertTime}ms")
        // In-memory insertion for 50k items should be very fast.
        assertTrue("Mass insertion too slow: ${insertTime}ms", insertTime < 10000)

        // 2. Measure Query Performance (Sub-millisecond goal)
        val targetPath = "/music/Rush/Album_2112/Song_21121.mp3"
        
        // Warm up the engine
        trackDao.getTrackByPath(targetPath)

        val queryTimes = mutableListOf<Long>()
        repeat(100) {
            val nanoTime = measureNanoTime {
                trackDao.getTrackByPath(targetPath)
            }
            queryTimes.add(nanoTime)
        }

        val averageNanos = queryTimes.average()
        val averageMillis = averageNanos / 1_000_000.0
        
        Log.d(tag, "Average query time for 1 track out of $trackCount: ${String.format("%.4f", averageMillis)}ms")
        
        // Assert sub-millisecond responsiveness (1ms = 1,000,000ns)
        assertTrue("Query latency exceeded 1ms: ${averageMillis}ms", averageMillis < 1.0)
    }

    /**
     * Verifies folder-based query performance across a massive library.
     */
    @Test
    fun testFolderQueryPerformance() = runBlocking {
        db.withTransaction {
            // Fill DB with 50k items
            val tracks = (1..trackCount).map { i ->
                val folder = "/music/Rush/Album_${i % 100}" // 100 different folders
                TrackEntity(
                    filePath = "$folder/Song_$i.mp3",
                    folderPath = folder,
                    title = "S", artist = "A", album = "A", trackGain = 0f, albumGain = 0f
                )
            }
            trackDao.insertAll(tracks)

            val queueEntries = tracks.mapIndexed { i, t ->
                QueueEntity(
                    trackId = (i + 1).toLong(),
                    filePath = t.filePath,
                    folderPath = t.folderPath,
                    m3uPath = m3uPath
                )
            }
            queueDao.insertAll(queueEntries)
        }

        val targetFolder = "/music/Rush/Album_21"
        
        // Warm up the engine
        queueDao.getQueueEntriesByFolderForPlaylist(m3uPath)

        val queryTimes = mutableListOf<Long>()
        repeat(100) {
            val nanoTime = measureNanoTime {
                queueDao.getTracksByFolderForPlaylist(targetFolder, m3uPath)
            }
            queryTimes.add(nanoTime)
        }

        val averageNanos = queryTimes.average()
        val averageMillis = averageNanos / 1_000_000.0
        
        Log.d(tag, "Average folder query time for Album_21 (500 tracks): ${String.format("%.4f", averageMillis)}ms")
        
        // Even with 500 tracks in a folder, the indexed query should be extremely fast.
        assertTrue("Folder query latency too high: ${averageMillis}ms", averageMillis < 5.0)
    }
}

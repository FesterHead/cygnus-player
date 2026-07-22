package com.festerhead.cygnusplayer.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.festerhead.cygnusplayer.data.entities.QueueEntity

/**
 * Data Access Object for the playback queue.
 * Optimized for massive sequence handling and duplicate track support.
 */
@Dao
interface QueueDao {
    /**
     * Inserts a single queue entry.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: QueueEntity): Long

    /**
     * Inserts multiple queue entries efficiently.
     * Used when loading large M3U playlists.
     */
    @androidx.room.Transaction
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entries: List<QueueEntity>)

    /**
     * Retrieves all queue entries in sequential order.
     */
    @Query("SELECT * FROM queue ORDER BY queue_id ASC")
    suspend fun getAllQueueEntries(): List<QueueEntity>

    /**
     * Retrieves only the unique sequential IDs for a specific playlist.
     * Used for low-overhead shuffle mapping.
     */
    @Query("SELECT queue_id FROM queue WHERE m3u_path = :m3uPath ORDER BY queue_id ASC")
    suspend fun getQueueIdsForPlaylist(m3uPath: String): List<Long>

    /**
     * Retrieves all unique folder paths represented in a specific playlist.
     * Used to build the directory list for folder-shuffling modes.
     */
    @Query("SELECT DISTINCT folder_path FROM queue WHERE m3u_path = :m3uPath")
    suspend fun getAllUniqueFoldersForPlaylist(m3uPath: String): List<String>

    /**
     * Retrieves all tracks belonging to a specific folder within a playlist.
     */
    @Query("SELECT * FROM queue WHERE folder_path = :folderPath AND m3u_path = :m3uPath ORDER BY queue_id ASC")
    suspend fun getTracksByFolderForPlaylist(folderPath: String, m3uPath: String): List<QueueEntity>

    /**
     * Retrieves all queue entries grouped by folder path for a specific playlist.
     */
    @Query("SELECT * FROM queue WHERE m3u_path = :m3uPath ORDER BY folder_path ASC, queue_id ASC")
    suspend fun getQueueEntriesByFolderForPlaylist(m3uPath: String): List<QueueEntity>

    /**
     * Retrieves a single queue entry by its unique ID.
     */
    @Query("SELECT * FROM queue WHERE queue_id = :queueId")
    suspend fun getQueueEntryById(queueId: Long): QueueEntity?

    /**
     * Clears all queue entries for a specific playlist.
     */
    @Query("DELETE FROM queue WHERE m3u_path = :m3uPath")
    suspend fun clearQueueForPlaylist(m3uPath: String)

    /**
     * Clears the entire playback queue.
     */
    @Query("DELETE FROM queue")
    suspend fun clearAll()
}

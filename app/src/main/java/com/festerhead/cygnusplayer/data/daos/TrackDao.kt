package com.festerhead.cygnusplayer.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.festerhead.cygnusplayer.data.entities.TrackEntity

/**
 * Data Access Object for the tracks table.
 * Provides optimized queries for track metadata and path-based lookups.
 */
@Dao
interface TrackDao {
    /**
     * Inserts a new track into the database.
     * @param track The track entity to insert.
     * @return The auto-generated row ID of the inserted track.
     * @throws android.database.sqlite.SQLiteConstraintException if a track with the same path exists.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(track: TrackEntity): Long

    /**
     * Inserts multiple tracks efficiently.
     */
    @androidx.room.Transaction
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(tracks: List<TrackEntity>)

    /**
     * Updates an existing track's metadata.
     */
    @androidx.room.Update
    suspend fun update(track: TrackEntity)

    /**
     * Retrieves a track by its unique database ID.
     */
    @Query("SELECT * FROM tracks WHERE track_id = :id")
    suspend fun getTrackById(id: Long): TrackEntity?

    /**
     * Retrieves a track by its unique file path.
     * Used during M3U parsing to link playlist entries to existing metadata.
     */
    @Query("SELECT * FROM tracks WHERE file_path = :filePath")
    suspend fun getTrackByPath(filePath: String): TrackEntity?

    /**
     * Retrieves tracks matching a set of file paths.
     */
    @Query("SELECT * FROM tracks WHERE file_path IN (:filePaths)")
    suspend fun getTracksByPaths(filePaths: List<String>): List<TrackEntity>

    /**
     * Deletes all tracks from the database. Use with caution.
     */
    @Query("DELETE FROM tracks")
    suspend fun deleteAll()
}

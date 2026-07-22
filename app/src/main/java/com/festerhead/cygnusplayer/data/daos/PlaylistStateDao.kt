package com.festerhead.cygnusplayer.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.festerhead.cygnusplayer.data.entities.PlaylistStateEntity

/**
 * Data Access Object for playlist state persistence.
 */
@Dao
interface PlaylistStateDao {
    /**
     * Saves or updates the state for a specific playlist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveState(state: PlaylistStateEntity)

    /**
     * Retrieves the saved state for a specific playlist path.
     */
    @Query("SELECT * FROM playlist_states WHERE m3u_path = :path")
    suspend fun getStateForPlaylist(path: String): PlaylistStateEntity?

    /**
     * Retrieves the most recently used playlist states (for history UI).
     * Ordered by last_opened descending.
     */
    @Query("SELECT * FROM playlist_states ORDER BY last_opened DESC")
    suspend fun getAllStates(): List<PlaylistStateEntity>

    /**
     * Deletes the saved state for a specific playlist.
     */
    @Query("DELETE FROM playlist_states WHERE m3u_path = :path")
    suspend fun deleteState(path: String)
}

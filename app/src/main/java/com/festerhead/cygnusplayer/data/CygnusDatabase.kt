package com.festerhead.cygnusplayer.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.festerhead.cygnusplayer.data.daos.FolderDao
import com.festerhead.cygnusplayer.data.daos.PlaylistStateDao
import com.festerhead.cygnusplayer.data.daos.QueueDao
import com.festerhead.cygnusplayer.data.daos.TrackDao
import com.festerhead.cygnusplayer.data.entities.FolderEntity
import com.festerhead.cygnusplayer.data.entities.PlaylistStateEntity
import com.festerhead.cygnusplayer.data.entities.QueueEntity
import com.festerhead.cygnusplayer.data.entities.TrackEntity

/**
 * Main Room database for Cygnus Player.
 * Stores track metadata, playback sequences, and persistent application state.
 */
@Database(
    entities = [
        TrackEntity::class,
        FolderEntity::class,
        QueueEntity::class,
        PlaylistStateEntity::class,
    ],
    version = 5,
    exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class CygnusDatabase : RoomDatabase() {
    /**
     * Provides access to track-related database operations.
     */
    abstract fun trackDao(): TrackDao

    /**
     * Provides access to folder-related database operations.
     */
    abstract fun folderDao(): FolderDao

    /**
     * Provides access to queue-related database operations.
     */
    abstract fun queueDao(): QueueDao

    /**
     * Provides access to playlist state operations.
     */
    abstract fun playlistStateDao(): PlaylistStateDao
}

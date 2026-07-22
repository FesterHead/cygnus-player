package com.festerhead.cygnusplayer

import android.app.Application
import androidx.room.Room
import com.festerhead.cygnusplayer.core.QueueController
import com.festerhead.cygnusplayer.core.ReplayGainController
import com.festerhead.cygnusplayer.core.ShuffleEngine
import com.festerhead.cygnusplayer.data.CygnusDatabase
import com.festerhead.cygnusplayer.data.PlaylistRepository
import com.festerhead.cygnusplayer.data.metadata.Media3MetadataExtractor
import com.festerhead.cygnusplayer.data.parser.M3uParser

/**
 * Base Application class for Cygnus Player.
 * Manages the singleton database instance and core controllers for the application.
 */
class CygnusApplication : Application() {

    /**
     * The singleton database instance.
     */
    val database: CygnusDatabase by lazy {
        Room.databaseBuilder(
            this,
            CygnusDatabase::class.java,
            "cygnus-db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    /**
     * Singleton instance of the [QueueController] for managing playback sequences.
     */
    val queueController: QueueController by lazy {
        QueueController(
            database.queueDao(),
            database.trackDao()
        )
    }

    /**
     * Singleton instance of the [ReplayGainController].
     */
    val replayGainController: ReplayGainController by lazy {
        ReplayGainController()
    }

    /**
     * Singleton instance of the [PlaylistRepository].
     */
    val playlistRepository: PlaylistRepository by lazy {
        PlaylistRepository(
            context = this,
            database = database,
            m3uParser = M3uParser(),
            metadataExtractor = Media3MetadataExtractor(this),
            trackDao = database.trackDao(),
            queueDao = database.queueDao(),
            playlistStateDao = database.playlistStateDao(),
            shuffleEngine = ShuffleEngine()
        )
    }
}

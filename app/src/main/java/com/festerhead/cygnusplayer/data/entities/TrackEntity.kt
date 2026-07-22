package com.festerhead.cygnusplayer.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a unique audio file in the library.
 *
 * This entity acts as the source of truth for file metadata and ReplayGain values.
 * Note: A single [TrackEntity] can be referenced multiple times in a playback sequence via QueueEntity.
 *
 * @property trackId Unique auto-generated primary key.
 * @property filePath The absolute or relative path to the physical audio file. Must be unique.
 * @property folderPath The path to the parent directory, used for folder-based shuffling.
 * @property title The song title, extracted from metadata tags.
 * @property artist The artist name, extracted from metadata tags.
 * @property album The album name, extracted from metadata tags.
 * @property trackGain The ReplayGain value for individual track normalization.
 * @property albumGain The ReplayGain value for album-level normalization.
 */
@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["file_path"], unique = true),
        Index(value = ["folder_path"])
    ]
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "track_id")
    val trackId: Long = 0,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "content_uri")
    val contentUri: String? = null,

    @ColumnInfo(name = "folder_path")
    val folderPath: String,

    @ColumnInfo(name = "title")
    val title: String = "<not found>",

    @ColumnInfo(name = "artist")
    val artist: String = "<not found>",

    @ColumnInfo(name = "album")
    val album: String = "<not found>",

    @ColumnInfo(name = "track_gain")
    val trackGain: Float? = null,

    @ColumnInfo(name = "album_gain")
    val albumGain: Float? = null
)

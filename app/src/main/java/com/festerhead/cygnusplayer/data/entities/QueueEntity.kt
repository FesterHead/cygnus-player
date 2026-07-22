package com.festerhead.cygnusplayer.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents an entry in the playback queue.
 *
 * This entity is the core of the sequence-critical playback logic. By using a unique 
 * [queueId] (sequence_id) for every entry, we can distinguish between multiple 
 * occurrences of the same physical file in a single playlist.
 *
 * @property queueId Unique sequential ID for this specific position in the queue.
 * @property trackId Reference to the [TrackEntity] containing the metadata.
 * @property filePath Redundant storage of file path for high-performance indexing.
 * @property folderPath Redundant storage of folder path to support `RANDOM_FOLDER_SEQUENTIAL`.
 * @property m3uPath The path to the playlist file this entry belongs to.
 */
@Entity(
    tableName = "queue",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["track_id"],
            childColumns = ["track_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["track_id"]),
        Index(value = ["file_path"]),
        Index(value = ["folder_path"]),
        Index(value = ["m3u_path", "folder_path"]),
        Index(value = ["m3u_path", "queue_id"])
    ]
)
data class QueueEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "queue_id")
    val queueId: Long = 0,

    @ColumnInfo(name = "track_id")
    val trackId: Long,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "folder_path")
    val folderPath: String,

    @ColumnInfo(name = "m3u_path")
    val m3uPath: String
)

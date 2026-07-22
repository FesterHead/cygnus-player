package com.festerhead.cygnusplayer.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists the playback state for a specific M3U/M3U8 playlist.
 *
 * This allows the player to "resume" a playlist exactly where it left off,
 * preserving both the track position and the chosen shuffle mode.
 *
 * @property m3uPath The absolute path to the .m3u or .m3u8 file. Acts as the unique identifier.
 * @property lastQueueId The [QueueEntity.queueId] of the last track played in this playlist.
 * @property shuffleMode The active shuffle mode (e.g., SEQUENTIAL, TRACK_RANDOM, RANDOM_FOLDER_SEQUENTIAL).
 * @property lastOpened Timestamp of when the playlist was last selected.
 * @property mapping The persisted playback sequence (pointer array).
 */
@Entity(tableName = "playlist_states")
data class PlaylistStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "m3u_path")
    val m3uPath: String,

    @ColumnInfo(name = "last_queue_id")
    val lastQueueId: Long,

    @ColumnInfo(name = "shuffle_mode")
    val shuffleMode: ShuffleMode,

    @ColumnInfo(name = "last_opened")
    val lastOpened: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_position_ms")
    val lastPositionMs: Long = 0L,

    @ColumnInfo(name = "mapping")
    val mapping: LongArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlaylistStateEntity

        if (m3uPath != other.m3uPath) return false
        if (lastQueueId != other.lastQueueId) return false
        if (shuffleMode != other.shuffleMode) return false
        if (lastOpened != other.lastOpened) return false
        if (lastPositionMs != other.lastPositionMs) return false
        if (mapping != null) {
            if (other.mapping == null) return false
            if (!mapping.contentEquals(other.mapping)) return false
        } else if (other.mapping != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = m3uPath.hashCode()
        result = (31 * result) + lastQueueId.hashCode()
        result = (31 * result) + shuffleMode.hashCode()
        result = (31 * result) + lastOpened.hashCode()
        result = (31 * result) + lastPositionMs.hashCode()
        result = (31 * result) + (mapping?.contentHashCode() ?: 0)
        return result
    }
}

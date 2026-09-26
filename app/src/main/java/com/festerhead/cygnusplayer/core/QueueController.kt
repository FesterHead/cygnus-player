package com.festerhead.cygnusplayer.core

import com.festerhead.cygnusplayer.data.daos.QueueDao
import com.festerhead.cygnusplayer.data.daos.TrackDao
import com.festerhead.cygnusplayer.data.entities.QueueEntity
import com.festerhead.cygnusplayer.data.entities.TrackEntity

/**
 * Orchestrates the sliding window queue for ExoPlayer.
 * 
 * Specifically designed to maintain an O(1) memory footprint by ensuring only 
 * a maximum of 3 tracks are loaded into the player's active queue at any time, 
 * regardless of the total playlist size (e.g., 50,000+ tracks).
 * 
 * @property queueDao DAO for accessing the unique sequence mappings.
 * @property trackDao DAO for resolving track metadata and file paths.
 */
class QueueController(
    private val queueDao: QueueDao,
    private val trackDao: TrackDao,
) {
    private var currentMapping: LongArray = longArrayOf()
    private var currentIndex: Int = -1
    private var cachedWindow: WindowData? = null

    /**
     * Initializes the controller with a new playback sequence.
     * 
     * @param mapping The complete sequence of [QueueEntity.queueId]s for the playlist.
     * @param startQueueId The [QueueEntity.queueId] to start playback from.
     */
    fun initialize(mapping: LongArray, startQueueId: Long) {
        currentMapping = mapping
        currentIndex = mapping.indexOf(startQueueId).coerceAtLeast(0)
        cachedWindow = null
    }

    /**
     * Clears the current mapping and resets the internal position index.
     */
    fun clear() {
        currentMapping = longArrayOf()
        currentIndex = -1
        cachedWindow = null
    }

    /**
     * Resolves the [QueueEntity] and [TrackEntity] for the tracks in the sliding window.
     * 
     * @return A [WindowData] containing the metadata for the previous, current, and next tracks.
     */
    suspend fun getWindowData(): WindowData {
        val prevId = if (currentIndex > 0) currentMapping[currentIndex - 1] else null
        val currentId = if (currentIndex in currentMapping.indices) currentMapping[currentIndex] else null
        val nextId = if (currentIndex < (currentMapping.size - 1)) currentMapping[currentIndex + 1] else null

        val prev = if ((prevId != null) && (cachedWindow?.prev?.queueId == prevId)) {
            cachedWindow?.prev
        } else {
            prevId?.let { resolveTrack(it) }
        }

        val current = if ((currentId != null) && (cachedWindow?.current?.queueId == currentId)) {
            cachedWindow?.current
        } else {
            currentId?.let { resolveTrack(it) }
        }

        val next = if ((nextId != null) && (cachedWindow?.next?.queueId == nextId)) {
            cachedWindow?.next
        } else {
            nextId?.let { resolveTrack(it) }
        }

        val window = WindowData(prev = prev, current = current, next = next)
        cachedWindow = window
        return window
    }

    /**
     * Returns the currently cached [TrackData] at the center of the sliding window, if resolved.
     *
     * @return The cached [TrackData], or `null` if not yet resolved.
     */
    fun getCurrentTrackData(): TrackData? = cachedWindow?.current

    /**
     * Returns the cached [TrackData] for the next track in the sliding window, if resolved.
     *
     * @return The cached [TrackData], or `null` if not yet resolved or at the end of the queue.
     */
    fun getNextTrackData(): TrackData? = cachedWindow?.next

    /**
     * Updates an in-memory cached track entity when metadata extraction finishes in the background.
     *
     * @param updatedTrack The updated [TrackEntity] with extracted tags.
     */
    fun updateTrackInCache(updatedTrack: TrackEntity) {
        val window = cachedWindow ?: return
        cachedWindow = WindowData(
            prev = if (window.prev?.track?.trackId == updatedTrack.trackId) window.prev.copy(track = updatedTrack) else window.prev,
            current = if (window.current?.track?.trackId == updatedTrack.trackId) window.current.copy(track = updatedTrack) else window.current,
            next = if (window.next?.track?.trackId == updatedTrack.trackId) window.next.copy(track = updatedTrack) else window.next,
        )
    }

    /**
     * Moves the internal pointer forward by one and shifts the cached window forward synchronously.
     * Called when the player transitions to the "next" item in its sliding window.
     */
    fun moveNext() {
        if (currentIndex < currentMapping.size - 1) {
            currentIndex++
            cachedWindow = WindowData(
                prev = cachedWindow?.current,
                current = cachedWindow?.next,
                next = null,
            )
        }
    }

    /**
     * Moves the internal pointer backward by one and shifts the cached window backward synchronously.
     * While Cygnus is minimalist and "No-Skip", the system or car UI might trigger 
     * a previous action which we must handle for state integrity.
     */
    fun movePrevious() {
        if (currentIndex > 0) {
            currentIndex--
            cachedWindow = WindowData(
                prev = null,
                current = cachedWindow?.prev,
                next = cachedWindow?.current,
            )
        }
    }

    /**
     * Returns the [QueueEntity.queueId] of the track currently at the center of the window.
     */
    fun getCurrentQueueId(): Long? {
        return if (currentIndex in currentMapping.indices) currentMapping[currentIndex] else null
    }

    /**
     * Returns the human-readable position string (e.g., "2112/47533").
     */
    fun getPositionString(): String {
        if (currentMapping.isEmpty()) return "0/0"
        return "${currentIndex + 1}/${currentMapping.size}"
    }

    private suspend fun resolveTrack(queueId: Long): TrackData? {
        val queueEntry = queueDao.getQueueEntryById(queueId) ?: return null
        val track = trackDao.getTrackById(queueEntry.trackId) ?: return null
        return TrackData(queueId, track)
    }

    /**
     * Combined data for a track in the window.
     */
    data class TrackData(
        val queueId: Long,
        val track: TrackEntity
    )

    /**
     * Container for the 3-track sliding window.
     */
    data class WindowData(
        val prev: TrackData?,
        val current: TrackData?,
        val next: TrackData?
    )
}

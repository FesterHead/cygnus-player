package com.festerhead.cygnusplayer.core

import com.festerhead.cygnusplayer.data.entities.ShuffleMode
import kotlin.random.Random

/**
 * High-performance playback randomness engine.
 *
 * Specifically engineered to handle massive queues with O(1) memory overhead
 * by utilizing primitive LongArrays.
 */
class ShuffleEngine(private val random: Random = Random.Default) {

    private val folderHistory = mutableListOf<String>()
    private val maxHistorySize = 24

    /**
     * Generates a playback sequence mapping based on the active shuffle mode.
     *
     * @param queueIds The original sequential IDs from the database.
     * @param mode The desired shuffle strategy.
     * @param anchorId If provided, this ID will be placed at the first position (index 0).
     * @param folderMap A map of folder paths to their constituent queue IDs (for folder shuffle).
     * @return A new LongArray representing the playback order.
     */
    fun generateMapping(
        queueIds: LongArray,
        mode: ShuffleMode,
        anchorId: Long? = null,
        folderMap: Map<String, LongArray> = emptyMap(),
    ): LongArray {
        return when (mode) {
            ShuffleMode.SEQUENTIAL -> {
                if (anchorId == null) {
                    queueIds.copyOf()
                } else {
                    // Start from anchor and play forward.
                    // Forward-Only: Discard tracks appearing before anchor.
                    val anchorIndex = queueIds.indexOf(anchorId).coerceAtLeast(0)
                    queueIds.copyOfRange(anchorIndex, queueIds.size)
                }
            }

            ShuffleMode.TRACK_RANDOM -> {
                val pool = if (anchorId != null) {
                    // Forward-Only: Discard the anchor and everything that might have been played.
                    // Since TRACK_RANDOM is total chaos, we just exclude the anchor and shuffle the rest.
                    queueIds.filter { it != anchorId }.toLongArray()
                } else {
                    queueIds.copyOf()
                }

                // Fisher-Yates shuffle
                for (i in pool.size - 1 downTo 1) {
                    val j = random.nextInt(i + 1)
                    val temp = pool[i]
                    pool[i] = pool[j]
                    pool[j] = temp
                }

                if (anchorId != null) {
                    val result = LongArray(pool.size + 1)
                    result[0] = anchorId
                    System.arraycopy(pool, 0, result, 1, pool.size)
                    result
                } else {
                    pool
                }
            }

            ShuffleMode.RANDOM_FOLDER_SEQUENTIAL -> {
                if (folderMap.isEmpty()) return queueIds.copyOf()

                val allFolders = folderMap.keys.toList()
                val effectiveHistorySize = if (allFolders.size < maxHistorySize) 0 else maxHistorySize

                // Determine start folder if anchor provided
                val anchorFolder = if (anchorId != null) {
                    folderMap.entries.find { it.value.contains(anchorId) }?.key
                } else {
                    null
                }

                // Filter out recently played folders and the anchor folder
                val availableFolders = if (effectiveHistorySize > 0) {
                    allFolders.filterNot { it == anchorFolder || folderHistory.contains(it) }
                } else {
                    allFolders.filterNot { it == anchorFolder }
                }.toMutableList()

                if (availableFolders.isEmpty() && allFolders.size > 1) {
                    availableFolders.addAll(allFolders.filterNot { it == anchorFolder })
                    folderHistory.clear()
                }

                availableFolders.shuffle(random)

                // Re-insert anchor folder at the start
                val folderOrder = if (anchorFolder != null) {
                    listOf(anchorFolder) + availableFolders
                } else {
                    availableFolders
                }

                // Calculate total size - Forward-Only discards tracks before anchor in the anchorFolder
                var totalSize = 0
                for (folder in folderOrder) {
                    val tracks = folderMap[folder] ?: continue
                    if (folder == anchorFolder && anchorId != null) {
                        val anchorIndex = tracks.indexOf(anchorId).coerceAtLeast(0)
                        totalSize += (tracks.size - anchorIndex)
                    } else {
                        totalSize += tracks.size
                    }
                }

                val result = LongArray(totalSize)
                var currentIndex = 0

                for (folder in folderOrder) {
                    val tracks = folderMap[folder] ?: continue
                    
                    if (folder == anchorFolder && anchorId != null) {
                        // Forward-Only: Discard tracks before anchor
                        val anchorIndex = tracks.indexOf(anchorId).coerceAtLeast(0)
                        val remainingInFolder = tracks.size - anchorIndex
                        System.arraycopy(tracks, anchorIndex, result, currentIndex, remainingInFolder)
                        currentIndex += remainingInFolder
                    } else {
                        System.arraycopy(tracks, 0, result, currentIndex, tracks.size)
                        currentIndex += tracks.size
                    }

                    // Update history
                    if (effectiveHistorySize > 0) {
                        folderHistory.add(folder)
                        if (folderHistory.size > effectiveHistorySize) {
                            folderHistory.removeAt(0)
                        }
                    }
                }

                result
            }
        }
    }

    /**
     * Resets the folder history buffer.
     */
    fun clearHistory() {
        folderHistory.clear()
    }
}

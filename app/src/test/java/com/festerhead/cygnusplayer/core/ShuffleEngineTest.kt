package com.festerhead.cygnusplayer.core

import com.festerhead.cygnusplayer.data.entities.ShuffleMode
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Unit tests for [ShuffleEngine].
 *
 * Verifies the correctness and performance of different shuffle strategies including
 * SEQUENTIAL, TRACK_RANDOM, and RANDOM_FOLDER_SEQUENTIAL, ensuring consistent mapping
 * and forward-only constraints.
 */
class ShuffleEngineTest {

    private val engine = ShuffleEngine(Random(2112)) // Fixed seed for reproducibility

    /**
     * Tests that [ShuffleMode.SEQUENTIAL] keeps the original track order.
     */
    @Test
    fun testSequentialMapping() {
        val original = longArrayOf(1L, 2L, 3L, 4L, 5L)
        val result = engine.generateMapping(original, ShuffleMode.SEQUENTIAL)
        assertArrayEquals(original, result)
    }

    /**
     * Tests that [ShuffleMode.SEQUENTIAL] with an anchor correctly discards tracks
     * that appear before the anchor, satisfying the forward-only constraint.
     */
    @Test
    fun testSequentialMappingWithAnchorForwardOnly() {
        val original = longArrayOf(1L, 2L, 3L, 4L, 5L)
        val anchor = 3L
        // Forward-Only: 1 and 2 are discarded
        val expected = longArrayOf(3L, 4L, 5L)
        val result = engine.generateMapping(original, ShuffleMode.SEQUENTIAL, anchorId = anchor)
        assertArrayEquals(expected, result)
    }

    /**
     * Tests that [ShuffleMode.TRACK_RANDOM] produces a randomized order of tracks.
     */
    @Test
    fun testTrackRandomMapping() {
        val original = longArrayOf(1L, 2L, 3L, 4L, 5L)
        val result = engine.generateMapping(original, ShuffleMode.TRACK_RANDOM)
        
        assertEquals(original.size, result.size)
        assertTrue(result.all { it in original })
        assertNotEquals(original.toList(), result.toList())
    }

    /**
     * Tests that [ShuffleMode.TRACK_RANDOM] with an anchor forces the anchor to be
     * at the head of the queue, preserving the current state.
     */
    @Test
    fun testTrackRandomMappingWithAnchor() {
        val original = LongArray(100) { it.toLong() + 1 }
        val anchor = 42L
        val result = engine.generateMapping(original, ShuffleMode.TRACK_RANDOM, anchorId = anchor)
        
        assertEquals(original.size, result.size)
        assertEquals(anchor, result[0]) // Anchor must be first
        assertTrue(result.all { it in original })
    }

    /**
     * Tests that [ShuffleMode.RANDOM_FOLDER_SEQUENTIAL] keeps tracks from the same
     * folder together, while randomizing the order of folders.
     */
    @Test
    fun testRandomFolderSequentialMapping() {
        val folderMap = mapOf(
            "Rush" to longArrayOf(1L, 2L, 3L),
            "Moving Pictures" to longArrayOf(4L, 5L, 6L),
            "Hemispheres" to longArrayOf(7L, 8L),
        )
        val allIds = longArrayOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L)
        
        val result = engine.generateMapping(allIds, ShuffleMode.RANDOM_FOLDER_SEQUENTIAL, folderMap = folderMap)
        
        assertEquals(allIds.size, result.size)
        
        // Verify tracks from the same folder are together and in order
        fun verifyFolderIntegrity(folderTracks: LongArray) {
            val firstIndex = result.indexOf(folderTracks[0])
            for (i in folderTracks.indices) {
                assertEquals(folderTracks[i], result[firstIndex + i])
            }
        }
        
        folderMap.values.forEach { verifyFolderIntegrity(it) }
    }

    /**
     * Tests that [ShuffleMode.RANDOM_FOLDER_SEQUENTIAL] respects the forward-only 
     * constraint when an anchor is provided, truncating the folder and discarding 
     * previous folders.
     */
    @Test
    fun testRandomFolderSequentialWithAnchorForwardOnly() {
        val folderMap = mapOf(
            "Rush" to longArrayOf(1L, 2L, 3L),
            "Moving Pictures" to longArrayOf(4L, 5L, 6L),
            "Hemispheres" to longArrayOf(7L, 8L),
        )
        val allIds = longArrayOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L)
        val anchor = 5L // In "Moving Pictures" (Tracks are 4, 5, 6)
        
        val result = engine.generateMapping(allIds, ShuffleMode.RANDOM_FOLDER_SEQUENTIAL, anchorId = anchor, folderMap = folderMap)
        
        // Forward-Only: "Moving Pictures" starts from 5, and 4 is discarded.
        // The total size should be 8 - 1 = 7
        assertEquals(7, result.size)
        assertEquals(5L, result[0])
        assertEquals(6L, result[1])
        // The rest of the folders follow in some random order (e.g., Rush then Hemispheres)
        assertTrue(result.contains(1L))
        assertTrue(result.contains(7L))
        assertTrue(!result.contains(4L)) // Discarded!
    }

    /**
     * Tests that the history buffer logic correctly prevents recently played folders
     * from being selected again, enforcing variety.
     */
    @Test
    fun testFolderHistoryBufferEnabled() {
        engine.clearHistory()
        val manyFolders = (1..100).associateBy({ "F$it" }, { longArrayOf(it.toLong()) })
        val ids = LongArray(100) { it.toLong() + 1 }
        
        // Populate history
        val mapping1 = engine.generateMapping(ids, ShuffleMode.RANDOM_FOLDER_SEQUENTIAL, folderMap = manyFolders)
        assertEquals(100, mapping1.size)
        val historyFrom1 = mapping1.toList().takeLast(24).map { "F$it" }.toSet()
        
        // Next mapping should NOT start with any of the history
        val mapping2 = engine.generateMapping(ids, ShuffleMode.RANDOM_FOLDER_SEQUENTIAL, folderMap = manyFolders)
        assertEquals(100, mapping2.size)
        val firstFolder2 = "F${mapping2[0]}"
        
        assertTrue("Next shuffle started with a folder from history: $firstFolder2", firstFolder2 !in historyFrom1)
    }

    /**
     * Tests that in a realistic library of 250 albums (well above the 24-album limit),
     * [ShuffleMode.RANDOM_FOLDER_SEQUENTIAL] preserves all albums and tracks, and enforces
     * that the 24 most recently queued albums from the previous shuffle are not re-selected
     * among the first 24 albums of the subsequent shuffle.
     */
    @Test
    fun testFolderHistoryBufferWith250Albums() {
        engine.clearHistory()
        val albumCount = 250
        val tracksPerAlbum = 4
        var nextId = 1L
        val folderMap = LinkedHashMap<String, LongArray>()
        for (i in 1..albumCount) {
            val tracks = LongArray(tracksPerAlbum) { nextId++ }
            folderMap["Album_$i"] = tracks
        }
        val allIds = LongArray(albumCount * tracksPerAlbum) { it.toLong() + 1 }

        // Helper function to extract ordered folder list from track ID sequence
        val trackToFolder = mutableMapOf<Long, String>()
        folderMap.forEach { (folder, tracks) ->
            tracks.forEach { trackToFolder[it] = folder }
        }
        fun extractFolderSequence(mapping: LongArray): List<String> {
            val folderOrder = mutableListOf<String>()
            mapping.forEach { trackId ->
                val folder = trackToFolder[trackId] ?: ""
                if (folderOrder.isEmpty() || folderOrder.last() != folder) {
                    folderOrder.add(folder)
                }
            }
            return folderOrder
        }

        // Initial mapping: all 250 albums (1,000 tracks) must be present
        val mapping1 = engine.generateMapping(allIds, ShuffleMode.RANDOM_FOLDER_SEQUENTIAL, folderMap = folderMap)
        assertEquals(allIds.size, mapping1.size)

        val foldersFrom1 = extractFolderSequence(mapping1)
        assertEquals(albumCount, foldersFrom1.size)
        val last24From1 = foldersFrom1.takeLast(24).toSet()
        assertEquals(24, last24From1.size)

        // Second mapping (reshuffle): all 250 albums must still be present
        val mapping2 = engine.generateMapping(allIds, ShuffleMode.RANDOM_FOLDER_SEQUENTIAL, folderMap = folderMap)
        assertEquals(allIds.size, mapping2.size)

        val foldersFrom2 = extractFolderSequence(mapping2)
        assertEquals(albumCount, foldersFrom2.size)
        val first24From2 = foldersFrom2.take(24).toSet()
        assertEquals(24, first24From2.size)

        // Verify the 24-album separation constraint: none of the last 24 albums appear in the first 24 albums
        val overlap = last24From1.intersect(first24From2)
        assertTrue(
            "Expected 24-album separation, but found overlap between last 24 of mapping1 and first 24 of mapping2: $overlap",
            overlap.isEmpty(),
        )

        // Third mapping: verify separation across continuous cycles
        val last24From2 = foldersFrom2.takeLast(24).toSet()
        val mapping3 = engine.generateMapping(allIds, ShuffleMode.RANDOM_FOLDER_SEQUENTIAL, folderMap = folderMap)
        assertEquals(allIds.size, mapping3.size)
        val foldersFrom3 = extractFolderSequence(mapping3)
        val first24From3 = foldersFrom3.take(24).toSet()
        val overlap2 = last24From2.intersect(first24From3)
        assertTrue(
            "Expected 24-album separation between mapping2 and mapping3, but found overlap: $overlap2",
            overlap2.isEmpty(),
        )
    }

    /**
     * Tests that when the total album count is strictly less than 24, the history buffer
     * size evaluates to 0 per specification, ensuring that all albums are included across
     * successive shuffles and no albums are restricted by history filtering.
     */
    @Test
    fun testFolderHistoryDisabledWhenLessThan24Folders() {
        engine.clearHistory()
        val albumCount = 10
        val folderMap = (1..albumCount).associateBy({ "Album_$it" }, { longArrayOf(it.toLong()) })
        val ids = LongArray(albumCount) { it.toLong() + 1 }

        val mapping1 = engine.generateMapping(ids, ShuffleMode.RANDOM_FOLDER_SEQUENTIAL, folderMap = folderMap)
        assertEquals(albumCount, mapping1.size)

        val mapping2 = engine.generateMapping(ids, ShuffleMode.RANDOM_FOLDER_SEQUENTIAL, folderMap = folderMap)
        assertEquals(albumCount, mapping2.size)
        assertEquals(ids.toSet(), mapping1.toSet())
        assertEquals(ids.toSet(), mapping2.toSet())
    }

    /**
     * Tests boundary behavior when the album count is exactly 24.
     * Verifies that all 24 albums are present on the initial shuffle, and that the fallback
     * recovery mechanism cleanly repopulates and reshuffles all 24 albums on subsequent runs
     * without dropping albums or deadlocking.
     */
    @Test
    fun testFolderHistoryBoundaryAtExactly24Folders() {
        engine.clearHistory()
        val albumCount = 24
        val folderMap = (1..albumCount).associateBy({ "Album_$it" }, { longArrayOf(it.toLong()) })
        val ids = LongArray(albumCount) { it.toLong() + 1 }

        val mapping1 = engine.generateMapping(ids, ShuffleMode.RANDOM_FOLDER_SEQUENTIAL, folderMap = folderMap)
        assertEquals(albumCount, mapping1.size)
        assertEquals(ids.toSet(), mapping1.toSet())

        val mapping2 = engine.generateMapping(ids, ShuffleMode.RANDOM_FOLDER_SEQUENTIAL, folderMap = folderMap)
        assertEquals(albumCount, mapping2.size)
        assertEquals(ids.toSet(), mapping2.toSet())
    }

    /**
     * Tests that the shuffle engine handles 50,000 items efficiently within the 
     * required performance threshold.
     */
    @Test
    fun testMassiveQueuePerformance() {
        val count = 50_000
        val original = LongArray(count) { it.toLong() }
        
        val startTime = System.currentTimeMillis()
        val result = engine.generateMapping(original, ShuffleMode.TRACK_RANDOM)
        val duration = System.currentTimeMillis() - startTime
        
        assertEquals(count, result.size)
        assertTrue("Shuffling 50k items took too long: ${duration}ms", duration < 500)
    }
}

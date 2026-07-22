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
        val historyFrom1 = mapping1.asSequence().take(24).map { "F$it" }.toSet()
        
        // Next mapping should NOT start with any of the history
        val mapping2 = engine.generateMapping(ids, ShuffleMode.RANDOM_FOLDER_SEQUENTIAL, folderMap = manyFolders)
        val firstFolder2 = "F${mapping2[0]}"
        
        assertTrue("Next shuffle started with a folder from history: $firstFolder2", firstFolder2 !in historyFrom1)
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

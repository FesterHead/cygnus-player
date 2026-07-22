package com.festerhead.cygnusplayer.data.metadata

import android.content.Context
import androidx.media3.common.Format
import androidx.media3.common.Metadata
import androidx.media3.common.TrackGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.ApicFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for the mapping logic in [Media3MetadataExtractor].
 * 
 * Verifies that ReplayGain and ID3 metadata are correctly extracted and mapped
 * from Media3 [Format] objects, handling both modern and legacy tag variants.
 */
@UnstableApi
@RunWith(RobolectricTestRunner::class)
class Media3MetadataExtractorMappingTest {

    private lateinit var extractor: Media3MetadataExtractor
    private val context = ApplicationProvider.getApplicationContext<Context>()

    /**
     * Initializes the extractor before each test.
     */
    @Before
    fun setUp() {
        extractor = Media3MetadataExtractor(context)
    }

    /**
     * Verifies that modern 4-character ID3 tags (e.g., TIT2, TPE1) containing 
     * title, album, and ReplayGain values are mapped correctly.
     */
    @Test
    fun testMp3ReplayGainExtraction() {
        val metadata = Metadata(
            TextInformationFrame("TIT2", null, listOf("Tom Sawyer")),
            TextInformationFrame("TPE1", null, listOf("Rush")),
            TextInformationFrame("TALB", null, listOf("Moving Pictures")),
            TextInformationFrame("TXXX", "replaygain_track_gain", listOf("-9.03 dB")),
            TextInformationFrame("TXXX", "replaygain_album_gain", listOf("-7.45 dB")),
        )

        val result = testMapping(metadata)

        assertEquals("Tom Sawyer", result.title)
        assertEquals("Rush", result.artist)
        assertEquals("Moving Pictures", result.album)
        assertEquals(-9.03f, result.trackGain)
        assertEquals(-7.45f, result.albumGain)
    }

    /**
     * Verifies that artwork (APIC frames) is correctly extracted.
     */
    @Test
    fun testArtworkExtraction() {
        val fakeArtwork = byteArrayOf(0, 1, 2, 3)
        val metadata = Metadata(
            ApicFrame("image/jpeg", "Cover", 3, fakeArtwork)
        )

        val result = testMapping(metadata)

        org.junit.Assert.assertArrayEquals(fakeArtwork, result.artwork)
    }

    /**
     * Verifies that legacy 3-character ID3 tags (e.g., TT2, TP1) are correctly
     * identified and mapped to the domain model.
     */
    @Test
    fun testLegacy3CharacterId3Tags() {
        val metadata = Metadata(
            TextInformationFrame("TT2", null, listOf("Spirit of Radio")),
            TextInformationFrame("TP1", null, listOf("Rush")),
            TextInformationFrame("TAL", null, listOf("Permanent Waves")),
            TextInformationFrame("TXX", "replaygain_track_gain", listOf("-9.03 dB")),
            TextInformationFrame("TXX", "replaygain_album_gain", listOf("-7.45 dB"))
        )

        val result = testMapping(metadata)

        assertEquals("Spirit of Radio", result.title)
        assertEquals("Rush", result.artist)
        assertEquals("Permanent Waves", result.album)
        assertEquals(-9.03f, result.trackGain)
        assertEquals(-7.45f, result.albumGain)
    }

    /**
     * Verifies that if required metadata is missing, the extractor applies the
     * mandatory fallback value of "<not found>".
     */
    @Test
    fun testMetadataNotFoundFallback() {
        val metadata = Metadata() // Empty metadata

        val result = testMapping(metadata)

        assertEquals("<not found>", result.title)
        assertEquals("<not found>", result.artist)
        assertEquals("<not found>", result.album)
    }

    /**
     * Helper method to simulate track group metadata extraction.
     * @param metadata The [Metadata] frame structure to test.
     * @return The resulting [ExtractedMetadata] domain model.
     */
    private fun testMapping(metadata: Metadata): ExtractedMetadata {
        val format = Format.Builder()
            .setMetadata(metadata)
            .build()
        val trackGroup = TrackGroup(format)
        val trackGroupArray = TrackGroupArray(trackGroup)
        
        return extractor.mapTrackGroupsToMetadata(trackGroupArray)
    }
}

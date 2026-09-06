package com.festerhead.cygnusplayer.core

import com.festerhead.cygnusplayer.data.entities.ShuffleMode
import com.festerhead.cygnusplayer.data.entities.TrackEntity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.math.pow

/**
 * Unit tests for [ReplayGainController].
 *
 * Verifies that:
 * 1. Shuffle modes map to the correct ReplayGain normalization type (Logic-First requirement).
 * 2. Volume multipliers are calculated accurately from dB gains using the standard formula: 10^(gain / 20).
 * 3. Graceful fallback logic behaves correctly when album or track gains are missing.
 */
class ReplayGainControllerTest {

    private lateinit var controller: ReplayGainController

    /**
     * Initializes the [ReplayGainController] instance before each test.
     */
    @Before
    fun setUp() {
        controller = ReplayGainController()
    }

    /**
     * Verifies that TRACK_RANDOM requires TRACK_GAIN.
     */
    @Test
    fun getRequiredGainType_trackRandom_returnsTrackGain() {
        val result = controller.getRequiredGainType(ShuffleMode.TRACK_RANDOM)
        assertEquals(ReplayGainType.TRACK_GAIN, result)
    }

    /**
     * Verifies that SEQUENTIAL requires ALBUM_GAIN.
     */
    @Test
    fun getRequiredGainType_sequential_returnsAlbumGain() {
        val result = controller.getRequiredGainType(ShuffleMode.SEQUENTIAL)
        assertEquals(ReplayGainType.ALBUM_GAIN, result)
    }

    /**
     * Verifies that RANDOM_FOLDER_SEQUENTIAL requires ALBUM_GAIN.
     */
    @Test
    fun getRequiredGainType_randomFolderSequential_returnsAlbumGain() {
        val result = controller.getRequiredGainType(ShuffleMode.RANDOM_FOLDER_SEQUENTIAL)
        assertEquals(ReplayGainType.ALBUM_GAIN, result)
    }

    /**
     * Verifies that a gain of 0 dB results in a volume multiplier of 1.0.
     */
    @Test
    fun getVolumeMultiplier_zeroDb_returnsOne() {
        val track = createTrack(trackGain = 0f, albumGain = 0f)
        val multiplier = controller.getVolumeMultiplier(track, ReplayGainType.TRACK_GAIN)
        assertEquals(1.0f, multiplier, 0.0001f)
    }

    /**
     * Verifies that negative dB gain (e.g. -2.04 dB) results in an attenuated multiplier (~0.7907).
     */
    @Test
    fun getVolumeMultiplier_negativeGain_returnsAttenuatedMultiplier() {
        val track = createTrack(trackGain = -2.04f, albumGain = -2.04f)
        val expected = 10f.pow(-2.04f / 20f)
        val multiplier = controller.getVolumeMultiplier(track, ReplayGainType.TRACK_GAIN)
        assertEquals(expected, multiplier, 0.0001f)
    }

    /**
     * Verifies that positive dB gain (e.g. +6.0 dB) results in an amplified multiplier (~1.995).
     */
    @Test
    fun getVolumeMultiplier_positiveGain_returnsAmplifiedMultiplier() {
        val track = createTrack(trackGain = 6.0f, albumGain = 6.0f)
        val expected = 10f.pow(6.0f / 20f)
        val multiplier = controller.getVolumeMultiplier(track, ReplayGainType.TRACK_GAIN)
        assertEquals(expected, multiplier, 0.0001f)
    }

    /**
     * Verifies that ALBUM_GAIN falls back to trackGain when albumGain is null.
     */
    @Test
    fun getVolumeMultiplier_albumGainNull_fallsBackToTrackGain() {
        val track = createTrack(trackGain = -3.5f, albumGain = null)
        val expected = 10f.pow(-3.5f / 20f)
        val multiplier = controller.getVolumeMultiplier(track, ReplayGainType.ALBUM_GAIN)
        assertEquals(expected, multiplier, 0.0001f)
    }

    /**
     * Verifies that a track with no gain tags defaults to 1.0 multiplier (0 dB).
     */
    @Test
    fun getVolumeMultiplier_allGainsNull_defaultsToOne() {
        val track = createTrack(trackGain = null, albumGain = null)
        val trackMultiplier = controller.getVolumeMultiplier(track, ReplayGainType.TRACK_GAIN)
        val albumMultiplier = controller.getVolumeMultiplier(track, ReplayGainType.ALBUM_GAIN)
        assertEquals(1.0f, trackMultiplier, 0.0001f)
        assertEquals(1.0f, albumMultiplier, 0.0001f)
    }

    /**
     * Helper to create a dummy [TrackEntity] with specified gain values.
     *
     * @param trackGain Optional track gain in dB.
     * @param albumGain Optional album gain in dB.
     * @return A configured [TrackEntity].
     */
    private fun createTrack(trackGain: Float?, albumGain: Float?): TrackEntity {
        return TrackEntity(
            trackId = 1L,
            filePath = "music/album/track.mp3",
            folderPath = "music/album",
            title = "Test Track",
            artist = "Test Artist",
            album = "Test Album",
            trackGain = trackGain,
            albumGain = albumGain
        )
    }
}

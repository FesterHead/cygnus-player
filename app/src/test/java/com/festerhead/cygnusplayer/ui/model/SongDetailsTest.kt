package com.festerhead.cygnusplayer.ui.model

import com.festerhead.cygnusplayer.data.entities.TrackEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [SongDetails] model mapping, sanitization, and ReplayGain formatting.
 */
class SongDetailsTest {

    @Test
    fun testFromTrackWithNullTrack() {
        val details = SongDetails.fromTrack(null)

        assertEquals("<empty>", details.artistName)
        assertEquals("<empty>", details.trackTitle)
        assertEquals("<empty>", details.albumTitle)
        assertEquals("<empty>", details.date)
        assertEquals("<empty>", details.genre)
        assertEquals("<empty>", details.comment)
        assertEquals("<empty>", details.trackGain)
        assertEquals("<empty>", details.trackPeak)
        assertEquals("<empty>", details.albumGain)
        assertEquals("<empty>", details.albumPeak)
    }

    @Test
    fun testFromTrackWithPlaceholdersAndMissingTags() {
        val track = TrackEntity(
            filePath = "/storage/music/track1.mp3",
            folderPath = "/storage/music/",
            title = "<not found>",
            artist = "<not found>",
            album = "<not found>",
            date = null,
            genre = "",
            comment = "   ",
            trackGain = null,
            trackPeak = null,
            albumGain = null,
            albumPeak = null,
        )

        val details = SongDetails.fromTrack(track)

        assertEquals("<empty>", details.artistName)
        assertEquals("<empty>", details.trackTitle)
        assertEquals("<empty>", details.albumTitle)
        assertEquals("<empty>", details.date)
        assertEquals("<empty>", details.genre)
        assertEquals("<empty>", details.comment)
        assertEquals("<empty>", details.trackGain)
        assertEquals("<empty>", details.trackPeak)
        assertEquals("<empty>", details.albumGain)
        assertEquals("<empty>", details.albumPeak)
    }

    @Test
    fun testFromTrackWithFullDetailsMatchingScreenshots() {
        val track = TrackEntity(
            filePath = "/storage/music/Rush/Moving Pictures/Limelight.mp3",
            folderPath = "/storage/music/Rush/Moving Pictures/",
            title = "Limelight",
            artist = "Rush",
            album = "Moving Pictures",
            date = "1981",
            genre = "Rock",
            comment = "40th Anniversary Deluxe Edition",
            trackGain = -1.61f,
            trackPeak = 0.898102f,
            albumGain = -1.71f,
            albumPeak = 1.000000f,
        )

        val details = SongDetails.fromTrack(track)

        assertEquals("Rush", details.artistName)
        assertEquals("Limelight", details.trackTitle)
        assertEquals("Moving Pictures", details.albumTitle)
        assertEquals("1981", details.date)
        assertEquals("Rock", details.genre)
        assertEquals("40th Anniversary Deluxe Edition", details.comment)
        assertEquals("-1.61 dB", details.trackGain)
        assertEquals("0.898102", details.trackPeak)
        assertEquals("-1.71 dB", details.albumGain)
        assertEquals("1.000000", details.albumPeak)

        val displayList = details.toDisplayList()
        assertEquals(10, displayList.size)
        assertEquals("Artist Name" to "Rush", displayList[0])
        assertEquals("Track Title" to "Limelight", displayList[1])
        assertEquals("Album Title" to "Moving Pictures", displayList[2])
        assertEquals("Date" to "1981", displayList[3])
        assertEquals("Genre" to "Rock", displayList[4])
        assertEquals("Comment" to "40th Anniversary Deluxe Edition", displayList[5])
        assertEquals("Track Gain" to "-1.61 dB", displayList[6])
        assertEquals("Track Peak" to "0.898102", displayList[7])
        assertEquals("Album Gain" to "-1.71 dB", displayList[8])
        assertEquals("Album Peak" to "1.000000", displayList[9])
    }

    @Test
    fun testPositiveGainFormatting() {
        val track = TrackEntity(
            filePath = "/storage/music/track.mp3",
            folderPath = "/storage/music/",
            trackGain = 1.50f,
            albumGain = 0.00f,
        )

        val details = SongDetails.fromTrack(track)
        assertEquals("+1.50 dB", details.trackGain)
        assertEquals("0.00 dB", details.albumGain)
    }
}

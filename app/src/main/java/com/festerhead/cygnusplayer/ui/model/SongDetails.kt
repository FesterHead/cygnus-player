package com.festerhead.cygnusplayer.ui.model

import com.festerhead.cygnusplayer.data.entities.TrackEntity
import java.util.Locale

/**
 * Detailed metadata attributes for display in the Now Playing song details modal.
 *
 * If a tag is absent, empty, or marked as unavailable, it defaults to "<empty>".
 *
 * @property artistName The name of the artist, or "<empty>".
 * @property trackTitle The title of the song, or "<empty>".
 * @property albumTitle The title of the album, or "<empty>".
 * @property date The recording or release date/year, or "<empty>".
 * @property genre The musical genre, or "<empty>".
 * @property comment User or release comment, or "<empty>".
 * @property trackGain The ReplayGain track normalization value formatted with unit (e.g. "-1.61 dB"), or "<empty>".
 * @property trackPeak The track peak amplitude formatted to 6 decimal places (e.g. "0.898102"), or "<empty>".
 * @property albumGain The ReplayGain album normalization value formatted with unit (e.g. "-1.71 dB"), or "<empty>".
 * @property albumPeak The album peak amplitude formatted to 6 decimal places (e.g. "1.000000"), or "<empty>".
 */
data class SongDetails(
    val artistName: String = EMPTY_VALUE,
    val trackTitle: String = EMPTY_VALUE,
    val albumTitle: String = EMPTY_VALUE,
    val date: String = EMPTY_VALUE,
    val genre: String = EMPTY_VALUE,
    val comment: String = EMPTY_VALUE,
    val trackGain: String = EMPTY_VALUE,
    val trackPeak: String = EMPTY_VALUE,
    val albumGain: String = EMPTY_VALUE,
    val albumPeak: String = EMPTY_VALUE,
) {
    /**
     * Converts the song details into an ordered list of label-value pairs matching
     * the order specified in the song details specification.
     *
     * @return List of Pairs where first is the label Name and second is the Value.
     */
    fun toDisplayList(): List<Pair<String, String>> = listOf(
        "Artist Name" to artistName,
        "Track Title" to trackTitle,
        "Album Title" to albumTitle,
        "Date" to date,
        "Genre" to genre,
        "Comment" to comment,
        "Track Gain" to trackGain,
        "Track Peak" to trackPeak,
        "Album Gain" to albumGain,
        "Album Peak" to albumPeak,
    )

    companion object {
        const val EMPTY_VALUE = "<empty>"
        private const val NOT_FOUND = "<not found>"

        /**
         * Creates a [SongDetails] instance from a [TrackEntity], applying "<empty>"
         * fallbacks to missing, blank, or placeholder values.
         *
         * @param track The database [TrackEntity], or null if no track is playing.
         * @return Formatted [SongDetails] ready for presentation.
         */
        fun fromTrack(track: TrackEntity?): SongDetails {
            if (track == null) return SongDetails()

            return SongDetails(
                artistName = sanitizeText(track.artist),
                trackTitle = sanitizeText(track.title),
                albumTitle = sanitizeText(track.album),
                date = sanitizeText(track.date),
                genre = sanitizeText(track.genre),
                comment = sanitizeText(track.comment),
                trackGain = formatGain(track.trackGain),
                trackPeak = formatPeak(track.trackPeak),
                albumGain = formatGain(track.albumGain),
                albumPeak = formatPeak(track.albumPeak),
            )
        }

        /**
         * Sanitizes text strings by trimming whitespace and mapping null, blank,
         * or "<not found>" values to "<empty>".
         *
         * @param value The candidate string to check.
         * @return Cleaned string or "<empty>".
         */
        private fun sanitizeText(value: String?): String {
            if (value.isNullOrBlank() || value == NOT_FOUND) {
                return EMPTY_VALUE
            }
            return value.trim()
        }

        /**
         * Formats ReplayGain dB value into a string (e.g. "-1.61 dB" or "+1.50 dB").
         *
         * @param gain The gain in decibels, or null if absent.
         * @return Formatted decibel string or "<empty>".
         */
        private fun formatGain(gain: Float?): String {
            if (gain == null) return EMPTY_VALUE
            return if (gain > 0f) {
                String.format(Locale.US, "+%.2f dB", gain)
            } else {
                String.format(Locale.US, "%.2f dB", gain)
            }
        }

        /**
         * Formats ReplayGain peak amplitude value to 6 decimal places (e.g. "0.898102" or "1.000000").
         *
         * @param peak The peak linear amplitude, or null if absent.
         * @return Formatted 6-decimal string or "<empty>".
         */
        private fun formatPeak(peak: Float?): String {
            if (peak == null) return EMPTY_VALUE
            return String.format(Locale.US, "%.6f", peak)
        }
    }
}

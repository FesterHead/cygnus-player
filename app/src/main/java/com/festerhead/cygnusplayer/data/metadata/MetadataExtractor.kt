package com.festerhead.cygnusplayer.data.metadata

import java.io.File

/**
 * Result of metadata extraction from an audio file.
 *
 * @property title The track title, extracted from ID3 tags or defaulted to "<not found>".
 * @property artist The artist name, extracted from ID3 tags or defaulted to "<not found>".
 * @property album The album title, extracted from ID3 tags or defaulted to "<not found>".
 * @property trackGain The ReplayGain track normalization value in dB, or null if not present.
 * @property albumGain The ReplayGain album normalization value in dB, or null if not present.
 * @property artwork Raw byte array representing the album cover image, or null if absent.
 * @property date The recording or release date/year, or null if not present.
 * @property genre The musical genre, or null if not present.
 * @property comment User or album comment, or null if not present.
 * @property trackPeak The track peak amplitude, or null if not present.
 * @property albumPeak The album peak amplitude, or null if not present.
 */
data class ExtractedMetadata(
    val title: String = "<not found>",
    val artist: String = "<not found>",
    val album: String = "<not found>",
    val trackGain: Float? = null,
    val albumGain: Float? = null,
    val artwork: ByteArray? = null,
    val date: String? = null,
    val genre: String? = null,
    val comment: String? = null,
    val trackPeak: Float? = null,
    val albumPeak: Float? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExtractedMetadata

        if (title != other.title) return false
        if (artist != other.artist) return false
        if (album != other.album) return false
        if (trackGain != other.trackGain) return false
        if (albumGain != other.albumGain) return false
        if (date != other.date) return false
        if (genre != other.genre) return false
        if (comment != other.comment) return false
        if (trackPeak != other.trackPeak) return false
        if (albumPeak != other.albumPeak) return false
        if (artwork != null) {
            if (other.artwork == null) return false
            if (!artwork.contentEquals(other.artwork)) return false
        } else if (other.artwork != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = (31 * result) + artist.hashCode()
        result = (31 * result) + album.hashCode()
        result = (31 * result) + (trackGain?.hashCode() ?: 0)
        result = (31 * result) + (albumGain?.hashCode() ?: 0)
        result = (31 * result) + (date?.hashCode() ?: 0)
        result = (31 * result) + (genre?.hashCode() ?: 0)
        result = (31 * result) + (comment?.hashCode() ?: 0)
        result = (31 * result) + (trackPeak?.hashCode() ?: 0)
        result = (31 * result) + (albumPeak?.hashCode() ?: 0)
        result = (31 * result) + (artwork?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Interface for components that can extract metadata and ReplayGain tags from MP3 files.
 */
interface MetadataExtractor {
    /**
     * Extracts metadata from the physical MP3 file.
     * 
     * @param file The MP3 file to scan.
     * @return [ExtractedMetadata] containing the discovered tags.
     */
    suspend fun extract(file: File): ExtractedMetadata
}

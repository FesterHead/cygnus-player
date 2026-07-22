package com.festerhead.cygnusplayer.data.metadata

import java.io.File

/**
 * Result of metadata extraction from an audio file.
 */
data class ExtractedMetadata(
    val title: String = "<not found>",
    val artist: String = "<not found>",
    val album: String = "<not found>",
    val trackGain: Float? = null,
    val albumGain: Float? = null,
    val artwork: ByteArray? = null,
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

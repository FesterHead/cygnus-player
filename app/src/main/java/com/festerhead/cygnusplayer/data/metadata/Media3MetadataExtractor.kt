package com.festerhead.cygnusplayer.data.metadata

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.ApicFrame
import androidx.media3.extractor.metadata.id3.CommentFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.inspector.MetadataRetriever
import kotlinx.coroutines.guava.await
import java.io.File

/**
 * [MetadataExtractor] implementation using Media3's MetadataRetriever.
 * 
 * Specifically designed to handle ReplayGain tags in ID3 TXXX frames, comments, and standard ID3 metadata for MP3 files.
 */
@OptIn(UnstableApi::class)
class Media3MetadataExtractor(private val context: Context) : MetadataExtractor {

    override suspend fun extract(file: File): ExtractedMetadata {
        return extract(file.toUri())
    }

    /**
     * Extracts metadata from a content URI.
     */
    suspend fun extract(uri: Uri): ExtractedMetadata {
        val mediaItem = MediaItem.fromUri(uri)
        
        return try {
            MetadataRetriever.Builder(context, mediaItem).build().use { retriever ->
                val trackGroups = retriever.retrieveTrackGroups().await()
                mapTrackGroupsToMetadata(trackGroups)
            }
        } catch (_: Exception) {
            ExtractedMetadata()
        }
    }

    /**
     * Internal mapping logic to convert Media3 TrackGroupArray into our domain model.
     * Exposed for testing.
     */
    @UnstableApi
    fun mapTrackGroupsToMetadata(trackGroups: androidx.media3.exoplayer.source.TrackGroupArray): ExtractedMetadata {
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var trackGain: Float? = null
        var albumGain: Float? = null
        var artwork: ByteArray? = null
        var date: String? = null
        var genre: String? = null
        var comment: String? = null
        var trackPeak: Float? = null
        var albumPeak: Float? = null

        for (i in 0 until trackGroups.length) {
            val format = trackGroups[i].getFormat(0)
            
            format.metadata?.let { metadata ->
                title = title ?: getStandardField(metadata, "TITLE") ?: format.label
                artist = artist ?: getStandardField(metadata, "ARTIST")
                album = album ?: getStandardField(metadata, "ALBUM")
                date = date ?: getStandardField(metadata, "DATE")
                genre = genre ?: getStandardField(metadata, "GENRE")?.let { cleanGenre(it) }

                for (j in 0 until metadata.length()) {
                    when (val entry = metadata[j]) {
                        is TextInformationFrame -> {
                            if ((entry.id == "TXXX") || (entry.id == "TXX")) {
                                val desc = entry.description?.lowercase()?.trim() ?: ""
                                val value = entry.values.firstOrNull()
                                when (desc) {
                                    "replaygain_track_gain" -> trackGain = trackGain ?: parseGain(value)
                                    "replaygain_track_peak" -> trackPeak = trackPeak ?: parseGain(value)
                                    "replaygain_album_gain" -> albumGain = albumGain ?: parseGain(value)
                                    "replaygain_album_peak" -> albumPeak = albumPeak ?: parseGain(value)
                                }
                            }
                        }
                        is CommentFrame -> {
                            val desc = entry.description?.lowercase()?.trim() ?: ""
                            if (desc != "itunnorm" && desc != "itunmcu") {
                                val text = entry.text
                                if (!text.isNullOrBlank()) {
                                    comment = comment ?: text
                                }
                            }
                        }
                        is ApicFrame -> {
                            artwork = artwork ?: entry.pictureData
                        }
                    }
                }
            }
        }

        return ExtractedMetadata(
            title = title ?: "<not found>",
            artist = artist ?: "<not found>",
            album = album ?: "<not found>",
            trackGain = trackGain,
            albumGain = albumGain,
            artwork = artwork,
            date = date,
            genre = genre,
            comment = comment,
            trackPeak = trackPeak,
            albumPeak = albumPeak,
        )
    }

    /**
     * Attempts to find a standard metadata field (like Title or Artist) from various frame types.
     * Handles both modern 4-letter IDs (TIT2) and legacy 3-letter IDs (TT2).
     */
    private fun getStandardField(metadata: Metadata, fieldName: String): String? {
        for (i in 0 until metadata.length()) {
            val entry = metadata[i]
            if (entry is TextInformationFrame) {
                val id = entry.id.uppercase()
                
                // Direct match
                if (id == fieldName.uppercase()) return entry.values.firstOrNull()

                // Logic match
                val isMatch = when (fieldName.uppercase()) {
                    "TITLE" -> (id == "TIT2") || (id == "TT2")
                    "ARTIST" -> (id == "TPE1") || (id == "TP1")
                    "ALBUM" -> (id == "TALB") || (id == "TAL")
                    "DATE" -> (id == "TDRC") || (id == "TYER") || (id == "TYE") || (id == "TDRL") || (id == "TDAT") || (id == "TRDA")
                    "GENRE" -> (id == "TCON") || (id == "TCO")
                    else -> false
                }
                
                if (isMatch) return entry.values.firstOrNull()
            }
        }
        return null
    }

    /**
     * Cleans legacy genre formatting (such as "(17)Rock" or "(17)") into plain genre text.
     *
     * @param rawGenre The unformatted genre string from the metadata frame.
     * @return The sanitized genre name.
     */
    private fun cleanGenre(rawGenre: String): String {
        val trimmed = rawGenre.trim()
        if (trimmed.startsWith("(") && trimmed.contains(")")) {
            val afterParen = trimmed.substringAfter(")").trim()
            if (afterParen.isNotEmpty()) {
                return afterParen
            }
        }
        return trimmed
    }

    /**
     * Parses gain strings like "-8.45 dB" or "-10.56dB" into a Float.
     */
    private fun parseGain(gainString: String?): Float? {
        if (gainString.isNullOrBlank()) return null
        return gainString.replace("dB", "", ignoreCase = true).trim().toFloatOrNull()
    }
}


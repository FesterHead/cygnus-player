package com.festerhead.cygnusplayer.data.parser

import java.io.InputStream

/**
 * Result of parsing a single entry in an M3U file.
 * 
 * @property relativePath The relative path to the audio file as found in the M3U.
 */
data class M3uEntry(
    val relativePath: String
)

/**
 * High-performance parser for M3U and M3U8 playlists.
 * 
 * Specifically engineered to handle massive, duplicate-heavy playlists while 
 * maintaining an O(1) memory footprint during processing.
 */
class M3uParser {

    companion object {
        private const val UTF8_BOM = "\uFEFF"
    }

    /**
     * Parses an M3U/M3U8 input stream and returns a [Sequence] of relative file paths.
     * 
     * This implementation:
     * 1. Strips UTF-8 Byte Order Marks (BOM) often present in Windows-generated files.
     * 2. Ignores all #EXT extended tags, favoring the physical file as the metadata source.
     * 3. Sanitizes Windows backslashes (\) to Android forward slashes (/).
     * 4. Filters out absolute paths.
     * 
     * @param inputStream The stream of the playlist file to parse.
     * @return A [Sequence] of [M3uEntry] for streaming into the database.
     */
    fun parse(inputStream: InputStream): Sequence<M3uEntry> = sequence {
        inputStream.bufferedReader().use { reader ->
            reader.lineSequence()
                .mapIndexed { index, line ->
                    // Strip all leading BOMs from the first line if present
                    if (index == 0) {
                        var sanitized = line
                        while (sanitized.startsWith(UTF8_BOM)) {
                            sanitized = sanitized.removePrefix(UTF8_BOM)
                        }
                        sanitized
                    } else {
                        line
                    }
                }
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .mapNotNull { rawPath ->
                    val sanitizedPath = rawPath.replace("\\", "/")

                    // We only support relative paths. 
                    // If the path is absolute (starts with / or has a :), we ignore it.
                    if (sanitizedPath.startsWith("/") || sanitizedPath.contains(":")) {
                        null
                    } else {
                        M3uEntry(sanitizedPath)
                    }
                }
                .forEach { yield(it) }
        }
    }
}

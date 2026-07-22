package com.festerhead.cygnusplayer.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

/**
 * Resolves relative file paths from M3U playlists to MediaStore content URIs.
 * 
 * This is essential for Scoped Storage compatibility on modern Android devices, 
 * as it avoids direct java.io.File access which is restricted for media files.
 */
class MediaStoreResolver(private val context: Context) {

    /**
     * Finds the content URI for a file given its relative path from the music root.
     * 
     * @param relativePathFromVolumeRoot The relative path starting from the storage root 
     *                                   (e.g., "Music/Rush/2112/01.mp3").
     * @return The MediaStore content URI if found, or null otherwise.
     */
    fun resolvePathToUri(relativePathFromVolumeRoot: String): Uri? {
        val file = File(relativePathFromVolumeRoot)
        val displayName = file.name
        // MediaStore.Audio.Media.RELATIVE_PATH expects a trailing slash
        val relativePath = file.parent?.let { if (it.endsWith("/")) it else "$it/" } ?: ""

        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} = ? AND ${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(relativePath, displayName)

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
            }
        }
        
        // Fallback: search for just the display name if relative path fails (less precise)
        return null
    }
}

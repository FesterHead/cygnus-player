package com.festerhead.cygnusplayer.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.festerhead.cygnusplayer.core.ShuffleEngine
import com.festerhead.cygnusplayer.data.daos.PlaylistStateDao
import com.festerhead.cygnusplayer.data.daos.QueueDao
import com.festerhead.cygnusplayer.data.daos.TrackDao
import com.festerhead.cygnusplayer.data.entities.PlaylistStateEntity
import com.festerhead.cygnusplayer.data.entities.QueueEntity
import com.festerhead.cygnusplayer.data.entities.ShuffleMode
import com.festerhead.cygnusplayer.data.entities.TrackEntity
import com.festerhead.cygnusplayer.data.metadata.Media3MetadataExtractor
import com.festerhead.cygnusplayer.data.parser.M3uParser
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Orchestrates playlist data management, combining M3U parsing, metadata extraction,
 * and database persistence using Scoped Storage-compliant APIs.
 */
class PlaylistRepository(
    private val context: Context,
    private val database: CygnusDatabase,
    private val m3uParser: M3uParser,
    val metadataExtractor: Media3MetadataExtractor,
    private val trackDao: TrackDao,
    private val queueDao: QueueDao,
    private val playlistStateDao: PlaylistStateDao,
    private val shuffleEngine: ShuffleEngine,
) {
    private val loadMutex = Mutex()
    val mediaStoreResolver = MediaStoreResolver(context)

    /**
     * Ensures a playlist's tracks are loaded and a playback sequence is generated.
     */
    suspend fun loadPlaylist(path: String): PlaylistStateEntity? = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            android.util.Log.d("CygnusRepo", "Loading playlist: $path")
            val state = playlistStateDao.getStateForPlaylist(path) ?: return@withLock null

            if ((state.mapping != null) && state.mapping.isNotEmpty()) {
                android.util.Log.d("CygnusRepo", "Playlist already cached: $path")
                return@withLock state
            }

            database.withTransaction {
                queueDao.clearQueueForPlaylist(path)

                // 1. Determine if path is a URI (Scoped Storage) or a physical file path (Emulator)
                val m3uUri = if (path.startsWith("content://") || path.startsWith("file://")) {
                    path.toUri()
                } else {
                    Uri.fromFile(File(path))
                }

                val inputStream = try {
                    context.contentResolver.openInputStream(m3uUri)
                } catch (_: Exception) {
                    null
                } ?: return@withTransaction

                val entries = m3uParser.parse(inputStream).toList()
                if (entries.isEmpty()) {
                    val emptyState = state.copy(mapping = longArrayOf(), lastOpened = System.currentTimeMillis())
                    playlistStateDao.saveState(emptyState)
                    return@withTransaction
                }

                val uniquePaths = entries.asSequence().map { it.relativePath }.distinct().toList()

                // Resolve existing tracks in batches of 999 (SQLite limit)
                val existingTracksMap = mutableMapOf<String, Long>()
                uniquePaths.chunked(999).forEach { chunk ->
                    val resolved = trackDao.getTracksByPaths(chunk)
                    resolved.forEach { existingTracksMap[it.filePath] = it.trackId }
                }

                // Identify missing tracks and batch insert placeholder entities
                val missingPaths = uniquePaths.filter { !existingTracksMap.containsKey(it) }
                if (missingPaths.isNotEmpty()) {
                    val placeholders = missingPaths.map { relPath ->
                        val cleanFileName = relPath.substringAfterLast("/").substringAfterLast("\\").substringBeforeLast(".")
                        TrackEntity(
                            filePath = relPath,
                            contentUri = null,
                            folderPath = relPath.substringBeforeLast("/", "").let { if (it.isEmpty()) "" else "$it/" },
                            artist = "<not found>",
                            album = "<not found>",
                            title = cleanFileName,
                            trackGain = null,
                            albumGain = null,
                        )
                    }
                    trackDao.insertAll(placeholders)

                    // Complete the map by resolving newly inserted tracks
                    missingPaths.chunked(999).forEach { chunk ->
                        val resolved = trackDao.getTracksByPaths(chunk)
                        resolved.forEach { existingTracksMap[it.filePath] = it.trackId }
                    }
                }

                // Build queue entries and batch insert
                val queueEntries = entries.map { entry ->
                    val trackId = existingTracksMap[entry.relativePath] ?: 0L
                    QueueEntity(
                        trackId = trackId,
                        filePath = entry.relativePath,
                        folderPath = entry.relativePath.substringBeforeLast("/", "").let { if (it.isEmpty()) "" else "$it/" },
                        m3uPath = path,
                    )
                }
                queueDao.insertAll(queueEntries)

                // Generate mapping
                val rawQueueIds = queueDao.getQueueIdsForPlaylist(path).toLongArray()
                val folderMap = if (state.shuffleMode == ShuffleMode.RANDOM_FOLDER_SEQUENTIAL) {
                    val uniqueFolders = queueDao.getAllUniqueFoldersForPlaylist(path)
                    uniqueFolders.associateWith { folder ->
                        queueDao.getTracksByFolderForPlaylist(folder, path).map { it.queueId }.toLongArray()
                    }
                } else {
                    emptyMap()
                }

                val mapping = shuffleEngine.generateMapping(
                    rawQueueIds,
                    state.shuffleMode,
                    folderMap = folderMap,
                )

                val updatedState = state.copy(
                    mapping = mapping,
                    lastOpened = System.currentTimeMillis(),
                )

                playlistStateDao.saveState(updatedState)
            }
            // Return fresh state from DB after transaction
            val finalState = playlistStateDao.getStateForPlaylist(path)
            android.util.Log.d("CygnusRepo", "Playlist loaded: $path with ${finalState?.mapping?.size ?: 0} items")
            finalState
        }
    }

    /**
     * Extracts a relative path from a URI to help MediaStore resolution.
     * Exposed for lazy path resolution.
     */
    fun resolveRelativePathFromUri(uri: Uri): String {
        return when (uri.scheme) {
            "file" -> {
                val path = uri.path ?: ""
                val index = path.indexOf("/Music/", ignoreCase = true)
                if (index != -1) {
                    val rel = path.substring(index + 1)
                    val parent = File(rel).parent ?: ""
                    if (parent.isEmpty()) "" else if (parent.endsWith("/")) parent else "$parent/"
                } else ""
            }
            "content" -> {
                // Example content URI from SAF: 
                // content://com.android.externalstorage.documents/tree/primary%3AMusic%2FFester's%20Favs/document/primary%3AMusic%2FFester's%20Favs%2Fandroid-testing.m3u8
                val decodedPath = Uri.decode(uri.path ?: "")
                val docId = decodedPath.substringAfterLast("/document/")
                if (docId.contains("Music", ignoreCase = true)) {
                    val relPath = docId.substringAfter(":").substringBeforeLast("/")
                    if (relPath.isEmpty()) "" else if (relPath.endsWith("/")) relPath else "$relPath/"
                } else {
                    // Fallback to library_root if M3U doesn't reside directly inside a known music structure,
                    // or if it's placed directly inside a subdirectory under the granted SAF tree.
                    val treeId = decodedPath.substringAfter("/tree/").substringBefore("/document/")
                    val relPath = treeId.substringAfter(":").substringBeforeLast("/")
                    if (relPath.isNotEmpty()) {
                        if (relPath.endsWith("/")) relPath else "$relPath/"
                    } else ""
                }
            }
            else -> ""
        }
    }

    /**
     * Clears cached tracks and queue entries for a playlist, ensuring re-adding resolves fresh.
     */
    suspend fun deletePlaylistData(m3uPath: String) = withContext(Dispatchers.IO) {
        queueDao.clearQueueForPlaylist(m3uPath)
    }
}

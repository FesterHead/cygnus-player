package com.festerhead.cygnusplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import android.os.Bundle
import android.widget.Toast
import androidx.media3.common.AudioAttributes
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.LibraryResult
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.festerhead.cygnusplayer.CygnusApplication
import com.festerhead.cygnusplayer.R
import com.festerhead.cygnusplayer.core.QueueController
import com.festerhead.cygnusplayer.core.ReplayGainController
import com.festerhead.cygnusplayer.core.ReplayGainType
import androidx.core.net.toUri
import com.festerhead.cygnusplayer.data.entities.ShuffleMode
import com.festerhead.cygnusplayer.ui.widget.CygnusWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * The core playback service for Cygnus Player.
 *
 * This service manages the [ExoPlayer] instance and hosts the [MediaLibrarySession],
 * providing a bridge for background audio playback, system-wide media control 
 * integration, and Android Auto support.
 */
@OptIn(UnstableApi::class)
class CygnusPlaybackService : MediaLibraryService() {

    private var player: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null
    private var queueController: QueueController? = null
    private var replayGainController: ReplayGainController? = null
    private var playlistRepository: com.festerhead.cygnusplayer.data.PlaylistRepository? = null
    
    private var currentShuffleMode: ShuffleMode = ShuffleMode.SEQUENTIAL
    private var currentPlaylistPath: String? = null
    private var isInitializing = false
    private var pausedByNoisy = false

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            // Auto-resume if we were paused by a disconnect and a high-quality sink is now available
            if (pausedByNoisy && (player?.isPlaying == false)) {
                val hasHeadphonesOrBluetooth = addedDevices?.any {
                    (it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) ||
                            (it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) ||
                            (it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET)
                } ?: false

                if (hasHeadphonesOrBluetooth) {
                    Log.i("CygnusPlayback", "Audio device reconnected. Resuming playback.")
                    player?.play()
                    pausedByNoisy = false
                }
            }
        }
    }
    
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onCreate() {
        super.onCreate()

        val app = application as CygnusApplication
        queueController = app.queueController
        replayGainController = app.replayGainController
        playlistRepository = app.playlistRepository

        val extractorsFactory = ExtractorsFactory { arrayOf(Mp3Extractor()) }

        // Initialize ExoPlayer with gapless playback optimizations
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this, extractorsFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        player!!.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updateWidgetState()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        queueController?.moveNext()
                        updateSlidingWindow()
                        persistPlaybackState()
                    }
                    applyReplayGain()
                    updateWidgetState()
                }

                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    updateWidgetState()
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("CygnusPlayback", "Playback error occurred: ${error.message}", error)
                    // Display details in a Toast to ensure it is visible even without a connected debugger
                    Toast.makeText(
                        this@CygnusPlaybackService,
                        "Playback Error: ${error.localizedMessage ?: error.errorCodeName}",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
        )

        // Initialize MediaLibrarySession with Callback for Playback Resumption and AA Browsing
        mediaLibrarySession = MediaLibrarySession.Builder(this, player!!, MediaLibraryCallback())
            .build()
        
        // Register Noisy Receiver
        val noisyReceiver = BecomingNoisyReceiver(player!!) {
            pausedByNoisy = true
        }
        registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), RECEIVER_NOT_EXPORTED)

        // Register Audio Device Callback for auto-resume
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)

        // Register Widget Toggle Receiver
        registerReceiver(
            WidgetToggleReceiver(),
            IntentFilter("com.festerhead.cygnusplayer.TOGGLE_PLAY_PAUSE"),
            RECEIVER_NOT_EXPORTED,
        )
    }

    private fun updateWidgetState() {
        val app = application as CygnusApplication
        val controller = player ?: return
        val currentMediaItem = controller.currentMediaItem ?: return
        val metadata = currentMediaItem.mediaMetadata
        val positionStr = app.queueController.getPositionString()
        val isPlaying = controller.isPlaying

        serviceScope.launch(Dispatchers.IO) {
            val context = this@CygnusPlaybackService
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(CygnusWidget::class.java)

            // Cache artwork to file for the widget process
            metadata.artworkData?.let { bytes ->
                try {
                    val file = File(cacheDir, "current_artwork.png")
                    FileOutputStream(file).use { it.write(bytes) }
                } catch (e: Exception) {
                    Log.e("CygnusPlayback", "Failed to cache artwork for widget", e)
                }
            }

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        set(stringPreferencesKey("title"), metadata.title?.toString() ?: "Unknown")
                        set(stringPreferencesKey("artist"), metadata.artist?.toString() ?: "Unknown")
                        set(stringPreferencesKey("album"), metadata.albumTitle?.toString() ?: "Unknown")
                        set(stringPreferencesKey("position"), positionStr)
                        set(booleanPreferencesKey("is_playing"), isPlaying)
                    }
                }
                CygnusWidget().update(context, glanceId)
            }
        }
    }

    inner class WidgetToggleReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.festerhead.cygnusplayer.TOGGLE_PLAY_PAUSE") {
                player?.let {
                    if (it.isPlaying) it.pause() else it.play()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "cygnus_channel"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, "Playback", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)

        startForeground(
            1,
            Notification.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Cygnus Player")
                .setContentText("Playback started")
                .build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
            
        intent?.getStringExtra(EXTRA_PLAYLIST_PATH)?.let {
            startPlaylist(it)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun handlePlaybackResumption(): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
        serviceScope.launch {
            try {
                val app = application as CygnusApplication
                // Find the most recently opened playlist
                val lastState = app.database.playlistStateDao().getAllStates().firstOrNull()
                if (lastState != null) {
                    val updatedState = playlistRepository?.loadPlaylist(lastState.m3uPath)
                    if (updatedState != null) {
                        currentPlaylistPath = updatedState.m3uPath
                        currentShuffleMode = updatedState.shuffleMode
                        
                        queueController?.initialize(updatedState.mapping!!, updatedState.lastQueueId)
                        val window = queueController?.getWindowData()
                        
                        if (window?.current != null) {
                            val mediaItems = listOfNotNull(window.prev, window.current, window.next).map { createMediaItem(it) }
                            val startIndex = if (window.prev != null) 1 else 0
                            
                            future.set(
                                MediaSession.MediaItemsWithStartPosition(
                                    ImmutableList.copyOf(mediaItems),
                                    startIndex,
                                    updatedState.lastPositionMs,
                                ),
                            )
                            applyReplayGain()
                            return@launch
                        }
                    }
                }
                future.setException(IllegalStateException("No playlist state found to resume"))
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    private fun startPlaylist(path: String) {
        if ((currentPlaylistPath == path) && isInitializing) return
        
        // Persist the current playlist's position before switching to a new one
        persistPlaybackState()

        currentPlaylistPath = path
        isInitializing = true
        
        serviceScope.launch {
            try {
                val updatedState = playlistRepository?.loadPlaylist(path)
                if (updatedState != null) {
                    currentShuffleMode = updatedState.shuffleMode
                    queueController?.initialize(updatedState.mapping!!, updatedState.lastQueueId)
                    initializeSlidingWindow(updatedState.lastPositionMs)
                    updateWidgetState()
                }
            } finally {
                isInitializing = false
            }
        }
    }

    private fun persistPlaybackState() {
        val path = currentPlaylistPath ?: return
        val currentQueueId = queueController?.getCurrentQueueId() ?: return
        val currentPos = player?.currentPosition ?: 0L

        serviceScope.launch {
            val app = application as CygnusApplication
            val state = app.database.playlistStateDao().getStateForPlaylist(path)
            state?.let {
                app.database.playlistStateDao().saveState(
                    it.copy(
                        lastQueueId = currentQueueId,
                        lastPositionMs = currentPos,
                    ),
                )
            }
        }
    }

    private fun initializeSlidingWindow(startPositionMs: Long = 0L) {
        serviceScope.launch {
            val window = queueController?.getWindowData() ?: return@launch
            
            val mediaItems = listOfNotNull(window.prev, window.current, window.next).map { data ->
                createMediaItem(data)
            }
            
            player?.setMediaItems(mediaItems)
            
            // Seek to current (index 0 if no prev, index 1 if prev exists)
            val startIndex = if (window.prev != null) 1 else 0
            player?.seekTo(startIndex, startPositionMs)
            player?.prepare()
            player?.play()
        }
    }

    private fun updateSlidingWindow() {
        serviceScope.launch {
            val window = queueController?.getWindowData() ?: return@launch
            
            // The player just moved to 'current'. 
            // In the player's queue, index 0 was 'prev', 1 was 'current', 2 was 'next'.
            // Now player index is 2 (the new current).
            // We want to remove index 0 (old prev), then add 'new next' at index 2.
            
            player?.removeMediaItem(0)
            window.next?.let { 
                player?.addMediaItem(createMediaItem(it))
            }
        }
    }

    private fun applyReplayGain() {
        serviceScope.launch {
            val window = queueController?.getWindowData() ?: return@launch
            val currentTrack = window.current?.track ?: return@launch

            Log.i(
                "CygnusPlayback",
                "STARTED PLAYING: ${currentTrack.filePath} | Title: ${currentTrack.title} | Artist: ${currentTrack.artist} | Album: ${currentTrack.album} | TrackGain: ${currentTrack.trackGain} | AlbumGain: ${currentTrack.albumGain}",
            )

            val gainType = replayGainController?.getRequiredGainType(currentShuffleMode) ?: ReplayGainType.TRACK_GAIN
            val multiplier = replayGainController?.getVolumeMultiplier(currentTrack, gainType) ?: 1f
            
            player?.volume = multiplier
        }
    }

    private fun createMediaItem(data: QueueController.TrackData): MediaItem {
        Log.d(
            "CygnusPlayback",
            "Preparing sliding window track: ${data.track.filePath} | Title: ${data.track.title} | Artist: ${data.track.artist} | Album: ${data.track.album} | TrackGain: ${data.track.trackGain} | AlbumGain: ${data.track.albumGain}",
        )

        val extras = Bundle().apply {
            putString(EXTRA_ACTIVE_PLAYLIST_PATH, currentPlaylistPath)
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(data.track.title)
            .setArtist(data.track.artist)
            .setAlbumTitle(data.track.album)
            .setArtworkData(null, null) // Artwork is always loaded lazily to save memory
            .setExtras(extras)
            .build()

        // 1. Determine if we need to resolve the playable URI (MediaStore/SAF)
        val cachedUri = data.track.contentUri
        val needsUriResolution = cachedUri == null
        val isPlaceholder = (data.track.title == data.track.filePath.substringAfterLast("/").substringAfterLast("\\").substringBeforeLast(".")) &&
                (data.track.artist == "<not found>")

        // 2. Resolve or use cached URI
        val playableUriString = if (cachedUri == null) {
            val app = application as CygnusApplication
            val path = currentPlaylistPath ?: ""
            val m3uUri = if (path.startsWith("content://") || path.startsWith("file://")) {
                path.toUri()
            } else {
                Uri.fromFile(File(path))
            }
            val parentRelativePath = app.playlistRepository.resolveRelativePathFromUri(m3uUri)
            val fullRelPath = if (parentRelativePath.isEmpty()) data.track.filePath else "$parentRelativePath${data.track.filePath}"
            
            var contentUri = app.playlistRepository.mediaStoreResolver.resolvePathToUri(fullRelPath)
            
            if ((contentUri == null) && parentRelativePath.isNotEmpty()) {
                val cleanRelPath = data.track.filePath.substringAfterLast("/")
                val alternativePath = if (parentRelativePath.endsWith("/")) "$parentRelativePath$cleanRelPath" else "$parentRelativePath/$cleanRelPath"
                contentUri = app.playlistRepository.mediaStoreResolver.resolvePathToUri(alternativePath)
            }
            
            if (contentUri == null) {
                val prefs = getSharedPreferences("cygnus_prefs", MODE_PRIVATE)
                val libraryRootStr = prefs.getString("library_root", null)
                if (libraryRootStr != null) {
                    val decodedRoot = Uri.decode(libraryRootStr)
                    val treeId = decodedRoot.substringAfter("/tree/").trim()
                    val cleanTrackRel = data.track.filePath.replace("\\", "/")
                    val documentId = "$treeId/$cleanTrackRel"
                    val queryEncodedTreeId = treeId.replace("/", "%2F").replace(":", "%3A").replace(" ", "%20").replace("'", "%27")
                    val queryEncodedDocId = documentId.replace("/", "%2F").replace(":", "%3A").replace(" ", "%20").replace("'", "%27")
                    val constructedUriString = "content://com.android.externalstorage.documents/tree/$queryEncodedTreeId/document/$queryEncodedDocId"
                    contentUri = constructedUriString.toUri()
                }
            }
            contentUri?.toString() ?: data.track.filePath
        } else {
            cachedUri
        }

        // 3. Trigger asynchronous lazy metadata/artwork extraction
        // We ALWAYS do this to ensure artwork is loaded, as artwork is not stored in the DB.
        val trackUri = playableUriString.toUri()
        serviceScope.launch(Dispatchers.IO) {
            try {
                val app = application as CygnusApplication
                val realMeta = app.playlistRepository.metadataExtractor.extract(trackUri)
                
                // Persist updates to DB if needed (New URI or placeholder tags)
                if (needsUriResolution || isPlaceholder) {
                    val updatedTrack = data.track.copy(
                        contentUri = playableUriString,
                        title = if (isPlaceholder && (realMeta.title != "<not found>")) realMeta.title else data.track.title,
                        artist = if (isPlaceholder) realMeta.artist else data.track.artist,
                        album = if (isPlaceholder) realMeta.album else data.track.album,
                        trackGain = if (isPlaceholder) realMeta.trackGain else data.track.trackGain,
                        albumGain = if (isPlaceholder) realMeta.albumGain else data.track.albumGain,
                    )
                    app.database.trackDao().update(updatedTrack)
                }
                
                // Push the artwork (and tags if updated) back into the active player
                serviceScope.launch(Dispatchers.Main) {
                    val p = player ?: return@launch
                    for (i in 0 until p.mediaItemCount) {
                        val item = p.getMediaItemAt(i)
                        if (item.mediaId == data.queueId.toString()) {
                            val updatedMetadata = item.mediaMetadata.buildUpon()
                                .setTitle(if (isPlaceholder && (realMeta.title != "<not found>")) realMeta.title else data.track.title)
                                .setArtist(if (isPlaceholder) realMeta.artist else data.track.artist)
                                .setAlbumTitle(if (isPlaceholder) realMeta.album else data.track.album)
                                .setArtworkData(realMeta.artwork, null)
                                .build()
                            
                            p.replaceMediaItem(i, item.buildUpon().setMediaMetadata(updatedMetadata).build())
                            
                            // Explicitly trigger widget update if this item is currently playing
                            if (i == p.currentMediaItemIndex) {
                                updateWidgetState()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CygnusPlayback", "Lazy metadata extraction failed for track: ${data.track.filePath}", e)
            }
        }

        return MediaItem.Builder()
            .setMediaId(data.queueId.toString())
            .setUri(playableUriString)
            .setMediaMetadata(metadata)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaLibrarySession

    inner class MediaLibraryCallback : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .add(SessionCommand.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT)
                .add(SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN)
                .build()
            return MediaSession.ConnectionResult.accept(
                sessionCommands,
                connectionResult.availablePlayerCommands,
            )
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId("RECENT_ROOT")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Recent Playlists")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
                        .build(),
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            if (parentId == "RECENT_ROOT") {
                val app = application as CygnusApplication
                val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                serviceScope.launch {
                    val history = app.database.playlistStateDao().getAllStates()
                    val items = history.map { state ->
                        val name = Uri.decode(state.m3uPath).substringAfterLast("/").substringAfterLast("\\")
                        MediaItem.Builder()
                            .setMediaId("PLAYLIST|$state.m3uPath")
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(name)
                                    .setIsBrowsable(false)
                                    .setIsPlayable(true)
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                                    .build(),
                            )
                            .build()
                    }
                    future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
                }
                return future
            }
            return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return handlePlaybackResumption()
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val firstItem = mediaItems.firstOrNull()
            if (firstItem?.mediaId?.startsWith("PLAYLIST|") == true) {
                val path = firstItem.mediaId.substringAfter("PLAYLIST|")
                startPlaylist(path)
                return Futures.immediateFuture(mutableListOf()) // Playback handled by startPlaylist
            }
            return super.onAddMediaItems(mediaSession, controller, mediaItems)
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)

        val p = player
        val s = mediaLibrarySession
        if ((s != null) && (p != null)) {
            p.release()
            s.release()
        }
        mediaLibrarySession = null
        player = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PLAYLIST_PATH = "extra_playlist_path"
        const val EXTRA_ACTIVE_PLAYLIST_PATH = "extra_active_playlist_path"
    }
}

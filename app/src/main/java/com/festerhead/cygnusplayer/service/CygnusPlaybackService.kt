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
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.LibraryResult
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.festerhead.cygnusplayer.CygnusApplication
import com.festerhead.cygnusplayer.R
import com.festerhead.cygnusplayer.core.QueueController
import com.festerhead.cygnusplayer.core.ReplayGainController
import com.festerhead.cygnusplayer.core.ReplayGainType
import androidx.core.content.edit
import androidx.core.net.toUri
import com.festerhead.cygnusplayer.data.entities.ShuffleMode
import com.festerhead.cygnusplayer.ui.widget.CygnusWidget
import com.festerhead.cygnusplayer.ui.widget.CygnusWidgetReceiver

import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
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

    private var player: Player? = null
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
        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this, extractorsFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .build()

        // Wrap ExoPlayer in a ForwardingPlayer that denies all navigation commands.
        // This ensures that Android Auto and other system controllers hide the
        // Next, Previous, and Seek buttons entirely to align with the "Immutable Journey" philosophy.
        player = object : ForwardingPlayer(exoPlayer) {
            override fun play() {
                pausedByNoisy = false
                if (mediaItemCount == 0) {
                    val prefs = getSharedPreferences("cygnus_prefs", MODE_PRIVATE)
                    val activePath = prefs.getString("active_playlist_path", null)
                    if (!activePath.isNullOrEmpty()) {
                        startPlaylist(activePath, autoPlay = true)
                        return
                    }
                }
                super.play()
            }

            override fun pause() {
                pausedByNoisy = false
                super.pause()
            }

            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .remove(COMMAND_SEEK_TO_NEXT)
                    .remove(COMMAND_SEEK_TO_PREVIOUS)
                    .remove(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .remove(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .remove(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .remove(COMMAND_SEEK_BACK)
                    .remove(COMMAND_SEEK_FORWARD)
                    .remove(COMMAND_GET_TIMELINE)
                    .remove(COMMAND_SEEK_TO_DEFAULT_POSITION)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    COMMAND_SEEK_TO_NEXT,
                    COMMAND_SEEK_TO_PREVIOUS,
                    COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                    COMMAND_SEEK_BACK,
                    COMMAND_SEEK_FORWARD,
                    COMMAND_GET_TIMELINE,
                    COMMAND_SEEK_TO_DEFAULT_POSITION,
                    -> false
                    else -> super.isCommandAvailable(command)
                }
            }
        }

        player!!.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (!isPlaying) {
                        persistPlaybackState()
                    }
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

        // Initialize MediaLibrarySession with a unique ID to break stale Android Auto caches.
        // We use a MediaLibraryCallback that restricts browsing to satisfy minimalist goals.
        mediaLibrarySession = MediaLibrarySession.Builder(this, player!!, MediaLibraryCallback())
            .setId("CygnusMinimalistSessionV2")
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
            IntentFilter(ACTION_TOGGLE_PLAY_PAUSE),
            RECEIVER_NOT_EXPORTED,
        )

        // Register Widget Request Update Receiver
        registerReceiver(
            WidgetRequestUpdateReceiver(),
            IntentFilter(CygnusWidgetReceiver.ACTION_REQUEST_WIDGET_UPDATE),
            RECEIVER_NOT_EXPORTED,
        )

        // Automatically restore active playlist state if previously configured
        restoreActivePlaylistState(autoPlay = false)
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

    /**
     * Resets the home screen widget state to default placeholder text and removes cached artwork.
     */
    private fun clearWidgetState() {
        serviceScope.launch(Dispatchers.IO) {
            val context = this@CygnusPlaybackService
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(CygnusWidget::class.java)

            // Remove cached artwork file
            try {
                val file = File(cacheDir, "current_artwork.png")
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e("CygnusPlayback", "Failed to delete artwork cache for widget", e)
            }

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        set(stringPreferencesKey("title"), "No track playing")
                        set(stringPreferencesKey("artist"), "Unknown Artist")
                        set(stringPreferencesKey("album"), "Unknown Album")
                        set(stringPreferencesKey("position"), "0/0")
                        set(booleanPreferencesKey("is_playing"), false)
                    }
                }
                CygnusWidget().update(context, glanceId)
            }
        }
    }

    /**
     * Stops active playback, clears player media items and sliding window queue,
     * removes the active playlist reference, resets widget state, and dismisses foreground notification.
     */
    fun stopPlaybackAndClear() {
        val p = player
        if (p != null) {
            p.stop()
            p.clearMediaItems()
        }
        currentPlaylistPath = null
        pausedByNoisy = false
        queueController?.clear()
        val app = application as? CygnusApplication
        app?.queueController?.clear()

        getSharedPreferences("cygnus_prefs", MODE_PRIVATE).edit {
            remove("active_playlist_path")
        }

        clearWidgetState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(1)
    }

    inner class WidgetToggleReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_TOGGLE_PLAY_PAUSE) {
                player?.let {
                    if (it.isPlaying) it.pause() else it.play()
                }
            }
        }
    }

    inner class WidgetRequestUpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == CygnusWidgetReceiver.ACTION_REQUEST_WIDGET_UPDATE) {
                updateWidgetState()
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
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )

        if (intent?.action == ACTION_TOGGLE_PLAY_PAUSE) {
            val p = player
            if (p != null) {
                if (p.isPlaying) p.pause() else p.play()
            }
        }

        if (intent?.action == ACTION_STOP_PLAYBACK) {
            val targetPath = intent.getStringExtra(EXTRA_PLAYLIST_PATH)
            if (targetPath == null || targetPath == currentPlaylistPath) {
                stopPlaybackAndClear()
            }
            return super.onStartCommand(intent, flags, startId)
        }

        intent?.getStringExtra(EXTRA_PLAYLIST_PATH)?.let {
            startPlaylist(it, autoPlay = true)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Attempts to restore the active playlist from persistent preferences and database.
     *
     * @param autoPlay Whether playback should immediately commence after restoring state.
     */
    private fun restoreActivePlaylistState(autoPlay: Boolean = false) {
        val prefs = getSharedPreferences("cygnus_prefs", MODE_PRIVATE)
        val activePath = prefs.getString("active_playlist_path", null)
        if (!activePath.isNullOrEmpty()) {
            startPlaylist(activePath, autoPlay = autoPlay)
        }
    }

    /**
     * Initializes and begins playback for the specified playlist path.
     *
     * @param path The M3U file path or URI string to load into the queue.
     * @param autoPlay Whether to begin playback immediately once prepared.
     */
    private fun startPlaylist(path: String, autoPlay: Boolean = true) {
        if ((currentPlaylistPath == path) && ((player?.mediaItemCount ?: 0) > 0)) {
            if (autoPlay && (player?.isPlaying == false)) {
                player?.play()
            }
            return
        }

        if ((currentPlaylistPath == path) && isInitializing) return

        // Persist the current playlist's position before switching to a new one
        persistPlaybackState()

        currentPlaylistPath = path
        getSharedPreferences("cygnus_prefs", MODE_PRIVATE).edit {
            putString("active_playlist_path", path)
        }
        isInitializing = true

        serviceScope.launch {
            try {
                val updatedState = playlistRepository?.loadPlaylist(path)
                if (updatedState != null) {
                    currentShuffleMode = updatedState.shuffleMode
                    queueController?.initialize(updatedState.mapping ?: longArrayOf(), updatedState.lastQueueId)
                    setupSlidingWindow(updatedState.lastPositionMs, autoPlay = autoPlay)
                    updateWidgetState()
                    val app = application as CygnusApplication
                    app.database.playlistStateDao().saveState(
                        updatedState.copy(lastOpened = System.currentTimeMillis()),
                    )
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

    /**
     * Sets up the 3-track sliding window in ExoPlayer and prepares playback.
     *
     * @param startPositionMs Playback offset in milliseconds to seek into the initial track.
     * @param autoPlay Whether to immediately call [Player.play] after preparation.
     */
    private suspend fun setupSlidingWindow(startPositionMs: Long = 0L, autoPlay: Boolean = true) {
        val window = queueController?.getWindowData() ?: return

        val mediaItems = listOfNotNull(window.prev, window.current, window.next).map { data ->
            createMediaItem(data)
        }

        val p = player ?: return
        p.setMediaItems(mediaItems)

        // Seek to current (index 0 if no prev, index 1 if prev exists)
        val startIndex = if (window.prev != null) 1 else 0
        p.seekTo(startIndex, startPositionMs)
        p.prepare()
        if (autoPlay) {
            p.play()
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

    internal class MediaLibraryCallback : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            // Define a strict "Allow-list" for session commands.
            // We allow getting the library root to satisfy AA health checks and tests.
            // We do NOT allow searching or getting children, which hides the browse tabs.
            val minimalistSessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(androidx.media3.session.SessionCommand.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT)
                .remove(androidx.media3.session.SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN)
                .remove(androidx.media3.session.SessionCommand.COMMAND_CODE_LIBRARY_GET_ITEM)
                .remove(androidx.media3.session.SessionCommand.COMMAND_CODE_LIBRARY_SEARCH)
                .build()

            // Define a strict "Allow-list" for player commands.
            // We ONLY allow Play/Pause, Stop, and Metadata retrieval.
            // By excluding GET_TIMELINE and SEEK_TO_NEXT/PREVIOUS, we signal to car head units
            // and system controllers that there is no queue to navigate, hiding those buttons.
            val minimalistPlayerCommands = Player.Commands.Builder()
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_STOP)
                .add(Player.COMMAND_GET_METADATA)
                .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
                .build()

            return MediaSession.ConnectionResult.accept(
                minimalistSessionCommands,
                minimalistPlayerCommands,
            )
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            // Return a valid but non-browsable root to satisfy Android Auto health checks.
            // Using a unique ID (CYGNUS_MINIMALIST_ROOT) helps flush any stale AA caches.
            val rootItem = MediaItem.Builder()
                .setMediaId("CYGNUS_MINIMALIST_ROOT")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(false)
                        .setIsPlayable(false)
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
            // Return empty for every request to ensure no suggestions are shown.
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params))
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // Disable automated playback resumption to prevent Android Auto from 
            // displaying "phantom" playlists from previous sessions or stale caches.
            return Futures.immediateFailedFuture(UnsupportedOperationException("Manual start required"))
        }

    }

    override fun onDestroy() {
        persistPlaybackState()
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
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.festerhead.cygnusplayer.TOGGLE_PLAY_PAUSE"
        const val ACTION_STOP_PLAYBACK = "com.festerhead.cygnusplayer.STOP_PLAYBACK"
    }
}

package com.festerhead.cygnusplayer.service

import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.ListenableFuture

/**
 * Handles media session callbacks for the Cygnus Player.
 *
 * This implementation bridges system-level media commands (Play, Pause, etc.)
 * directly to the underlying Player instance.
 * It also supports playback resumption via [onPlaybackResumption].
 */
@androidx.media3.common.util.UnstableApi
class MediaSessionCallback(
    private val onResumePlayback: () -> ListenableFuture<MediaSession.MediaItemsWithStartPosition>
) : MediaSession.Callback {

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        return MediaSession.ConnectionResult.accept(
            MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS,
            MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
        )
    }

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        isForPlayback: Boolean
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        return onResumePlayback()
    }
}



package com.festerhead.cygnusplayer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.media3.exoplayer.ExoPlayer

/**
 * Receiver for the [AudioManager.ACTION_AUDIO_BECOMING_NOISY] intent.
 *
 * Automatically pauses playback when audio output changes (e.g., wired headphones 
 * are unplugged or Bluetooth disconnects) to prevent audio from leaking through 
 * the device speakers.
 *
 * @param player The [ExoPlayer] instance to pause.
 * @param onNoisy Callback triggered when a noisy event occurs.
 */
class BecomingNoisyReceiver(
    private val player: ExoPlayer,
    private val onNoisy: () -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            player.pause()
            onNoisy()
        }
    }
}

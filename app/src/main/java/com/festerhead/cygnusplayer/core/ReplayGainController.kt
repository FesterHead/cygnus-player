package com.festerhead.cygnusplayer.core

import com.festerhead.cygnusplayer.data.entities.ShuffleMode
import com.festerhead.cygnusplayer.data.entities.TrackEntity
import kotlin.math.pow

/**
 * Supported ReplayGain normalization types.
 */
enum class ReplayGainType {
    /** Uses track-specific gain for maximum loudness consistency across randomized sequences. */
    TRACK_GAIN,

    /** Uses album-specific gain to preserve the artistic dynamics intended for sequential listening. */
    ALBUM_GAIN
}

/**
 * Controller to determine the correct ReplayGain strategy based on playback context.
 *
 * Logic-First Requirement:
 * - Use TRACK_GAIN for TRACK_RANDOM.
 * - Use ALBUM_GAIN for SEQUENTIAL and RANDOM_FOLDER_SEQUENTIAL.
 */
class ReplayGainController {

    /**
     * Determines the required gain type for the given shuffle mode.
     * 
     * @param mode The active [ShuffleMode].
     * @return The required [ReplayGainType].
     */
    fun getRequiredGainType(mode: ShuffleMode): ReplayGainType {
        return when (mode) {
            ShuffleMode.TRACK_RANDOM -> ReplayGainType.TRACK_GAIN
            ShuffleMode.SEQUENTIAL,
            ShuffleMode.RANDOM_FOLDER_SEQUENTIAL -> ReplayGainType.ALBUM_GAIN
        }
    }

    /**
     * Calculates the volume multiplier for a track based on the required gain type.
     * 
     * Uses the formula: multiplier = 10 ^ (gain / 20)
     * 
     * @param track The [TrackEntity] containing gain metadata.
     * @param type The required [ReplayGainType].
     * @return A volume multiplier (0.0 to 1.0 or higher).
     */
    fun getVolumeMultiplier(track: TrackEntity, type: ReplayGainType): Float {
        val gain = when (type) {
            ReplayGainType.TRACK_GAIN -> track.trackGain
            ReplayGainType.ALBUM_GAIN -> track.albumGain ?: track.trackGain
        } ?: 0f

        return 10f.pow(gain / 20f)
    }
}

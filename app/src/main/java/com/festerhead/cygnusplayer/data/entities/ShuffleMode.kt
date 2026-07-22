package com.festerhead.cygnusplayer.data.entities

/**
 * Supported playback randomness states.
 */
enum class ShuffleMode {
    /** Standard linear playback. */
    SEQUENTIAL,
    /** True shuffle across the entire unique sequence. */
    TRACK_RANDOM,
    /** Shuffles directory groups, playing tracks within a folder sequentially. */
    RANDOM_FOLDER_SEQUENTIAL
}

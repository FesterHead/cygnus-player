package com.festerhead.cygnusplayer.data

import androidx.room.TypeConverter
import com.festerhead.cygnusplayer.data.entities.ShuffleMode

/**
 * Room TypeConverters for custom data types and enums.
 */
class Converters {
    @TypeConverter
    fun fromShuffleMode(value: ShuffleMode): String {
        return value.name
    }

    @TypeConverter
    fun toShuffleMode(value: String): ShuffleMode {
        return ShuffleMode.valueOf(value)
    }

    @TypeConverter
    fun fromLongArray(value: LongArray?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toLongArray(value: String?): LongArray? {
        if (value.isNullOrBlank()) return null
        return value.split(",").map { it.toLong() }.toLongArray()
    }
}

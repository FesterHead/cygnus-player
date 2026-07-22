package com.festerhead.cygnusplayer.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a directory containing one or more tracks in the library.
 *
 * Used primarily for the `RANDOM_FOLDER_SEQUENTIAL` shuffle mode to track
 * playback history and ensure folder-level sequence integrity.
 *
 * @property folderId Unique auto-generated primary key.
 * @property folderPath The absolute path to the directory. Must be unique.
 * @property folderName The display name of the folder (usually the leaf directory name).
 */
@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["folder_path"], unique = true)
    ]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "folder_id")
    val folderId: Long = 0,

    @ColumnInfo(name = "folder_path")
    val folderPath: String,

    @ColumnInfo(name = "folder_name")
    val folderName: String
)

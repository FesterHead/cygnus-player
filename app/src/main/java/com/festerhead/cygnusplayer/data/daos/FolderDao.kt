package com.festerhead.cygnusplayer.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.festerhead.cygnusplayer.data.entities.FolderEntity

/**
 * Data Access Object for the folders table.
 * Supports directory-aware shuffle logic and folder history management.
 */
@Dao
interface FolderDao {
    /**
     * Inserts a new folder into the database.
     * @param folder The folder entity to insert.
     * @return The auto-generated row ID of the inserted folder.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity): Long

    /**
     * Retrieves a folder by its unique directory path.
     */
    @Query("SELECT * FROM folders WHERE folder_path = :path")
    suspend fun getFolderByPath(path: String): FolderEntity?

    /**
     * Retrieves all folders in the library.
     */
    @Query("SELECT * FROM folders")
    suspend fun getAllFolders(): List<FolderEntity>

    /**
     * Deletes all folders from the database.
     */
    @Query("DELETE FROM folders")
    suspend fun deleteAll()
}

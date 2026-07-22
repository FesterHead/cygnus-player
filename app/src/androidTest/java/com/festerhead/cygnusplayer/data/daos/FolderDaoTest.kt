package com.festerhead.cygnusplayer.data.daos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.festerhead.cygnusplayer.data.CygnusDatabase
import com.festerhead.cygnusplayer.data.entities.FolderEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumented tests for [FolderDao].
 * Verifies path persistence and unique constraint enforcement for directories using 
 * realistic Android paths.
 */
@RunWith(AndroidJUnit4::class)
class FolderDaoTest {
    private lateinit var folderDao: FolderDao
    private lateinit var db: CygnusDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, CygnusDatabase::class.java
        ).build()
        folderDao = db.folderDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    /**
     * Verifies that a folder can be inserted and retrieved by its directory path.
     */
    @Test
    fun writeFolderAndReadByPath() {
        runBlocking {
            val folderPath = "/storage/emulated/0/Music/Rush/Hemispheres"
            val folder = FolderEntity(
                folderPath = folderPath,
                folderName = "Hemispheres"
            )
            folderDao.insert(folder)
            val result = folderDao.getFolderByPath(folderPath)
            assertEquals("Hemispheres", result?.folderName)
        }
    }

    /**
     * Verifies that different directory paths are correctly persisted.
     */
    @Test
    fun testAndroidStyleFolderPathPersistence() {
        runBlocking {
            val path1 = "/storage/emulated/0/Music/Rush/A Farewell to Kings"
            val path2 = "/storage/emulated/0/Music/Rush/Permanent Waves"
            
            val f1 = FolderEntity(folderPath = path1, folderName = "A Farewell to Kings")
            val f2 = FolderEntity(folderPath = path2, folderName = "Permanent Waves")
            
            folderDao.insert(f1)
            folderDao.insert(f2)
            
            val r1 = folderDao.getFolderByPath(path1)
            val r2 = folderDao.getFolderByPath(path2)
            
            assertEquals("A Farewell to Kings", r1?.folderName)
            assertEquals("Permanent Waves", r2?.folderName)
        }
    }

    /**
     * Verifies that the unique constraint on folder_path prevents duplicate entries.
     * Note: [FolderDao.insert] uses REPLACE strategy.
     */
    @Test
    fun testUniquePathConstraintReplace() {
        runBlocking {
            val path = "/storage/emulated/0/Music/Rush/Signals"
            val folder1 = FolderEntity(folderPath = path, folderName = "Signals (Old)")
            val folder2 = FolderEntity(folderPath = path, folderName = "Signals")
            
            folderDao.insert(folder1)
            folderDao.insert(folder2)
            
            val allFolders = folderDao.getAllFolders()
            assertEquals(1, allFolders.size)
            assertEquals("Signals", allFolders[0].folderName)
        }
    }

    /**
     * Verifies the full lifecycle of folder records including deletion.
     */
    @Test
    fun testFolderLifecycle() {
        runBlocking {
            val path = "/storage/emulated/0/Music/Rush/Grace Under Pressure"
            val f = FolderEntity(folderPath = path, folderName = "Grace Under Pressure")
            
            folderDao.insert(f)
            assertEquals(1, folderDao.getAllFolders().size)
            
            folderDao.deleteAll()
            assertEquals(0, folderDao.getAllFolders().size)
        }
    }
}

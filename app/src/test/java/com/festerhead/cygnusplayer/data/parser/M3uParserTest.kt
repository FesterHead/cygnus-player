package com.festerhead.cygnusplayer.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream

/**
 * Unit tests for [M3uParser].
 * Verifies high-performance relative path resolution and BOM handling using 
 * real-world resource files with a Rush theme.
 */
class M3uParserTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val parser = M3uParser()

    /**
     * Verifies that the parser correctly handles Windows-style relative paths 
     * from a resource file.
     */
    @Test
    fun testWindowsStyleResourceFile() {
        val rootDir = tempFolder.root
        val m3uFile = copyResourceToFolder("playlists/windows_style.m3u8", rootDir)
        
        val results = parser.parse(FileInputStream(m3uFile)).toList()

        assertEquals(16, results.size)
        assertTrue(results[0].relativePath.endsWith("01 - Finding My Way.mp3"))
        assertTrue(results[15].relativePath.endsWith("08 - In the End.mp3"))
    }

    /**
     * Verifies that the parser correctly handles Unix-style relative paths 
     * from a resource file.
     */
    @Test
    fun testUnixStyleResourceFile() {
        val rootDir = tempFolder.root
        val m3uFile = copyResourceToFolder("playlists/unix_style.m3u8", rootDir)
        
        val results = parser.parse(FileInputStream(m3uFile)).toList()

        assertEquals(11, results.size)
        assertTrue(results[0].relativePath.endsWith("01 - Bastille Day.mp3"))
        assertTrue(results[10].relativePath.endsWith("06 - Something for Nothing.mp3"))
    }

    /**
     * Verifies that a file with a UTF-8 BOM is handled gracefully, ensuring 
     * the BOM is stripped before processing paths.
     */
    @Test
    fun testBomResourceFile() {
        val rootDir = tempFolder.root
        val m3uFile = copyResourceToFolder("playlists/bom_style.m3u8", rootDir)
        
        val results = parser.parse(FileInputStream(m3uFile)).toList()

        assertEquals(5, results.size)
        assertTrue(results[0].relativePath.endsWith("01 Finding My Way.mp3"))
        assertTrue(results[1].relativePath.endsWith("01 - Cygnus X-1 Book II; Hemispheres.mp3"))
    }

    /**
     * Verifies performance with a massive library (50,000 entries across 5,000 albums) 
     * to ensure file system access and stream processing remain efficient.
     */
    @Test
    fun testMassiveLibraryParsingPerformance() {
        val rootDir = tempFolder.root
        val m3uFile = copyResourceToFolder("playlists/massive_library.m3u8", rootDir)
        val trackCount = 50_000
        
        val startTime = System.currentTimeMillis()
        val results = parser.parse(FileInputStream(m3uFile))
        
        var processedCount = 0
        results.forEach { _ -> processedCount++ }
        
        val duration = System.currentTimeMillis() - startTime

        assertEquals(trackCount, processedCount)
        assertTrue("Streaming $trackCount tracks took too long: ${duration}ms", duration < 60000)
    }

    /**
     * Verifies performance with a massive playlist in fewer folders (50,000 entries across 100 folders).
     */
    @Test
    fun testMassiveFolderParsingPerformance() {
        val rootDir = tempFolder.root
        val m3uFile = copyResourceToFolder("playlists/massive_folders.m3u8", rootDir)
        val trackCount = 50_000

        val startTime = System.currentTimeMillis()
        val results = parser.parse(FileInputStream(m3uFile))
        
        var processedCount = 0
        results.forEach { _ -> processedCount++ }
        
        val duration = System.currentTimeMillis() - startTime

        assertEquals(trackCount, processedCount)
        assertTrue("Parsing $trackCount entries took too long: ${duration}ms", duration < 60000)
    }

    /**
     * Verifies that absolute paths (Windows and Unix) are correctly ignored,
     * maintaining the constraint of library portability.
     */
    @Test
    fun testAbsolutePathsAreIgnored() {
        val rootDir = tempFolder.root
        val m3uFile = File(rootDir, "absolute.m3u8")
        m3uFile.writeText(
            """
            /unix/absolute/path.mp3
            C:\windows\absolute\path.mp3
            relative/path.mp3
        """.trimIndent()
        )
        
        val results = parser.parse(FileInputStream(m3uFile)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0].relativePath.endsWith("relative/path.mp3"))
    }

    /**
     * Verifies that absolute paths are skipped.
     */
    @Test
    fun testMissingFilesAreSkipped() {
        val rootDir = tempFolder.root
        val m3uFile = File(rootDir, "missing.m3u8")
        m3uFile.writeText("""
            exists.mp3
            /absolute/missing.mp3
        """.trimIndent())
        
        val results = parser.parse(FileInputStream(m3uFile)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0].relativePath.endsWith("exists.mp3"))
    }

    /**
     * Verifies that duplicate tracks are preserved as distinct entities in the 
     * playback sequence, allowing the queue engine to treat them as unique.
     */
    @Test
    fun testDuplicatesArePreserved() {
        val rootDir = tempFolder.root
        val m3uFile = File(rootDir, "duplicates.m3u8")
        m3uFile.writeText("""
            song.mp3
            song.mp3
            song.mp3
        """.trimIndent())
        
        val results = parser.parse(FileInputStream(m3uFile)).toList()

        assertEquals(3, results.size)
        results.forEach { 
            assertTrue(it.relativePath.endsWith("song.mp3"))
        }
    }

    /**
     * Helper to load a test resource and write it to a physical file.
     */
    private fun copyResourceToFolder(resourcePath: String, targetFolder: File): File {
        val classLoader = javaClass.classLoader 
            ?: throw IllegalStateException("Could not get class loader")
        val resource = classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")
        
        val targetFile = File(targetFolder, resourcePath.substringAfterLast("/"))
        targetFile.outputStream().use { output ->
            resource.copyTo(output)
        }
        return targetFile
    }
}

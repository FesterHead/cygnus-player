package com.festerhead.cygnusplayer.data.metadata

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented test to verify metadata extraction from a real physical royalty-free MP3 file.
 */
@UnstableApi
@RunWith(AndroidJUnit4::class)
class MetadataExtractorFileTest {

    private lateinit var extractor: Media3MetadataExtractor
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        extractor = Media3MetadataExtractor(context)
    }

    /**
     * Verifies that the extractor can read standard ID3 tags from impact_moderato.mp3
     * and correctly returns null for missing ReplayGain tags.
     */
    @Test
    fun testRealFileExtractionWithoutReplayGain() = runBlocking {
        val testFile = stageResourceFile("impact_moderato.mp3")

        val result = extractor.extract(testFile)
        
        android.util.Log.d("MetadataTest", "Extraction result (no gain): $result")

        // Verify standard tags present in the file
        assertEquals("Impact Moderato", result.title)
        assertEquals("Kevin MacLeod", result.artist)
        org.junit.Assert.assertNull("Track gain should be null when tag is absent", result.trackGain)
        org.junit.Assert.assertNull("Album gain should be null when tag is absent", result.albumGain)
        assertNotNull(result)
    }

    /**
     * Verifies that the extractor reads both standard ID3 tags and ReplayGain values
     * (-2.04 dB track and album gain) from impact_moderato_replaygain.mp3.
     */
    @Test
    fun testRealFileExtractionWithReplayGain() = runBlocking {
        val testFile = stageResourceFile("impact_moderato_replaygain.mp3")

        val result = extractor.extract(testFile)
        
        android.util.Log.d("MetadataTest", "Extraction result (with gain): $result")

        // Verify standard tags present in the file
        assertEquals("Impact Moderato", result.title)
        assertEquals("Kevin MacLeod", result.artist)
        
        // Verify ReplayGain tags
        assertNotNull("Track gain must not be null", result.trackGain)
        assertNotNull("Album gain must not be null", result.albumGain)
        assertEquals(-2.04f, result.trackGain ?: 0f, 0.01f)
        assertEquals(-2.04f, result.albumGain ?: 0f, 0.01f)
    }

    /**
     * Helper to stage a resource file to the application's cache directory for testing.
     * 
     * @param resourceName The filename of the resource to copy.
     * @return The staged [File] in the device's cache directory.
     */
    private fun stageResourceFile(resourceName: String): File {
        val testFile = File(context.cacheDir, resourceName)
        val inputStream = javaClass.classLoader?.getResourceAsStream(resourceName)
            ?: throw IllegalStateException("Resource $resourceName not found")

        inputStream.use { input ->
            testFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return testFile
    }
}

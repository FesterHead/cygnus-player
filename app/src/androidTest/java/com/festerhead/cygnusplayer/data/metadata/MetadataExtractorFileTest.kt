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
     * Verifies that the extractor can read standard ID3 tags from impact_moderato.mp3.
     * Note: ReplayGain is usually not present in royalty-free downloads, so we 
     * verify its absence or focus on the successful parsing of the file itself.
     */
    @Test
    fun testRealFileExtraction() = runBlocking {
        // Stage the resource file to the device's cache directory
        val testFile = File(context.cacheDir, "impact_moderato.mp3")
        
        val inputStream = javaClass.classLoader?.getResourceAsStream("impact_moderato.mp3")
            ?: throw IllegalStateException("Resource impact_moderato.mp3 not found")

        inputStream.use { input ->
            testFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        android.util.Log.d("MetadataTest", "Staged file size: ${testFile.length()} bytes")

        val result = extractor.extract(testFile)
        
        android.util.Log.d("MetadataTest", "Extraction result: $result")

        // Verify standard tags present in the file
        assertEquals("Impact Moderato", result.title)
        assertEquals("Kevin MacLeod", result.artist)
        
        // Ensure the extractor didn't crash and returned a valid object
        assertNotNull(result)
    }
}

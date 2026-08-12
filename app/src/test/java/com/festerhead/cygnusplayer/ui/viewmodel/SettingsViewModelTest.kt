package com.festerhead.cygnusplayer.ui.viewmodel

import com.festerhead.cygnusplayer.ui.settings.SettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [SettingsViewModel] utility logic.
 */
class SettingsViewModelTest {

    @Test
    fun testFormatMusicRootFolder_nullAndBlank() {
        assertNull(SettingsViewModel.formatMusicRootFolder(null))
        assertNull(SettingsViewModel.formatMusicRootFolder(""))
        assertNull(SettingsViewModel.formatMusicRootFolder("   "))
    }

    @Test
    fun testFormatMusicRootFolder_safPrimaryTreeUri() {
        val uri = "content://com.android.externalstorage.documents/tree/primary%3AMusic"
        val formatted = SettingsViewModel.formatMusicRootFolder(uri)
        assertEquals("Internal Storage > Music", formatted)
    }

    @Test
    fun testFormatMusicRootFolder_safPrimarySubFolderTreeUri() {
        val uri = "content://com.android.externalstorage.documents/tree/primary%3AMusic%2FAudiobooks"
        val formatted = SettingsViewModel.formatMusicRootFolder(uri)
        assertEquals("Internal Storage > Music/Audiobooks", formatted)
    }

    @Test
    fun testFormatMusicRootFolder_safSdCardTreeUri() {
        val uri = "content://com.android.externalstorage.documents/tree/1234-5678%3AMusic"
        val formatted = SettingsViewModel.formatMusicRootFolder(uri)
        assertEquals("1234-5678:Music", formatted)
    }

    @Test
    fun testFormatMusicRootFolder_safRawDownloadTreeUri() {
        val uri = "content://com.android.providers.downloads.documents/tree/raw%3A%2Fstorage%2Femulated%2F0%2FDownload"
        val formatted = SettingsViewModel.formatMusicRootFolder(uri)
        assertEquals("/storage/emulated/0/Download", formatted)
    }

    @Test
    fun testFormatMusicRootFolder_fileUri() {
        val uri = "file:///storage/emulated/0/Music"
        val formatted = SettingsViewModel.formatMusicRootFolder(uri)
        assertEquals("/storage/emulated/0/Music", formatted)
    }
}

package com.festerhead.cygnusplayer.ui.viewmodel

import com.festerhead.cygnusplayer.PlaylistPickerViewModel
import com.festerhead.cygnusplayer.data.entities.PlaylistStateEntity
import com.festerhead.cygnusplayer.data.entities.ShuffleMode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [PlaylistPickerViewModel] utility functions and sorting logic.
 */
class PlaylistPickerViewModelTest {

    @Test
    fun testSortHistory_nullActivePath_preservesOrder() {
        val p1 = PlaylistStateEntity("path1.m3u", 0, ShuffleMode.SEQUENTIAL, 100L)
        val p2 = PlaylistStateEntity("path2.m3u", 0, ShuffleMode.SEQUENTIAL, 200L)
        val history = listOf(p2, p1)

        val sorted = PlaylistPickerViewModel.sortHistory(history, null)
        assertEquals(listOf(p2, p1), sorted)
    }

    @Test
    fun testSortHistory_activePathMovesToTop() {
        val p1 = PlaylistStateEntity("path1.m3u", 0, ShuffleMode.SEQUENTIAL, 300L)
        val p2 = PlaylistStateEntity("path2.m3u", 0, ShuffleMode.SEQUENTIAL, 200L)
        val p3 = PlaylistStateEntity("path3.m3u", 0, ShuffleMode.SEQUENTIAL, 100L)
        val history = listOf(p1, p2, p3)

        val sorted = PlaylistPickerViewModel.sortHistory(history, "path2.m3u")
        assertEquals(listOf(p2, p1, p3), sorted)
    }

    @Test
    fun testSortHistory_activePathAlreadyAtTop() {
        val p1 = PlaylistStateEntity("path1.m3u", 0, ShuffleMode.SEQUENTIAL, 300L)
        val p2 = PlaylistStateEntity("path2.m3u", 0, ShuffleMode.SEQUENTIAL, 200L)
        val history = listOf(p1, p2)

        val sorted = PlaylistPickerViewModel.sortHistory(history, "path1.m3u")
        assertEquals(listOf(p1, p2), sorted)
    }
}

package com.festerhead.cygnusplayer.ui.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Unit tests for [CygnusWidgetReceiver].
 */
@RunWith(RobolectricTestRunner::class)

class CygnusWidgetReceiverUnitTest {

    @Test
    fun testActionRequestWidgetUpdateConstant() {
        assertEquals(
            "com.festerhead.cygnusplayer.REQUEST_WIDGET_UPDATE",
            CygnusWidgetReceiver.ACTION_REQUEST_WIDGET_UPDATE
        )
    }

    @Test
    fun testGlanceAppWidgetProperty() {
        val receiver = CygnusWidgetReceiver()
        assertTrue(receiver.glanceAppWidget is CygnusWidget)
    }

    @Test
    fun testOnUpdate_broadcastsWidgetUpdateRequest() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val shadowApp = shadowOf(context)
        val receiver = CygnusWidgetReceiver()

        val appWidgetManager = AppWidgetManager.getInstance(context)
        receiver.onUpdate(context, appWidgetManager, intArrayOf(1))

        val broadcast = shadowApp.broadcastIntents.find {
            it.action == CygnusWidgetReceiver.ACTION_REQUEST_WIDGET_UPDATE
        }
        assertNotNull("Expected ACTION_REQUEST_WIDGET_UPDATE broadcast on update", broadcast)
        assertEquals(context.packageName, broadcast?.`package`)
    }

    @Test
    fun testOnEnabled_broadcastsWidgetUpdateRequest() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val shadowApp = shadowOf(context)
        val receiver = CygnusWidgetReceiver()

        receiver.onEnabled(context)

        val broadcast = shadowApp.broadcastIntents.find {
            it.action == CygnusWidgetReceiver.ACTION_REQUEST_WIDGET_UPDATE
        }
        assertNotNull("Expected ACTION_REQUEST_WIDGET_UPDATE broadcast on enabled", broadcast)
        assertEquals(context.packageName, broadcast?.`package`)
    }
}

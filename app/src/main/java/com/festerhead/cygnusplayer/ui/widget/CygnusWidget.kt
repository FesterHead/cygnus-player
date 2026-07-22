@file:Suppress("RestrictedApi")
package com.festerhead.cygnusplayer.ui.widget

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.festerhead.cygnusplayer.R
import com.festerhead.cygnusplayer.MainActivity
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import java.io.File

/**
 * High-performance home screen widget for Cygnus Player.
 * Uses Jetpack Glance for a minimalist, reactive UI.
 */
class CygnusWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent(context)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context) {
        val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
        val title = prefs[stringPreferencesKey("title")] ?: "No track playing"
        val artist = prefs[stringPreferencesKey("artist")] ?: "Unknown Artist"
        val album = prefs[stringPreferencesKey("album")] ?: "Unknown Album"
        val position = prefs[stringPreferencesKey("position")] ?: "0/0"
        val isPlaying = prefs[booleanPreferencesKey("is_playing")] ?: false

        // Load artwork from internal cache file
        val artworkFile = File(context.cacheDir, "current_artwork.png")
        val artworkBitmap = if (artworkFile.exists()) {
            BitmapFactory.decodeFile(artworkFile.absolutePath)
        } else {
            null
        }

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(R.color.monokai_background))
                .padding(8.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album Art
            Box(
                modifier = GlanceModifier
                    .size(64.dp)
                    .background(ColorProvider(R.color.monokai_background)),
                contentAlignment = Alignment.Center,
            ) {
                if (artworkBitmap != null) {
                    Image(
                        provider = ImageProvider(artworkBitmap),
                        contentDescription = "Album Art",
                        modifier = GlanceModifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Image(
                        provider = ImageProvider(R.drawable.ic_launcher_foreground),
                        contentDescription = "Placeholder",
                        modifier = GlanceModifier.size(40.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(R.color.monokai_orange)),
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            // Metadata
            Column(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        color = ColorProvider(R.color.monokai_orange),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    text = "$artist - $album",
                    style = TextStyle(
                        color = ColorProvider(R.color.monokai_purple),
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
                Text(
                    text = position,
                    style = TextStyle(
                        color = ColorProvider(R.color.monokai_blue),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Controls
            Box(
                modifier = GlanceModifier
                    .size(48.dp)
                    .clickable(actionRunCallback<TogglePlayPauseAction>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(
                        if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = GlanceModifier.size(32.dp),
                    colorFilter = ColorFilter.tint(ColorProvider(R.color.monokai_text))
                )
            }
        }
    }
}

/**
 * Action callback to toggle playback from the widget.
 */
class TogglePlayPauseAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = android.content.Intent("com.festerhead.cygnusplayer.TOGGLE_PLAY_PAUSE").apply {
            `package` = context.packageName
        }
        context.sendBroadcast(intent)
    }
}

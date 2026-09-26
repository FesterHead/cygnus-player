package com.festerhead.cygnusplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.festerhead.cygnusplayer.ui.viewmodel.NowPlayingViewModel
import com.festerhead.cygnusplayer.ui.theme.MonokaiBlue
import com.festerhead.cygnusplayer.ui.theme.MonokaiGreen
import com.festerhead.cygnusplayer.ui.theme.MonokaiOrange
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import android.graphics.BitmapFactory

import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.tooling.preview.Preview
import com.festerhead.cygnusplayer.ui.model.SongDetails
import com.festerhead.cygnusplayer.ui.theme.CygnusPlayerTheme
import com.festerhead.cygnusplayer.ui.viewmodel.NowPlayingUiState

/**
 * Minimalist "Now Playing" screen.
 * 
 * Provides high-contrast playback controls and displays the current track position 
 * within the sequence. Navigation back to the library is handled by a high-contrast
 * Monokai Blue button, alongside a Song Details action for viewing granular ID3 tags.
 *
 * @param viewModel The [NowPlayingViewModel] providing playback state and metadata.
 * @param onNavigateBack Callback invoked when navigating back to the playlist picker.
 */
@Composable
fun NowPlayingScreen(
    viewModel: NowPlayingViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    NowPlayingContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onTogglePlayPause = { viewModel.togglePlayPause() },
    )
}

/**
 * Stateless content composable for the Now Playing screen.
 *
 * @param uiState Current playback state and track metadata.
 * @param onNavigateBack Callback invoked when navigating back to playlists.
 * @param onTogglePlayPause Callback invoked to toggle active playback.
 */
@Composable
fun NowPlayingContent(
    uiState: NowPlayingUiState,
    onNavigateBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
) {
    var showDetailsDialog by remember { mutableStateOf(false) }

    if (showDetailsDialog) {
        SongDetailsDialog(
            songDetails = uiState.songDetails,
            onDismissRequest = { showDetailsDialog = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp),
    ) {
        // Navigation Back Button & Song Details Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Playlists",
                    tint = MonokaiBlue,
                )
            }

            IconButton(onClick = { showDetailsDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Song Details",
                    tint = MonokaiBlue,
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Album Artwork
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = remember(uiState.artwork) {
                uiState.artwork?.let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)
                }
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Album Art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "No Artwork",
                    modifier = Modifier.size(120.dp),
                    tint = MonokaiOrange.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Track Info
        Text(
            text = uiState.playlistName,
            color = MonokaiGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.basicMarquee().padding(bottom = 8.dp),
        )
        Text(
            text = uiState.trackTitle,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.basicMarquee(),
        )
        Text(
            text = uiState.albumName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 18.sp,
            modifier = Modifier.basicMarquee(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Play/Pause Toggle
        IconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier.size(80.dp),
        ) {
            Icon(
                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // STRICTLY VISUAL ONLY (READ-ONLY) Progress Bar
        val progress = if (uiState.durationMs > 0) {
            uiState.currentPositionMs.toFloat() / uiState.durationMs.toFloat()
        } else {
            0f
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .padding(vertical = 0.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Time indicators: Elapsed / Remaining
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(uiState.currentPositionMs),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Text(
                text = "-" + formatTime((uiState.durationMs - uiState.currentPositionMs).coerceAtLeast(0L)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Position & Mode Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = uiState.position,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                )
                val friendlyMode = when (uiState.shuffleMode) {
                    com.festerhead.cygnusplayer.data.entities.ShuffleMode.SEQUENTIAL -> "SEQUENTIAL"
                    com.festerhead.cygnusplayer.data.entities.ShuffleMode.RANDOM_FOLDER_SEQUENTIAL -> "ALBUM SHUFFLE"
                    com.festerhead.cygnusplayer.data.entities.ShuffleMode.TRACK_RANDOM -> "CHAOS (RANDOM)"
                }
                Text(
                    text = friendlyMode,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Formats time from milliseconds into mm:ss format.
 */
private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
}

@Preview(name = "Now Playing Screen - Playing", showBackground = true)
@Composable
private fun NowPlayingContentPreview() {
    CygnusPlayerTheme {
        NowPlayingContent(
            uiState = NowPlayingUiState(
                isPlaying = true,
                trackTitle = "Limelight",
                albumName = "Moving Pictures",
                playlistName = "Rush Discography",
                position = "4/12",
                currentPositionMs = 135000L,
                durationMs = 260000L,
                songDetails = SongDetails(
                    artistName = "Rush",
                    trackTitle = "Limelight",
                    albumTitle = "Moving Pictures",
                    date = "1981",
                    genre = "Rock",
                    comment = "40th Anniversary Deluxe Edition",
                    trackGain = "-1.61 dB",
                    trackPeak = "0.898102",
                    albumGain = "-1.71 dB",
                    albumPeak = "1.000000",
                ),
            ),
            onNavigateBack = {},
            onTogglePlayPause = {},
        )
    }
}

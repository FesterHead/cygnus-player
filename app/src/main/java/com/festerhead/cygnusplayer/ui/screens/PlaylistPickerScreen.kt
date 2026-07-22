package com.festerhead.cygnusplayer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.festerhead.cygnusplayer.PlaylistPickerViewModel
import com.festerhead.cygnusplayer.data.entities.PlaylistStateEntity
import com.festerhead.cygnusplayer.data.entities.ShuffleMode
import com.festerhead.cygnusplayer.ui.theme.MonokaiGreen

/**
 * Screen for users to select, manage, and view recent playlist files (.m3u/.m3u8).
 */
@Composable
fun PlaylistPickerScreen(
    viewModel: PlaylistPickerViewModel,
    onPlaylistSelected: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Launcher for picking the Music Root folder
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let { viewModel.onRootFolderSelected(context, it) }
    }

    // Launcher for picking individual playlists
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.onPlaylistSelected(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSettings(context)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (uiState.pendingPlaylistPath != null) {
        val decodedPending = try {
            android.net.Uri.decode(uiState.pendingPlaylistPath!!)
        } catch (_: Exception) {
            uiState.pendingPlaylistPath!!
        }
        val cleanPendingName = decodedPending.substringAfterLast("/").substringAfterLast("\\")
        MintingDialog(
            fileName = cleanPendingName,
            onModeSelected = { viewModel.mintPlaylist(it) },
            onDismiss = { viewModel.cancelMinting() }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    fileLauncher.launch(arrayOf("*/*"))
                },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Open Playlist")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            Text(
                text = "Cygnus Player",
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
            )

            // Library Root Warning/Setup
            if (uiState.libraryRootUri == null) {
                LibraryRootPrompt { folderLauncher.launch(null) }
            }

            Text(
                text = "M3U / M3U8 Playlists Only",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(uiState.history) { state ->
                    PlaylistHistoryItem(
                        state = state,
                        onClick = { viewModel.onPlaylistClicked(context, state.m3uPath) { onPlaylistSelected(state.m3uPath) } },
                        onDelete = { viewModel.deleteFromHistory(context, state) },
                        isActive = state.m3uPath == uiState.activePlaylistPath
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryRootPrompt(onSelectRoot: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Scoped Storage Access Required",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                "To resolve relative paths inside M3U files, Cygnus needs access to your Music root folder.",
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Button(
                onClick = onSelectRoot,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Select Root")
            }
        }
    }
}

/**
 * Individual item representing a loaded playlist in the history list.
 */
@Composable
fun PlaylistHistoryItem(
    state: PlaylistStateEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    isActive: Boolean = false
) {
    // Cleanly decode the playlist filename and show only the filename itself
    val decodedPath = try {
        android.net.Uri.decode(state.m3uPath)
    } catch (_: Exception) {
        state.m3uPath
    }
    val fileName = decodedPath.substringAfterLast("/").substringAfterLast("\\")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isActive) Icons.Filled.PlayArrow else Icons.AutoMirrored.Filled.List,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 16.dp),
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = fileName,
                color = MonokaiGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.basicMarquee(),
            )
            val (modeLabel, modeColor) = when (state.shuffleMode) {
                ShuffleMode.SEQUENTIAL -> "SEQUENTIAL" to MaterialTheme.colorScheme.primary
                ShuffleMode.RANDOM_FOLDER_SEQUENTIAL -> "ALBUM SHUFFLE" to MaterialTheme.colorScheme.secondary
                ShuffleMode.TRACK_RANDOM -> "CHAOS (RANDOM)" to MaterialTheme.colorScheme.tertiary
            }
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(modeColor)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = modeLabel,
                    color = MaterialTheme.colorScheme.background,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        IconButton(onClick = { onDelete() }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove from history",
                tint = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
fun MintingDialog(
    fileName: String,
    onModeSelected: (ShuffleMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                text = "Mint Playlist",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    text = "Select a shuffle mode for:",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                )
                Text(
                    text = fileName,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))

                MintingButton(
                    label = "SEQUENTIAL",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onModeSelected(ShuffleMode.SEQUENTIAL) },
                )
                MintingButton(
                    label = "ALBUM SHUFFLE",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { onModeSelected(ShuffleMode.RANDOM_FOLDER_SEQUENTIAL) },
                )
                MintingButton(
                    label = "CHAOS (RANDOM)",
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { onModeSelected(ShuffleMode.TRACK_RANDOM) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    )
}

@Composable
fun MintingButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = { onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.background,
            fontWeight = FontWeight.Bold,
        )
    }
}

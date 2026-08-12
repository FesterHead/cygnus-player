package com.festerhead.cygnusplayer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.festerhead.cygnusplayer.VersionInfo

/**
 * Minimalist Settings screen for project information and basic configuration.
 * 
 * @param onNavigateBack Callback to return to the previous screen.
 * @param viewModel The [SettingsViewModel] instance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Configuration Section
            SettingsSection(title = "Configuration") {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text = "Music Root Folder",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.musicRootFolder ?: "Not set",
                        fontSize = 14.sp,
                        color = if (uiState.musicRootFolder != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.basicMarquee()
                    )
                }
                Button(
                    onClick = { viewModel.resetRootFolder() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Reset Music Root Folder", color = MaterialTheme.colorScheme.background)
                }
                Text(
                    text = "Forces the app to prompt for Music folder access on next launch.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Diagnostics Section
            SettingsSection(title = "Diagnostics") {
                DiagnosticRow(label = "Total Tracks Mapped", value = uiState.totalTracks.toString())
                DiagnosticRow(label = "Saved Playlists", value = uiState.totalPlaylists.toString())
            }

            // About Section
            SettingsSection(title = "About") {
                Text(
                    text = "Version: ${VersionInfo.VERSION_NAME} (${VersionInfo.VERSION_CODE})",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { uriHandler.openUri("https://github.com/festerhead/cygnus-player") }) {
                    Text("GitHub Repository", color = MaterialTheme.colorScheme.secondary)
                }
                TextButton(onClick = { uriHandler.openUri("https://opensource.org/licenses/MIT") }) {
                    Text("MIT License", color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

/**
 * Container card for grouping settings items into titled sections.
 *
 * @param title Section header title text.
 * @param content Composable content rendered inside the section card.
 */
@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

/**
 * Diagnostic key-value row.
 *
 * @param label Description label for the metric.
 * @param value Formatted string value of the metric.
 */
@Composable
fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurface)
        Text(text = value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    com.festerhead.cygnusplayer.ui.theme.CygnusPlayerTheme {
        SettingsScreen(onNavigateBack = {})
    }
}



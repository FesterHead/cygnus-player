package com.festerhead.cygnusplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.festerhead.cygnusplayer.ui.model.SongDetails
import com.festerhead.cygnusplayer.ui.theme.CygnusPlayerTheme
import com.festerhead.cygnusplayer.ui.theme.MonokaiBlue
import com.festerhead.cygnusplayer.ui.theme.MonokaiPurple
import com.festerhead.cygnusplayer.ui.theme.MonokaiSecondaryText
import com.festerhead.cygnusplayer.ui.theme.MonokaiText

/**
 * Dialog displaying granular song metadata and ReplayGain statistics.
 *
 * Presents a two-column table (Name and Value) strictly adhering to the
 * Monokai Pro (Filter Spectrum) theme and accessibility requirements.
 *
 * @param songDetails The [SongDetails] model containing metadata and ReplayGain statistics.
 * @param onDismissRequest Callback invoked when the user dismisses the dialog or clicks Close.
 */
@Composable
fun SongDetailsDialog(
    songDetails: SongDetails,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = "Close",
                    color = MonokaiPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MonokaiPurple,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Song Details",
                    color = MonokaiPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                ) {
                    Text(
                        text = "NAME",
                        color = MonokaiSecondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "VALUE",
                        color = MonokaiSecondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1.3f),
                    )
                }

                HorizontalDivider(
                    color = MonokaiSecondaryText.copy(alpha = 0.35f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                // Metadata Rows
                songDetails.toDisplayList().forEach { (name, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = name,
                            color = MonokaiBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = value,
                            color = if (value == SongDetails.EMPTY_VALUE) MonokaiSecondaryText else MonokaiText,
                            fontSize = 13.sp,
                            fontStyle = if (value == SongDetails.EMPTY_VALUE) FontStyle.Italic else FontStyle.Normal,
                            modifier = Modifier.weight(1.3f),
                        )
                    }
                    HorizontalDivider(
                        color = MonokaiSecondaryText.copy(alpha = 0.12f),
                        thickness = 0.5.dp,
                    )
                }
            }
        },
        containerColor = Color(0xFF222222),
        shape = RoundedCornerShape(16.dp),
    )
}

@Preview(name = "Song Details - Populated", showBackground = true)
@Composable
private fun SongDetailsDialogPopulatedPreview() {
    CygnusPlayerTheme {
        SongDetailsDialog(
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
            onDismissRequest = {},
        )
    }
}

@Preview(name = "Song Details - Empty Fallbacks", showBackground = true)
@Composable
private fun SongDetailsDialogEmptyPreview() {
    CygnusPlayerTheme {
        SongDetailsDialog(
            songDetails = SongDetails(),
            onDismissRequest = {},
        )
    }
}

@Preview(name = "Song Details - Long Multi-Line Content", showBackground = true)
@Composable
private fun SongDetailsDialogLongContentPreview() {
    CygnusPlayerTheme {
        SongDetailsDialog(
            songDetails = SongDetails(
                artistName = "Rush feat. Special Guest Orchestra",
                trackTitle = "2112: Overture / The Temples of Syrinx / Discovery / Presentation / Oracle: The Dream / Soliloquy / Grand Finale",
                albumTitle = "2112 (40th Anniversary Super Deluxe Edition Remastered)",
                date = "1976-04-01",
                genre = "Progressive Rock / Hard Rock / Concept Album",
                comment = "Recorded at Toronto Sound Studios, Toronto, Ontario. Mixed by Terry Brown. Remastered from the original analog multi-track tapes at Abbey Road Studios.",
                trackGain = "-9.84 dB",
                trackPeak = "0.998741",
                albumGain = "-8.95 dB",
                albumPeak = "1.000000",
            ),
            onDismissRequest = {},
        )
    }
}

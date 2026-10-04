package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SamSurface
import com.example.ui.theme.SamSurfaceBorder
import com.example.ui.theme.SamSurfaceElevated
import com.example.ui.theme.SamTextMuted
import com.example.ui.theme.SamTextPrimary
import com.example.ui.theme.SamTextSecondary
import kotlin.math.roundToInt

@Composable
fun MusicStudioDialog(
    onDismiss: () -> Unit,
    onGenerate: (genre: String, tempo: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val genres = remember { listOf("Lofi", "Cinematic", "Rap", "Synthwave", "Acoustic") }
    var selectedGenre by remember { mutableStateOf(genres[0]) }
    var tempo by remember { mutableStateOf(120f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "AI Music Studio",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SamTextPrimary
                        )
                    )
                    Text(
                        text = "Compose chord sheets & custom lyrics",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SamTextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Genre selection
                Column {
                    Text(
                        text = "Select Music Genre",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SamTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        genres.forEach { genre ->
                            val isSelected = selectedGenre == genre
                            AssistChip(
                                onClick = { selectedGenre = genre },
                                label = { Text(genre) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else SamSurface,
                                    labelColor = if (isSelected) MaterialTheme.colorScheme.primary else SamTextSecondary
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else SamSurfaceBorder
                                ),
                                modifier = Modifier.testTag("genre_chip_$genre")
                            )
                        }
                    }
                }

                // Tempo Selection
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Track Tempo",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = SamTextPrimary
                            )
                        )
                        Text(
                            text = "${tempo.roundToInt()} BPM",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Slider(
                        value = tempo,
                        onValueChange = { tempo = it },
                        valueRange = 60f..180f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = SamSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("tempo_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("60 BPM", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = SamTextMuted))
                        Text("Ambient", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = SamTextMuted))
                        Text("180 BPM", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = SamTextMuted))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGenerate(selectedGenre, tempo.roundToInt()) },
                modifier = Modifier.testTag("generate_track_button")
            ) {
                Text(
                    text = "Generate Track",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_studio_button")
            ) {
                Text(
                    text = "Cancel",
                    color = SamTextSecondary
                )
            }
        },
        containerColor = SamSurface,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    )
}

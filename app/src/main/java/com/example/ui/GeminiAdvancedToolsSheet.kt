package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiAdvancedToolsSheet(
    onDismiss: () -> Unit,
    personalIntelligenceEnabled: Boolean,
    onPersonalIntelligenceToggled: (Boolean) -> Unit,
    onActionSelected: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SamDarkBg,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Drag Handle Alternative / Title Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Advanced Gemini Tools",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = SamTextPrimary,
                        fontSize = 18.sp
                    )
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SamSurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Panel",
                        tint = SamTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Divider
            HorizontalDivider(color = SamSurfaceBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: Horizontal Row (Photos, Camera, Avatar, Files)
            Text(
                text = "Quick Attachments",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = SamCyanGlow,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val horizontalTools = listOf(
                    Triple("Photos", Icons.Default.PhotoLibrary, SamPurplePrimary),
                    Triple("Camera", Icons.Default.PhotoCamera, SamCyan),
                    Triple("Avatar", Icons.Default.Face, SamMagenta),
                    Triple("Files", Icons.Default.FolderOpen, SamTextSecondary)
                )

                horizontalTools.forEach { (name, icon, color) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onActionSelected(name) }
                            .padding(vertical = 8.dp)
                    ) {
                        Surface(
                            color = color.copy(alpha = 0.15f),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = name,
                                    tint = color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SamTextPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Vertical List (Images, Videos, Music, Canvas, Deep Research, Guided Learning, and Personal Intelligence toggle switch)
            Text(
                text = "Workspace & Services",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = SamCyanGlow,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 10.dp)
            )

            val verticalTools = listOf(
                Triple("Images", Icons.Default.Image, "Browse or search attached gallery images"),
                Triple("Videos", Icons.Default.VideoLibrary, "Explore your local or shared media clips"),
                Triple("Music", Icons.Default.MusicNote, "Listen or sync your customized sound library"),
                Triple("Set Alarm", Icons.Default.Alarm, "Schedule clock alarms and wake-up alerts"),
                Triple("Send SMS", Icons.Default.Sms, "Compose or dispatch SMS text messages"),
                Triple("Toggle Wi-Fi", Icons.Default.Wifi, "Control device Wi-Fi and network connectivity"),
                Triple("Canvas", Icons.Default.Palette, "Open code/document interactive workspace"),
                Triple("Deep Research", Icons.Default.Search, "Toggle deep search grounding tools"),
                Triple("Guided Learning", Icons.Default.School, "Get structured explanations from JARVIS")
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Regular vertical tools
                items(verticalTools.size) { idx ->
                    val (name, icon, desc) = verticalTools[idx]
                    Surface(
                        color = SamSurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onActionSelected(name) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = name,
                                tint = SamPurpleSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = SamTextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = SamTextMuted,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Go",
                                tint = SamTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Personal Intelligence Switch item
                item {
                    Surface(
                        color = SamSurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Personal Intelligence",
                                tint = SamMagenta,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Personal Intelligence",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = SamTextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "Allow JARVIS to adapt to your style and memory",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = SamTextMuted,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Switch(
                                checked = personalIntelligenceEnabled,
                                onCheckedChange = onPersonalIntelligenceToggled,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SamDarkBg,
                                    checkedTrackColor = SamMagenta,
                                    uncheckedThumbColor = SamTextMuted,
                                    uncheckedTrackColor = SamSurface
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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

data class PersonaOption(
    val id: String,
    val title: String,
    val description: String
)

@Composable
fun AvatarPersonaDialog(
    currentPersona: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val personas = remember {
        listOf(
            PersonaOption("JARVIS", "Classic JARVIS", "Crisp, elegant, formal & highly sophisticated British AI (Default)"),
            PersonaOption("SAM", "Casual Sam", "Witty, playful, extremely friendly, and conversational buddy"),
            PersonaOption("CYBERPUNK", "Cyberpunk Rebel", "Dry, direct, street-smart rebel cybernetic assistant"),
            PersonaOption("BUDDY", "Buddy (Hinglish)", "Warm, extremely casual, friendly Hinglish roomie vibes")
        )
    }

    var selectedPersona by remember {
        mutableStateOf(personas.find { it.id.equals(currentPersona, ignoreCase = true) } ?: personas[0])
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "JARVIS Persona Studio",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SamTextPrimary
                        )
                    )
                    Text(
                        text = "Customize voice tone & instructions",
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                personas.forEach { persona ->
                    val isSelected = selectedPersona == persona
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPersona = persona }
                            .testTag("persona_card_${persona.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) SamSurfaceElevated else SamSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else SamSurfaceBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedPersona = persona },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = SamTextMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = persona.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) SamTextPrimary else SamTextSecondary
                                    )
                                )
                                Text(
                                    text = persona.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = SamTextMuted
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedPersona.id) },
                modifier = Modifier.testTag("confirm_persona_button")
            ) {
                Text(
                    text = "Apply Persona",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_persona_button")
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

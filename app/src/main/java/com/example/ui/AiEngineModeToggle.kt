package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gemini.AiEngineMode
import com.example.ui.theme.SamCyan
import com.example.ui.theme.SamDarkBg
import com.example.ui.theme.SamPurplePrimary
import com.example.ui.theme.SamPurpleSecondary
import com.example.ui.theme.SamSurface
import com.example.ui.theme.SamSurfaceBorder
import com.example.ui.theme.SamSurfaceElevated
import com.example.ui.theme.SamTextMuted
import com.example.ui.theme.SamTextPrimary
import com.example.ui.theme.SamTextSecondary

/**
 * Modern M3 Dual-Engine Selector Toggle for Sam AI:
 * - ⚡ Quick Response: Ultra-fast answers using "gemini-3.8-flash" (temp 0.5, maxTokens 350)
 * - 🧠 Deep Thought: Complex reasoning using "gemini-3.1-pro-preview" (temp 0.7, maxTokens 4096, thinkingLevel high)
 */
@Composable
fun AiEngineModeToggle(
    engineMode: AiEngineMode,
    onEngineModeSelected: (AiEngineMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SamSurfaceElevated,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, SamSurfaceBorder),
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("ai_engine_mode_toggle")
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quick Response Mode Option
            val isQuick = engineMode == AiEngineMode.QUICK
            val quickBg by animateColorAsState(
                targetValue = if (isQuick) SamCyan.copy(alpha = 0.22f) else Color.Transparent,
                label = "quick_bg"
            )
            val quickBorder by animateColorAsState(
                targetValue = if (isQuick) SamCyan else Color.Transparent,
                label = "quick_border"
            )

            Surface(
                color = quickBg,
                shape = RoundedCornerShape(18.dp),
                border = if (isQuick) BorderStroke(1.dp, quickBorder) else null,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onEngineModeSelected(AiEngineMode.QUICK) }
                    .testTag("ai_engine_toggle_quick")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "⚡",
                        fontSize = 13.sp
                    )
                    Column {
                        Text(
                            text = "Quick Response",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isQuick) FontWeight.Bold else FontWeight.Medium,
                                color = if (isQuick) SamCyan else SamTextSecondary,
                                fontSize = 11.5.sp
                            )
                        )
                        if (isQuick) {
                            Text(
                                text = "3.8 Flash • Fast",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 9.sp,
                                    color = SamCyan.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }

            // Deep Thought Mode Option
            val isDeep = engineMode == AiEngineMode.DEEP_THINK
            val deepBg by animateColorAsState(
                targetValue = if (isDeep) SamPurplePrimary.copy(alpha = 0.25f) else Color.Transparent,
                label = "deep_bg"
            )
            val deepBorder by animateColorAsState(
                targetValue = if (isDeep) SamPurplePrimary else Color.Transparent,
                label = "deep_border"
            )

            Surface(
                color = deepBg,
                shape = RoundedCornerShape(18.dp),
                border = if (isDeep) BorderStroke(1.dp, deepBorder) else null,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onEngineModeSelected(AiEngineMode.DEEP_THINK) }
                    .testTag("ai_engine_toggle_deep")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🧠",
                        fontSize = 13.sp
                    )
                    Column {
                        Text(
                            text = "Deep Thought",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isDeep) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDeep) SamPurpleSecondary else SamTextSecondary,
                                fontSize = 11.5.sp
                            )
                        )
                        if (isDeep) {
                            Text(
                                text = "3.1 Pro • Thinking",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 9.sp,
                                    color = SamPurpleSecondary.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

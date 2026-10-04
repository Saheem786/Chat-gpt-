package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SamCyan
import com.example.ui.theme.SamCyanGlow
import com.example.ui.theme.SamDarkBg
import com.example.ui.theme.SamErrorRed
import com.example.ui.theme.SamMagenta
import com.example.ui.theme.SamPinkGlow
import com.example.ui.theme.SamPurplePrimary
import com.example.ui.theme.SamPurpleSecondary
import com.example.ui.theme.SamSurface
import com.example.ui.theme.SamSurfaceBorder
import com.example.ui.theme.SamSurfaceElevated
import com.example.ui.theme.SamTextMuted
import com.example.ui.theme.SamTextPrimary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-fidelity, real-time glowing Orb with expanding acoustic ripples and
 * orbital frequency spikes responding directly to microphone audio amplitude.
 */
@Composable
fun SamOrbVisualizer(
    state: AssistantState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_rotation")

    // Continuous rotation angle
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Breathing pulse for Idle
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Pulsing acoustic shockwave phase
    val ripplePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_phase"
    )

    // Smooth amplitude transition with fast attack and gentle decay
    val animatedAmp = remember { Animatable(0f) }
    LaunchedEffect(amplitude) {
        val target = amplitude.coerceIn(0f, 1f)
        val animDuration = if (target > animatedAmp.value) 50 else 120
        animatedAmp.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = animDuration, easing = FastOutSlowInEasing)
        )
    }

    val primaryColor = when (state) {
        AssistantState.IDLE -> SamPurplePrimary
        AssistantState.CONNECTING -> SamCyan
        AssistantState.LISTENING -> SamCyanGlow
        AssistantState.THINKING -> SamPurpleSecondary
        AssistantState.SPEAKING -> SamMagenta
        AssistantState.ERROR -> SamErrorRed
    }

    val secondaryColor = when (state) {
        AssistantState.IDLE -> SamPurpleSecondary
        AssistantState.CONNECTING -> SamPurplePrimary
        AssistantState.LISTENING -> SamCyan
        AssistantState.THINKING -> SamCyanGlow
        AssistantState.SPEAKING -> SamPurplePrimary
        AssistantState.ERROR -> Color(0xFFFB7185)
    }

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 4.4f
            val ampValue = animatedAmp.value
            val ampScale = ampValue * 36f
            val pulseScale = if (state == AssistantState.IDLE) breathingPulse else 1f

            val currentRadius = (baseRadius + ampScale) * pulseScale

            // 1. Dynamic acoustic expanding ripples (Reacting to microphone audio)
            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                // Expanding ripple shockwaves
                for (i in 0..2) {
                    val progress = (ripplePhase + (i * 0.33f)) % 1f
                    val waveRadius = currentRadius + (progress * (60f + (ampValue * 45f)))
                    val alpha = ((1f - progress) * (0.35f + (ampValue * 0.45f))).coerceIn(0f, 1f)

                    drawCircle(
                        color = primaryColor.copy(alpha = alpha),
                        radius = waveRadius,
                        center = center,
                        style = Stroke(width = (4f * (1f - progress) + (ampValue * 2f)).coerceAtLeast(1f))
                    )
                }

                // Stationary glowing energy envelopes
                for (i in 1..3) {
                    val rippleRadius = currentRadius + (i * 20f * (0.7f + ampValue))
                    drawCircle(
                        color = primaryColor.copy(alpha = (0.28f / i) * (0.6f + ampValue)),
                        radius = rippleRadius,
                        center = center,
                        style = Stroke(width = (4f - i).coerceAtLeast(1f))
                    )
                }
            } else if (state == AssistantState.IDLE) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.08f),
                    radius = baseRadius * 1.35f * breathingPulse,
                    center = center
                )
            }

            // 2. Rotating orbital energy rays & gradient sweep
            val angleRad = Math.toRadians(rotation.toDouble())
            val orbitalGrad = Brush.sweepGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.85f),
                    secondaryColor.copy(alpha = 0.95f),
                    SamPinkGlow.copy(alpha = 0.85f),
                    primaryColor.copy(alpha = 0.85f)
                ),
                center = center
            )

            drawCircle(
                brush = orbitalGrad,
                radius = currentRadius + 8f + (ampValue * 8f),
                center = center,
                style = Stroke(width = 4.5f + (ampValue * 7f))
            )

            // 3. Central Glowing Core with radial aura
            val coreGradient = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    secondaryColor.copy(alpha = 0.88f),
                    primaryColor.copy(alpha = 0.95f),
                    primaryColor.copy(alpha = 0.25f)
                ),
                center = center,
                radius = currentRadius
            )

            drawCircle(
                brush = coreGradient,
                radius = currentRadius,
                center = center
            )

            // 4. Dynamic harmonic wave spikes around circumference
            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                val pointCount = 42
                for (p in 0 until pointCount) {
                    val pAngle = (p.toDouble() / pointCount) * 2.0 * PI + angleRad
                    val waveMod = sin(pAngle * 5.0 + (angleRad * 2.5)) * (ampValue * 26f + 4f)
                    val pRadius = currentRadius + waveMod.toFloat()
                    val x = center.x + (pRadius * cos(pAngle)).toFloat()
                    val y = center.y + (pRadius * sin(pAngle)).toFloat()

                    drawCircle(
                        color = Color.White.copy(alpha = (0.7f + (ampValue * 0.3f)).coerceAtMost(1f)),
                        radius = 2.5f + (ampValue * 3.5f),
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

/**
 * Real-time dynamic audio waveform & spectrum frequency visualizer.
 * Renders animated multi-harmonic sine waves and responsive vertical frequency spectrum bars
 * that scale actively with the microphone audio amplitude.
 */
@Composable
fun RealtimeAudioWaveformVisualizer(
    state: AssistantState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_phase")

    // Flowing wave phase
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Smooth amplitude interpolation
    val animatedAmp = remember { Animatable(0f) }
    LaunchedEffect(amplitude) {
        val target = amplitude.coerceIn(0f, 1f)
        animatedAmp.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 60, easing = FastOutSlowInEasing)
        )
    }

    val waveColor = when (state) {
        AssistantState.LISTENING -> SamCyanGlow
        AssistantState.SPEAKING -> SamMagenta
        AssistantState.THINKING -> SamPurpleSecondary
        AssistantState.ERROR -> SamErrorRed
        else -> SamCyan
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = SamSurfaceElevated.copy(alpha = 0.75f)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(waveColor)
                    )
                    Text(
                        text = if (state == AssistantState.LISTENING) "LIVE AUDIO INPUT" else if (state == AssistantState.SPEAKING) "AUDIO OUTPUT" else "ACOUSTIC SENSORS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = SamTextMuted
                        )
                    )
                }

                Text(
                    text = "${(animatedAmp.value * 100).toInt()}% dB",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = waveColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Waveform & Frequency Spectrum Bars
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                val width = size.width
                val height = size.height
                val midY = height / 2f
                val amp = animatedAmp.value
                val isLive = state == AssistantState.LISTENING || state == AssistantState.SPEAKING

                // 1. Draw Multi-Layered Continuous Fluid Sine Waves
                val wavePath1 = Path()
                val wavePath2 = Path()

                val points = 60
                val dx = width / points

                wavePath1.moveTo(0f, midY)
                wavePath2.moveTo(0f, midY)

                for (i in 0..points) {
                    val x = i * dx
                    val normalizedX = (x / width) * 2.0 * PI

                    // Primary harmonic wave
                    val baseAmp1 = if (isLive) (amp * 26f + 4f) else 3f
                    val y1 = midY + (sin(normalizedX * 2.5 + phase.toDouble()) * baseAmp1).toFloat()
                    if (i == 0) wavePath1.moveTo(x, y1) else wavePath1.lineTo(x, y1)

                    // Secondary phase-shifted harmonic wave
                    val baseAmp2 = if (isLive) (amp * 20f + 3f) else 2f
                    val y2 = midY + (cos(normalizedX * 3.2 - phase.toDouble() * 1.3) * baseAmp2).toFloat()
                    if (i == 0) wavePath2.moveTo(x, y2) else wavePath2.lineTo(x, y2)
                }

                // Draw secondary softer glow wave
                drawPath(
                    path = wavePath2,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            SamPurplePrimary.copy(alpha = 0.3f),
                            SamCyan.copy(alpha = 0.6f),
                            SamPinkGlow.copy(alpha = 0.4f)
                        )
                    ),
                    style = Stroke(width = 2.5f)
                )

                // Draw primary vibrant neon wave
                drawPath(
                    path = wavePath1,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            SamPurpleSecondary.copy(alpha = 0.6f),
                            waveColor.copy(alpha = 0.95f),
                            SamPinkGlow.copy(alpha = 0.8f)
                        )
                    ),
                    style = Stroke(width = 3.5f + (amp * 2f))
                )

                // 2. Draw Symmetric Frequency Spectrum Bars
                val barCount = 31
                val barWidth = (width / (barCount * 1.6f)).coerceIn(3f, 8f)
                val spacing = (width - (barCount * barWidth)) / (barCount + 1)

                for (b in 0 until barCount) {
                    val x = spacing + b * (barWidth + spacing)
                    // Bell-curve distribution for natural audio spectrum
                    val centerFactor = 1f - (Math.abs(b - barCount / 2f) / (barCount / 2f))
                    val harmonicMod = (sin(b * 0.6 + phase.toDouble() * 2.0) + 1.0) / 2.0

                    val barHeight = if (isLive) {
                        (4f + (amp * 48f * centerFactor) + (harmonicMod.toFloat() * amp * 18f))
                            .coerceIn(4f, height - 6f)
                    } else {
                        (3f + (centerFactor * 6f * harmonicMod.toFloat())).coerceIn(3f, 14f)
                    }

                    val top = midY - (barHeight / 2f)
                    val barColor = if (centerFactor > 0.6f) {
                        waveColor
                    } else {
                        SamPurpleSecondary.copy(alpha = 0.7f + (centerFactor * 0.3f))
                    }

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                barColor.copy(alpha = 0.9f),
                                barColor.copy(alpha = 0.4f)
                            ),
                            startY = top,
                            endY = top + barHeight
                        ),
                        topLeft = Offset(x, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }
    }
}

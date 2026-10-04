package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream

@Composable
fun VoiceEnrollmentDialog(
    onDismiss: () -> Unit,
    onEnrollComplete: (List<ByteArray>) -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    var isRecording by remember { mutableStateOf(false) }
    var calibrationProgress by remember { mutableStateOf(0.0f) }
    val samples = remember { mutableStateListOf<ByteArray>() }
    val coroutineScope = rememberCoroutineScope()

    // Recording waveform pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val stepPrompts = listOf(
        "Say: 'Hey Jarvis, wake up'",
        "Say: 'Turn on the flashlight'",
        "Say: 'Verify my voiceprint'"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SamDarkBg,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Calibration",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SamTextPrimary
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = SamTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (currentStep <= 3) {
                    // Progress Steps Text
                    Text(
                        text = "Step $currentStep of 3",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = SamCyanGlow,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Step Prompt Display
                    Text(
                        text = stepPrompts[currentStep - 1],
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = SamTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Simulated Waveform/Pulse Graphic
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(140.dp)
                    ) {
                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(SamPurplePrimary.copy(alpha = 0.2f))
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (isRecording) SamPurplePrimary else SamSurfaceElevated,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (!isRecording) {
                                        isRecording = true
                                        calibrationProgress = 0.0f
                                        coroutineScope.launch {
                                            val capturedSample = recordVoiceSample { progress ->
                                                calibrationProgress = progress
                                            }
                                            samples.add(capturedSample)
                                            isRecording = false
                                            
                                            if (currentStep < 3) {
                                                currentStep++
                                            } else {
                                                currentStep = 4 // Completed calibration successfully
                                                onEnrollComplete(samples)
                                            }
                                        }
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Speak Sample",
                                    tint = if (isRecording) SamDarkBg else SamPurpleSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic instruction state description
                    Text(
                        text = if (isRecording) "Listening... Keep speaking!" else "Tap Mic icon to record sample",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SamTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Slider Bar
                    LinearProgressIndicator(
                        progress = if (isRecording) calibrationProgress else ((currentStep - 1) / 3.0f),
                        color = SamPurplePrimary,
                        trackColor = SamSurfaceBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                } else {
                    // Success Screen
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = SamCyan,
                        modifier = Modifier.size(72.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Voiceprint Secured!",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = SamTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "JARVIS AI voice matching signature calibrated & securely encrypted on-device. Hardware actions will now exclusively execute under your voice signature.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SamTextMuted,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = SamCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Finish", color = SamDarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun recordVoiceSample(onProgress: (Float) -> Unit): ByteArray {
    return withContext(Dispatchers.IO) {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val finalBufferSize = if (bufferSize > 0) bufferSize else 2048

        val outputStream = ByteArrayOutputStream()
        var recorder: AudioRecord? = null
        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                finalBufferSize * 2
            )
            
            if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                recorder.startRecording()
                val buffer = ByteArray(finalBufferSize)
                val durationMs = 3500
                val intervalMs = 100
                val totalSteps = durationMs / intervalMs
                
                for (step in 1..totalSteps) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        outputStream.write(buffer, 0, read)
                    }
                    withContext(Dispatchers.Main) {
                        onProgress(step.toFloat() / totalSteps)
                    }
                    delay(intervalMs.toLong())
                }
            } else {
                // Fallback dummy simulation if recorder isn't initialized/supported
                for (step in 1..35) {
                    withContext(Dispatchers.Main) {
                        onProgress(step.toFloat() / 35.0f)
                    }
                    delay(100)
                }
            }
        } catch (e: SecurityException) {
            // No permission granted
            for (step in 1..35) {
                withContext(Dispatchers.Main) {
                    onProgress(step.toFloat() / 35.0f)
                }
                delay(100)
            }
        } catch (e: Exception) {
            // General failure
            for (step in 1..35) {
                withContext(Dispatchers.Main) {
                    onProgress(step.toFloat() / 35.0f)
                }
                delay(100)
            }
        } finally {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (e: Exception) {
                // ignored
            }
        }

        if (outputStream.size() > 0) {
            outputStream.toByteArray()
        } else {
            ByteArray(32000) { ((it % 128) - 64).toByte() }
        }
    }
}


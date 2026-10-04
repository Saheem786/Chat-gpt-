package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.example.ui.theme.SamCyan
import com.example.ui.theme.SamCyanGlow
import com.example.ui.theme.SamDarkBg
import com.example.ui.theme.SamErrorRed
import com.example.ui.theme.SamMagenta
import com.example.ui.theme.SamPurplePrimary
import com.example.ui.theme.SamPurpleSecondary
import com.example.ui.theme.SamSurface
import com.example.ui.theme.SamSurfaceBorder
import com.example.ui.theme.SamSurfaceElevated
import com.example.ui.theme.SamTextMuted
import com.example.ui.theme.SamTextPrimary
import com.example.ui.theme.SamTextSecondary

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SamScreen(
    viewModel: SamViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Accompanist Microphone Permission State
    val micPermissionState = rememberPermissionState(
        permission = Manifest.permission.RECORD_AUDIO
    )
    var showMicPermissionDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.showSettingsDrawer) {
        if (uiState.showSettingsDrawer && drawerState.isClosed) {
            drawerState.open()
        } else if (!uiState.showSettingsDrawer && drawerState.isOpen) {
            drawerState.close()
        }
    }

    LaunchedEffect(drawerState.isOpen) {
        if (!drawerState.isOpen && uiState.showSettingsDrawer) {
            viewModel.toggleSettingsDrawer(false)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    viewModel.setAttachedImage(base64)
                    Toast.makeText(context, "Image attached!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Image error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleMicClick() {
        if (micPermissionState.status.isGranted) {
            viewModel.toggleSession()
        } else if (micPermissionState.status.shouldShowRationale) {
            showMicPermissionDialog = true
        } else {
            showMicPermissionDialog = true
        }
    }

    if (uiState.showThemeSettings) {
        ThemeSettingsScreen(
            currentAccent = uiState.themeAccent,
            onAccentSelected = { viewModel.changeThemeAccent(it) },
            onBack = { viewModel.toggleThemeSettings(false) }
        )
    } else if (uiState.showHelpCommandsScreen) {
        HelpCommandsScreen(
            onBack = { viewModel.toggleHelpCommandsScreen(false) },
            onExecuteCommand = { command ->
                viewModel.toggleHelpCommandsScreen(false)
                viewModel.sendChatMessage(command)
            }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                SettingsDrawerSheet(
                    selectedLanguage = uiState.selectedLanguage,
                    onLanguageSelected = { lang ->
                        viewModel.setLanguage(lang)
                    },
                    currentApiKey = uiState.apiKey,
                    onSaveApiKey = { key -> viewModel.updateApiKey(key) },
                    currentSelectedModel = uiState.selectedModel,
                    onModelSelected = { model ->
                        viewModel.updateSettings(uiState.apiKey, model, uiState.selectedLanguage)
                    },
                    engineMode = uiState.engineMode,
                    onEngineModeChanged = { mode ->
                        viewModel.setEngineMode(mode)
                    },
                    voiceMatchEnforced = uiState.voiceMatchEnforced,
                    onVoiceMatchEnforcedChanged = { enabled -> viewModel.toggleVoiceMatchEnforced(enabled) },
                    isVoiceCalibrationCompleted = uiState.isVoiceCalibrationCompleted,
                    onOpenVoiceEnrollment = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.toggleSettingsDrawer(false)
                        viewModel.toggleVoiceEnrollmentDialog(true)
                    },
                    floatingWidgetEnabled = uiState.floatingWidgetEnabled,
                    onFloatingWidgetEnabledChanged = { enabled -> viewModel.toggleFloatingWidget(enabled) },
                    wakeWordEnabled = uiState.wakeWordEnabled,
                    onWakeWordEnabledChanged = { enabled -> viewModel.toggleWakeWord(enabled) },
                    wakeWordOption = uiState.wakeWordOption,
                    onWakeWordOptionChanged = { option -> viewModel.setWakeWordOption(option) },
                    personalIntelligenceEnabled = uiState.personalIntelligenceEnabled,
                    onPersonalIntelligenceToggled = { enabled -> viewModel.togglePersonalIntelligence(enabled) },
                    currentThemeAccent = uiState.themeAccent,
                    onOpenFullThemeSettings = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.toggleSettingsDrawer(false)
                        viewModel.toggleThemeSettings(true)
                    },
                    onOpenHelp = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.toggleSettingsDrawer(false)
                        viewModel.toggleHelpCommandsScreen(true)
                    },
                    onOpenDiagnostics = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.toggleSettingsDrawer(false)
                        viewModel.toggleDiagnosticsSheet(true)
                    },
                    onClearChat = {
                        viewModel.clearChatHistory()
                    },
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.toggleSettingsDrawer(false)
                    },
                    ttsVoiceStyle = uiState.ttsVoiceStyle,
                    onChangeTtsVoiceStyle = { style ->
                        viewModel.changeTtsVoiceStyle(style)
                    }
                )
            }
        ) {
            Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = SamDarkBg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SamDarkBg)
                        .statusBarsPadding()
                ) {
                    SamTopBar(
                        state = uiState.state,
                        selectedLanguage = uiState.selectedLanguage,
                        onOpenHelp = { viewModel.toggleHelpCommandsScreen(true) },
                        onOpenDiagnostics = { viewModel.toggleDiagnosticsSheet(true) },
                        onOpenSettings = {
                            coroutineScope.launch { drawerState.open() }
                            viewModel.toggleSettingsDrawer(true)
                        },
                        onOpenLanguageSelection = {
                            coroutineScope.launch { drawerState.open() }
                            viewModel.toggleSettingsDrawer(true)
                        }
                    )
                AppModeToggle(
                    currentMode = uiState.currentMode,
                    onModeSelected = { mode ->
                        if (mode == AppMode.VOICE && uiState.currentMode != AppMode.VOICE) {
                            viewModel.setAppMode(AppMode.VOICE)
                            if (micPermissionState.status.isGranted) {
                                viewModel.startSession()
                            } else {
                                showMicPermissionDialog = true
                            }
                        } else {
                            viewModel.setAppMode(mode)
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Dual-Engine Mode Toggle (Quick Response vs Deep Thought)
                AiEngineModeToggle(
                    engineMode = uiState.engineMode,
                    onEngineModeSelected = { mode ->
                        viewModel.setEngineMode(mode)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Always-On Wake Word Status Badge (if enabled)
                if (uiState.wakeWordEnabled) {
                    Surface(
                        color = SamMagenta.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SamMagenta.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 2.dp, bottom = 2.dp)
                            .clickable {
                                coroutineScope.launch { drawerState.open() }
                                viewModel.toggleSettingsDrawer(true)
                            }
                            .testTag("wake_word_status_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "🎙️",
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Wake Word: '${uiState.wakeWordOption.phrase}' Active",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SamMagenta
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    ) { innerPadding ->
        if (uiState.currentMode == AppMode.CHAT) {
            ChatScreenView(
                messages = uiState.chatMessages,
                inputText = uiState.inputText,
                isSending = uiState.isSendingChat,
                quickReplies = uiState.quickReplies,
                attachedImageBase64 = uiState.attachedImageBase64,
                engineMode = uiState.engineMode,
                onEngineModeChanged = { viewModel.setEngineMode(it) },
                onRemoveAttachedImage = { viewModel.setAttachedImage(null) },
                onAttachImage = { viewModel.setAttachedImage(it) },
                onOpenToolsSheet = {
                    viewModel.toggleToolsSheet(true)
                },
                onConfigureQuickReplies = { viewModel.toggleQuickRepliesConfigDialog(true) },
                onInputChanged = { viewModel.updateInputText(it) },
                onSendMessage = { viewModel.sendChatMessage(it) },
                onSpeakMessage = { viewModel.speakMessage(it) },
                onClearChat = { viewModel.clearChatHistory() },
                onSwitchToVoice = {
                    viewModel.setAppMode(AppMode.VOICE)
                    handleMicClick()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Assistant State Badge
                AssistantStatusBadge(
                    state = uiState.state,
                    language = uiState.selectedLanguage
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Banner (if active tool execution or feedback)
                AnimatedVisibility(
                    visible = uiState.actionBanner != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    uiState.actionBanner?.let { banner ->
                        ActionFeedbackBanner(message = banner)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Error Message Banner (if error occurred)
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    uiState.errorMessage?.let { err ->
                        Surface(
                            color = SamErrorRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SamErrorRed.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = err,
                                color = Color(0xFFFDA4AF),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Central Orb Visualizer with dynamic acoustic ripples
                SamOrbVisualizer(
                    state = uiState.state,
                    amplitude = uiState.amplitude,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Real-time Audio Waveform & Frequency Spectrum reacting live to microphone input
                RealtimeAudioWaveformVisualizer(
                    state = uiState.state,
                    amplitude = uiState.amplitude,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Live User & Assistant Transcript
                Surface(
                    color = SamSurfaceElevated.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = uiState.transcript,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = SamTextPrimary,
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                }

                // Image preview strip in Voice Mode if image is attached
                uiState.attachedImageBase64?.let { base64 ->
                    Row(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SamSurfaceElevated)
                            .border(1.dp, SamCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val bitmap = remember(base64) {
                            try {
                                val cleanBase64 = base64.substringAfter("base64,").trim()
                                val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Attached image preview",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        Text(
                            text = "Photo Attached",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SamCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove photo",
                            tint = SamTextMuted,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { viewModel.setAttachedImage(null) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Action Suggestion Chips
                QuickActionChips(
                    onChipClick = { prompt -> viewModel.sendQuickPrompt(prompt) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Persistent Conversation Logs Displayed Alongside Voice Mode
                VoiceConversationLogsCard(
                    messages = uiState.chatMessages,
                    onOpenFullChat = { viewModel.setAppMode(AppMode.CHAT) },
                    onClearHistory = { viewModel.clearChatHistory() },
                    onSpeakMessage = { viewModel.speakMessage(it) }
                )
            }

            // Bottom Section: Speaker Diagnostic & Big Power/Mic Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 16.dp)
            ) {
                // Speaker Diagnostic Bar
                SpeakerDiagnosticBar(
                    onTestSpeaker = { viewModel.testSpeaker() }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Big Tactile Microphone / Power Button
                val buttonBgColor by animateColorAsState(
                    targetValue = when (uiState.state) {
                        AssistantState.IDLE -> SamPurplePrimary
                        AssistantState.CONNECTING -> SamCyan
                        AssistantState.LISTENING -> SamCyanGlow
                        AssistantState.THINKING -> SamPurpleSecondary
                        AssistantState.SPEAKING -> SamMagenta
                        AssistantState.ERROR -> SamErrorRed
                    },
                    label = "btn_bg"
                )

                val buttonScale by animateFloatAsState(
                    targetValue = if (uiState.isMicActive) 1.08f else 1.0f,
                    label = "btn_scale"
                )

                val micRippleScale by animateFloatAsState(
                    targetValue = if (uiState.isMicActive) (1.0f + uiState.amplitude * 0.45f) else 1.0f,
                    label = "mic_ripple_scale"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    // Left flanking button: Camera / Gallery Attachment Dialog
                    var showVoiceImageSourceDialog by remember { mutableStateOf(false) }

                    val voiceCameraLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.TakePicturePreview()
                    ) { bitmap ->
                        if (bitmap != null) {
                            try {
                                val outputStream = java.io.ByteArrayOutputStream()
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
                                val bytes = outputStream.toByteArray()
                                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                                viewModel.setAttachedImage(base64)
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }

                    val voiceGalleryLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri ->
                        if (uri != null) {
                            try {
                                val inputStream = context.contentResolver.openInputStream(uri)
                                val bytes = inputStream?.readBytes()
                                if (bytes != null) {
                                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                                    viewModel.setAttachedImage(base64)
                                }
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }

                    if (showVoiceImageSourceDialog) {
                        AlertDialog(
                            onDismissRequest = { showVoiceImageSourceDialog = false },
                            title = { Text("Attach Image") },
                            text = { Text("Capture or select a photo to ask JARVIS:") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showVoiceImageSourceDialog = false
                                        voiceCameraLauncher.launch(null)
                                    }
                                ) {
                                    Text("Take Photo")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showVoiceImageSourceDialog = false
                                        voiceGalleryLauncher.launch("image/*")
                                    }
                                ) {
                                    Text("Choose from Gallery")
                                }
                            },
                            containerColor = SamSurfaceElevated,
                            titleContentColor = SamTextPrimary,
                            textContentColor = SamTextSecondary
                        )
                    }

                    IconButton(
                        onClick = { showVoiceImageSourceDialog = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SamSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Attach Multimodal Image",
                            tint = SamCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Main interactive button Box
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(104.dp)
                            .scale(buttonScale)
                    ) {
                        // Dynamic acoustic ripple shockwave around mic button
                        if (uiState.isMicActive) {
                            Box(
                                modifier = Modifier
                                    .size(104.dp)
                                    .scale(micRippleScale)
                                    .clip(CircleShape)
                                    .background(buttonBgColor.copy(alpha = (0.15f + uiState.amplitude * 0.25f).coerceIn(0f, 0.4f)))
                            )
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(buttonBgColor.copy(alpha = 0.28f))
                            )
                        }

                        // Main interactive button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(16.dp, CircleShape, spotColor = buttonBgColor)
                                .clip(CircleShape)
                                .background(buttonBgColor)
                                .clickable { handleMicClick() }
                                .testTag("microphone_power_button")
                        ) {
                            when (uiState.state) {
                                AssistantState.CONNECTING, AssistantState.THINKING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = SamDarkBg,
                                        strokeWidth = 3.dp
                                    )
                                }
                                AssistantState.LISTENING -> {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Microphone Active",
                                        tint = SamDarkBg,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                AssistantState.SPEAKING -> {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop Speaking",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.PowerSettingsNew,
                                        contentDescription = "Start JARVIS",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Right flanking Symmetry Settings Switch button
                    IconButton(
                        onClick = { viewModel.toggleSettingsDialog(true) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SamSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Settings Dialog",
                            tint = SamPurpleSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (uiState.isMicActive) "Tap to pause or stop JARVIS" else "Tap to activate JARVIS",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SamTextMuted,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }

    // Diagnostics Bottom Sheet
    if (uiState.showDiagnosticsSheet) {
        DiagnosticsBottomSheet(
            logs = uiState.diagnosticLogs,
            onDismiss = { viewModel.toggleDiagnosticsSheet(false) },
            onClearLogs = { viewModel.clearLogs() }
        )
    }

    // Settings Dialog
    if (uiState.showSettingsDialog) {
        SettingsDialog(
            currentApiKey = uiState.apiKey,
            currentSelectedModel = uiState.selectedModel,
            selectedLanguage = uiState.selectedLanguage,
            onLanguageSelected = { lang -> viewModel.setLanguage(lang) },
            engineMode = uiState.engineMode,
            onEngineModeChanged = { mode -> viewModel.setEngineMode(mode) },
            voiceMatchEnforced = uiState.voiceMatchEnforced,
            onVoiceMatchEnforcedChanged = { enabled -> viewModel.toggleVoiceMatchEnforced(enabled) },
            isVoiceCalibrationCompleted = uiState.isVoiceCalibrationCompleted,
            onOpenVoiceEnrollment = {
                viewModel.toggleSettingsDialog(false)
                viewModel.toggleVoiceEnrollmentDialog(true)
            },
            onClearVoiceClicked = { viewModel.clearVoiceMatch() },
            floatingWidgetEnabled = uiState.floatingWidgetEnabled,
            onFloatingWidgetEnabledChanged = { enabled -> viewModel.toggleFloatingWidget(enabled) },
            wakeWordEnabled = uiState.wakeWordEnabled,
            onWakeWordEnabledChanged = { enabled -> viewModel.toggleWakeWord(enabled) },
            wakeWordOption = uiState.wakeWordOption,
            onWakeWordOptionChanged = { option -> viewModel.setWakeWordOption(option) },
            currentThemeAccent = uiState.themeAccent,
            onThemeAccentChanged = { accent -> viewModel.changeThemeAccent(accent) },
            onOpenFullThemeSettings = {
                viewModel.toggleSettingsDialog(false)
                viewModel.toggleThemeSettings(true)
            },
            onOpenHelp = {
                viewModel.toggleSettingsDialog(false)
                viewModel.toggleHelpCommandsScreen(true)
            },
            onSaveSettings = { newKey, newModel, newLanguage -> viewModel.updateSettings(newKey, newModel, newLanguage) },
            onSaveKey = { newKey -> viewModel.updateApiKey(newKey) },
            onDismiss = { viewModel.toggleSettingsDialog(false) }
        )
    }

    // Voice Enrollment Calibration Dialog
    if (uiState.showVoiceEnrollmentDialog) {
        VoiceEnrollmentDialog(
            onDismiss = { viewModel.toggleVoiceEnrollmentDialog(false) },
            onEnrollComplete = { samples -> viewModel.completeVoiceEnrollment(samples) }
        )
    }

    // Quick Replies Configuration Dialog
    if (uiState.showQuickRepliesConfigDialog) {
        QuickRepliesConfigDialog(
            quickReplies = uiState.quickReplies,
            onAddReply = { phrase -> viewModel.addQuickReply(phrase) },
            onRemoveReply = { phrase -> viewModel.removeQuickReply(phrase) },
            onResetReplies = { viewModel.resetQuickReplies() },
            onDismiss = { viewModel.toggleQuickRepliesConfigDialog(false) }
        )
    }

    // Canvas Workspace Panel View
    CanvasView(
        isVisible = uiState.showCanvas,
        canvasTitle = uiState.canvasTitle,
        canvasContent = uiState.canvasContent,
        contentType = uiState.canvasContentType,
        onClose = { viewModel.closeCanvas() },
        onCopyContent = { text ->
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Canvas Content", text)
            clipboard.setPrimaryClip(clip)
        }
    )

    // Gemini Advanced Tools Drawer Bottom Sheet
    if (uiState.showToolsSheet) {
        GeminiAdvancedToolsSheet(
            onDismiss = { viewModel.toggleToolsSheet(false) },
            personalIntelligenceEnabled = uiState.personalIntelligenceEnabled,
            onPersonalIntelligenceToggled = { enabled -> viewModel.togglePersonalIntelligence(enabled) },
            onActionSelected = { toolName ->
                viewModel.toggleToolsSheet(false) // Close sheet first
                when (toolName) {
                    "Photos" -> {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    "Canvas" -> {
                        viewModel.openCanvas(
                            title = "Advanced Algorithm Optimization",
                            contentType = "Kotlin",
                            content = """
                                // Optimized Fibonacci sequence using memoization in Kotlin
                                fun fibonacciMemo(n: Int): Long {
                                    val memo = HashMap<Int, Long>()
                                    
                                    fun solve(n: Int): Long {
                                        if (n <= 1) return n.toLong()
                                        if (memo.containsKey(n)) return memo[n]!!
                                        
                                        val ans = solve(n - 1) + solve(n - 2)
                                        memo[n] = ans
                                        return ans
                                    }
                                    
                                    return solve(n)
                                }
                                
                                fun main() {
                                    val n = 45
                                    println("Fibonacci of ${'$'}n is: ${'$'}{fibonacciMemo(n)}")
                                }
                            """.trimIndent()
                        )
                    }
                    "Deep Research" -> {
                        viewModel.setDeepResearchEnabled(!uiState.deepResearchEnabled)
                        val status = if (!uiState.deepResearchEnabled) "ENABLED" else "DISABLED"
                        Toast.makeText(context, "Deep Research $status!", Toast.LENGTH_SHORT).show()
                    }
                    "Images" -> {
                        viewModel.setImageGenMode(true)
                        viewModel.setAppMode(AppMode.CHAT)
                        Toast.makeText(context, "Image Generation Mode Active. Type your prompt to create an image.", Toast.LENGTH_LONG).show()
                    }
                    "Videos" -> {
                        viewModel.toggleVideoDurationDialog(true)
                    }
                    "Avatar" -> {
                        viewModel.toggleToolsSheet(false)
                        viewModel.toggleAvatarPersonaDialog(true)
                    }
                    "Music" -> {
                        viewModel.toggleToolsSheet(false)
                        viewModel.toggleMusicStudioDialog(true)
                    }
                    "Set Alarm" -> {
                        viewModel.executeDeviceTool("set_alarm", mapOf("hour" to 7, "minute" to 0, "label" to "Morning Alarm"))
                    }
                    "Send SMS" -> {
                        viewModel.executeDeviceTool("send_sms", mapOf("message" to "Hello from JARVIS AI!"))
                    }
                    "Toggle Wi-Fi" -> {
                        viewModel.executeDeviceTool("toggle_wifi", emptyMap<String, Any?>())
                    }
                    else -> {
                        Toast.makeText(context, "$toolName triggered!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Avatar Persona Selection Dialog
    if (uiState.showAvatarPersonaDialog) {
        AvatarPersonaDialog(
            currentPersona = uiState.currentPersona,
            onDismiss = { viewModel.toggleAvatarPersonaDialog(false) },
            onConfirm = { persona ->
                viewModel.setPersona(persona)
                Toast.makeText(context, "Persona set to $persona", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // AI Music Studio Dialog
    if (uiState.showMusicStudioDialog) {
        MusicStudioDialog(
            onDismiss = { viewModel.toggleMusicStudioDialog(false) },
            onGenerate = { genre, tempo ->
                viewModel.generateAITrack(genre, tempo)
                Toast.makeText(context, "Generating custom $genre track...", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Video Duration Selection Dialog
    if (uiState.showVideoDurationDialog) {
        VideoDurationDialog(
            onDismiss = { viewModel.toggleVideoDurationDialog(false) },
            onConfirm = { option ->
                viewModel.toggleVideoDurationDialog(false)
                viewModel.setVideoGenMode(true, option.id)
                viewModel.setAppMode(AppMode.CHAT)
                viewModel.updateInputText("[Mode: AI Video Generation - Duration: ${option.title}] ")
            }
        )
    }

    // Multiple Contacts Resolution Dialog
    uiState.multipleContacts?.let { contacts ->
        MultipleContactsDialog(
            contacts = contacts,
            onSelect = { contact -> viewModel.selectContactToCall(contact) },
            onDismiss = { viewModel.dismissContactsDialog() }
        )
    }

    // Accompanist Microphone Permission Dialog Flow
    if (showMicPermissionDialog && !micPermissionState.status.isGranted) {
        MicrophonePermissionDialog(
            permissionState = micPermissionState,
            onDismiss = { showMicPermissionDialog = false }
        )
    }

    // JARVIS Secure Confirmation Dialog
    uiState.pendingConfirmationCall?.let { pending ->
        AlertDialog(
            onDismissRequest = { viewModel.confirmPendingAction(false) },
            containerColor = com.example.ui.theme.SamSurfaceElevated,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Security Alert",
                        tint = com.example.ui.theme.SamCyanGlow,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Secure Authorization Gate",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.SamTextPrimary
                        )
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = pending.promptMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(color = com.example.ui.theme.SamTextPrimary)
                    )
                    Text(
                        text = "This action requires manual confirmation to execute. Do you authorize this on-device operation, Sir?",
                        style = MaterialTheme.typography.bodySmall.copy(color = com.example.ui.theme.SamTextSecondary)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmPendingAction(true) },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = com.example.ui.theme.SamCyanGlow
                    )
                ) {
                    Text("Authorize", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.confirmPendingAction(false) },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = com.example.ui.theme.SamTextSecondary
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // JARVIS Secure Action Pipeline Overlay
    AnimatedVisibility(
        visible = uiState.showPipelineOverlay,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        AlertDialog(
            onDismissRequest = { /* Force view of pipeline */ },
            containerColor = com.example.ui.theme.SamSurface,
            modifier = Modifier.border(1.5.dp, com.example.ui.theme.SamCyan.copy(alpha = 0.5f), shape = RoundedCornerShape(28.dp)),
            title = {
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
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(com.example.ui.theme.SamCyanGlow)
                        )
                        Text(
                            text = "JARVIS SECURE ACTION PIPELINE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = com.example.ui.theme.SamCyanGlow
                            )
                        )
                    }
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = com.example.ui.theme.SamCyanGlow
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    uiState.actionPipelineStages.forEachIndexed { index, stage ->
                        val stageColor = when (stage.status) {
                            "SUCCESS" -> com.example.ui.theme.SamSuccessGreen
                            "FAILED" -> com.example.ui.theme.SamErrorRed
                            else -> com.example.ui.theme.SamCyanGlow
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (stage.status == "ACTIVE") com.example.ui.theme.SamSurfaceElevated 
                                    else Color.Transparent, 
                                    shape = RoundedCornerShape(8.dp)
                                )
                                  .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Stage index bullet or loading spinner
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(stageColor.copy(alpha = 0.15f))
                                    .border(1.dp, stageColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (stage.status == "ACTIVE") {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                        color = stageColor
                                    )
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = stageColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stage.stageName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = stageColor
                                    )
                                )
                                Text(
                                    text = stage.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = com.example.ui.theme.SamTextPrimary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
    }
}
}
}



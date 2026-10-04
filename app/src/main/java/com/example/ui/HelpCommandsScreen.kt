package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.device.intent.ActionableIntent
import com.example.device.intent.IntentFunctionMapper
import com.example.device.intent.IntentType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.ui.theme.SamCyan
import com.example.ui.theme.SamCyanGlow
import com.example.ui.theme.SamDarkBg
import com.example.ui.theme.SamMagenta
import com.example.ui.theme.SamPurplePrimary
import com.example.ui.theme.SamPurpleSecondary
import com.example.ui.theme.SamSurface
import com.example.ui.theme.SamSurfaceBorder
import com.example.ui.theme.SamSurfaceElevated
import com.example.ui.theme.SamTextMuted
import com.example.ui.theme.SamTextPrimary
import com.example.ui.theme.SamTextSecondary

enum class CommandCategory(val title: String, val icon: ImageVector) {
    ALL("All", Icons.AutoMirrored.Filled.Help),
    CALLS("Calls & Contacts", Icons.Default.Call),
    APPS("Apps & Media", Icons.Default.AppShortcut),
    TIMERS("Timers & Alarms", Icons.Default.Timer),
    DEVICE("Device Actions", Icons.Default.FlashlightOn),
    AI_SEARCH("AI & Search", Icons.Default.Psychology),
    PERSONAS("Chat & Personas", Icons.Default.Language)
}

data class VoiceCommandItem(
    val id: String,
    val category: CommandCategory,
    val command: String,
    val alternateHindi: String? = null,
    val description: String,
    val icon: ImageVector,
    val isAction: Boolean = true
)

private val SAMPLE_VOICE_COMMANDS = listOf(
    // Calls & Contacts
    VoiceCommandItem(
        id = "call_mom",
        category = CommandCategory.CALLS,
        command = "Call Mom",
        alternateHindi = "Mummy ko call lagao",
        description = "Searches device contacts and directly dials Mom's phone number.",
        icon = Icons.Default.Call
    ),
    VoiceCommandItem(
        id = "call_rahul",
        category = CommandCategory.CALLS,
        command = "Call Rahul on mobile",
        alternateHindi = "Rahul ko phone mila do",
        description = "Finds contact named Rahul and opens phone dialer with number populated.",
        icon = Icons.Default.Call
    ),
    VoiceCommandItem(
        id = "search_contact",
        category = CommandCategory.CALLS,
        command = "Search contact Doctor Sharma",
        alternateHindi = "Doctor Sharma ka number dhundo",
        description = "Queries contacts list and presents phone details or direct dial.",
        icon = Icons.Default.Search
    ),
    VoiceCommandItem(
        id = "send_sms_rahul",
        category = CommandCategory.CALLS,
        command = "Send SMS to Rahul I am on my way",
        alternateHindi = "Rahul ko SMS bhejo main raste mein hoon",
        description = "Opens SMS composer with recipient and message pre-populated.",
        icon = Icons.AutoMirrored.Filled.Send
    ),
    VoiceCommandItem(
        id = "send_sms_number",
        category = CommandCategory.CALLS,
        command = "Send SMS to 9876543210 Hello Arushi",
        alternateHindi = "9876543210 ko SMS bhejo Hello",
        description = "Directly initiates SMS composer for phone number.",
        icon = Icons.AutoMirrored.Filled.Send
    ),

    // Timers & Alarms
    VoiceCommandItem(
        id = "timer_10m",
        category = CommandCategory.TIMERS,
        command = "Set a timer for 10 minutes",
        alternateHindi = "10 minute ka timer laga do",
        description = "Configures a 10-minute countdown timer on your device.",
        icon = Icons.Default.Timer
    ),
    VoiceCommandItem(
        id = "alarm_morning",
        category = CommandCategory.TIMERS,
        command = "Set an alarm for 7:00 AM tomorrow",
        alternateHindi = "Kal subah 7 baje ka alarm set karo",
        description = "Schedules a standard system alarm for 7:00 AM with label.",
        icon = Icons.Default.Alarm
    ),
    VoiceCommandItem(
        id = "schedule_event",
        category = CommandCategory.TIMERS,
        command = "Schedule a meeting with Team at 3 PM",
        alternateHindi = "Dopahar 3 baje Team Meeting calendar mein daal do",
        description = "Adds a calendar event with time, title, and reminder duration.",
        icon = Icons.Default.Alarm
    ),

    // Apps & Media
    VoiceCommandItem(
        id = "open_whatsapp",
        category = CommandCategory.APPS,
        command = "Open WhatsApp",
        alternateHindi = "WhatsApp khol do",
        description = "Launches WhatsApp messenger immediately.",
        icon = Icons.Default.AppShortcut
    ),
    VoiceCommandItem(
        id = "send_wa_msg",
        category = CommandCategory.APPS,
        command = "Send WhatsApp message to Mom: Main ghar aa raha hoon",
        alternateHindi = "WhatsApp par Mom ko message bhejo",
        description = "Pre-populates a WhatsApp chat with target recipient and message text.",
        icon = Icons.AutoMirrored.Filled.Send
    ),
    VoiceCommandItem(
        id = "open_yt",
        category = CommandCategory.APPS,
        command = "Open YouTube",
        alternateHindi = "YouTube chala do",
        description = "Launches YouTube application.",
        icon = Icons.Default.AppShortcut
    ),
    VoiceCommandItem(
        id = "play_music",
        category = CommandCategory.APPS,
        command = "Play Arijit Singh romantic songs on Spotify",
        alternateHindi = "Spotify par Arijit Singh ke gaane lagao",
        description = "Searches Spotify/YouTube Music and streams audio track.",
        icon = Icons.Default.MusicNote
    ),
    VoiceCommandItem(
        id = "open_insta",
        category = CommandCategory.APPS,
        command = "Open Instagram",
        alternateHindi = "Instagram open karo",
        description = "Opens Instagram social feed.",
        icon = Icons.Default.AppShortcut
    ),

    // Device Actions
    VoiceCommandItem(
        id = "torch_on",
        category = CommandCategory.DEVICE,
        command = "Turn on flashlight",
        alternateHindi = "Flashlight on kar do",
        description = "Toggles hardware torch/flashlight ON.",
        icon = Icons.Default.FlashlightOn
    ),
    VoiceCommandItem(
        id = "torch_off",
        category = CommandCategory.DEVICE,
        command = "Turn off flashlight",
        alternateHindi = "Torch band karo",
        description = "Toggles hardware torch/flashlight OFF.",
        icon = Icons.Default.FlashlightOn
    ),
    VoiceCommandItem(
        id = "volume_up",
        category = CommandCategory.DEVICE,
        command = "Increase system volume to 80%",
        alternateHindi = "Awaaz 80 percent kar do",
        description = "Adjusts device music and media volume directly.",
        icon = Icons.AutoMirrored.Filled.VolumeUp
    ),
    VoiceCommandItem(
        id = "battery_check",
        category = CommandCategory.DEVICE,
        command = "What is my battery level?",
        alternateHindi = "Battery kitni percent bachi hai?",
        description = "Checks battery percentage and charging status.",
        icon = Icons.Default.FlashlightOn
    ),
    VoiceCommandItem(
        id = "wifi_on",
        category = CommandCategory.DEVICE,
        command = "Turn on Wi-Fi",
        alternateHindi = "Wi-Fi on karo",
        description = "Opens system quick panel to turn Wi-Fi ON.",
        icon = Icons.Default.AppShortcut
    ),
    VoiceCommandItem(
        id = "wifi_off",
        category = CommandCategory.DEVICE,
        command = "Turn off Wi-Fi",
        alternateHindi = "Wi-Fi band karo",
        description = "Opens system quick panel to turn Wi-Fi OFF.",
        icon = Icons.Default.AppShortcut
    ),

    // AI & Search
    VoiceCommandItem(
        id = "deep_research",
        category = CommandCategory.AI_SEARCH,
        command = "What are the top news updates in tech today?",
        alternateHindi = "Aaj ki taaza tech khabrein batao",
        description = "Queries real-time web grounding via Gemini Search.",
        icon = Icons.Default.Search
    ),
    VoiceCommandItem(
        id = "quantum_explain",
        category = CommandCategory.AI_SEARCH,
        command = "Explain quantum computing in simple Hinglish",
        alternateHindi = "Quantum computing aasan bhasha mein samjhao",
        description = "Provides clear, conversational explanation in friendly Hinglish.",
        icon = Icons.Default.Psychology
    ),
    VoiceCommandItem(
        id = "write_email",
        category = CommandCategory.AI_SEARCH,
        command = "Write a formal sick leave email for 2 days",
        alternateHindi = "Office ke liye leave email likho",
        description = "Generates complete drafted professional text ready to copy.",
        icon = Icons.Default.Psychology
    ),
    VoiceCommandItem(
        id = "gen_image",
        category = CommandCategory.AI_SEARCH,
        command = "Generate an image of a neon cyberpunk city at night",
        alternateHindi = "Cyberpunk neon city ki photo banao",
        description = "Invokes AI image generation studio.",
        icon = Icons.Default.Videocam
    ),

    // Personas & Chat
    VoiceCommandItem(
        id = "switch_jarvis",
        category = CommandCategory.PERSONAS,
        command = "Switch to Jarvis persona",
        alternateHindi = "Jarvis mode chalu karo",
        description = "Transforms JARVIS into a sophisticated British AI butler.",
        icon = Icons.Default.Language
    ),
    VoiceCommandItem(
        id = "switch_buddy",
        category = CommandCategory.PERSONAS,
        command = "Switch to Buddy persona",
        alternateHindi = "Buddy mode lagao",
        description = "Transforms JARVIS into a friendly Hinglish college roommate.",
        icon = Icons.Default.Language
    ),
    VoiceCommandItem(
        id = "tell_joke",
        category = CommandCategory.PERSONAS,
        command = "Tell me a funny joke in Hindi",
        alternateHindi = "Ek mast chutkula sunao",
        description = "Shares witty humor and amusing punchlines.",
        icon = Icons.Default.Psychology
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HelpCommandsScreen(
    onBack: () -> Unit,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CommandCategory.ALL) }

    val filteredCommands = remember(searchQuery, selectedCategory) {
        SAMPLE_VOICE_COMMANDS.filter { item ->
            val matchesCategory = (selectedCategory == CommandCategory.ALL) || (item.category == selectedCategory)
            val matchesQuery = searchQuery.isBlank() ||
                    item.command.contains(searchQuery, ignoreCase = true) ||
                    (item.alternateHindi?.contains(searchQuery, ignoreCase = true) == true) ||
                    item.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SamDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Voice Commands & Help",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = SamTextPrimary
                            )
                        )
                        Text(
                            text = "Examples you can speak or type to JARVIS",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SamPurpleSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SamSurfaceElevated)
                            .testTag("help_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SamCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SamDarkBg
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Interactive Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search voice commands (e.g. Call, Timer, YouTube)...",
                            color = SamTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = SamCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = SamTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SamSurfaceElevated,
                        unfocusedContainerColor = SamSurface,
                        focusedBorderColor = SamCyan,
                        unfocusedBorderColor = SamSurfaceBorder,
                        focusedTextColor = SamTextPrimary,
                        unfocusedTextColor = SamTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("commands_search_field")
                )
            }

            // 2. Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(CommandCategory.values()) { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    text = cat.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SamSurface,
                                labelColor = SamTextSecondary,
                                iconColor = SamPurpleSecondary,
                                selectedContainerColor = SamCyan.copy(alpha = 0.2f),
                                selectedLabelColor = SamCyan,
                                selectedLeadingIconColor = SamCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = SamSurfaceBorder,
                                selectedBorderColor = SamCyan,
                                borderWidth = 1.dp
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // 3. Live Intent Function Mapping Tester
            item {
                IntentFunctionMappingTesterCard(
                    onExecuteCommand = onExecuteCommand
                )
            }

            // 4. Pro Voice Tips Card
            item {
                Surface(
                    color = SamSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(listOf(SamCyan.copy(alpha = 0.4f), SamMagenta.copy(alpha = 0.4f)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(SamCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = SamCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "💡 Voice Assistant Pro-Tips",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SamCyan
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "• Speak naturally in Hindi, English, or Hinglish.\n" +
                                    "• Say direct action words like 'Open WhatsApp', 'Call Mom', or 'Set a timer for 10 minutes'.\n" +
                                    "• Tap any command card below to try it immediately with JARVIS!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SamTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }

            // 4. Section Count Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AVAILABLE COMMANDS (${filteredCommands.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SamTextMuted,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            // 5. Command Cards
            items(filteredCommands, key = { it.id }) { item ->
                CommandCard(
                    item = item,
                    onExecute = { onExecuteCommand(item.command) },
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(item.command))
                        Toast.makeText(context, "Copied: \"${item.command}\"", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            if (filteredCommands.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = SamTextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No matching commands found",
                                style = MaterialTheme.typography.bodyMedium.copy(color = SamTextMuted)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try searching for 'call', 'timer', 'alarm', or 'youtube'",
                                style = MaterialTheme.typography.bodySmall.copy(color = SamPurpleSecondary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandCard(
    item: VoiceCommandItem,
    onExecute: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SamSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("command_card_${item.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SamCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = SamCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "\"${item.command}\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = SamTextPrimary,
                                fontSize = 14.sp
                            )
                        )
                        item.alternateHindi?.let { hindi ->
                            Text(
                                text = "Hindi: \"$hindi\"",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = SamCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                // Category Badge
                Surface(
                    color = SamSurfaceElevated,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder)
                ) {
                    Text(
                        text = item.category.title.split(" ").first(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SamPurpleSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = SamTextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SamSurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy command",
                        tint = SamTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = SamCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SamCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.clickable { onExecute() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = SamCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Try Now",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SamCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Live interactive tester for Natural Language to Actionable Android Intent translation.
 */
@Composable
fun IntentFunctionMappingTesterCard(
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var testInput by remember { mutableStateOf("call Mom") }
    var mappedIntent by remember(testInput) {
        mutableStateOf(IntentFunctionMapper.mapNaturalLanguageToIntent(testInput, context))
    }

    val sampleQueries = listOf(
        "call Mom",
        "set alarm for 7:30 AM",
        "send SMS to Rahul hello",
        "navigate to Airport",
        "play Believer on YouTube",
        "turn on flashlight"
    )

    Surface(
        color = SamSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SamCyan.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("intent_mapping_tester_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "⚡",
                    fontSize = 18.sp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Intent Function Mapping Engine",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SamTextPrimary,
                            fontSize = 14.sp
                        )
                    )
                    Text(
                        text = "Translates natural language into actionable Android Intents",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SamTextMuted,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick chip suggestions
            Text(
                text = "Tap to test natural query:",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = SamCyan,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.5.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(sampleQueries) { query ->
                    val isSelected = testInput.equals(query, ignoreCase = true)
                    Surface(
                        color = if (isSelected) SamCyan.copy(alpha = 0.25f) else SamSurface,
                        border = BorderStroke(1.dp, if (isSelected) SamCyan else SamSurfaceBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clickable {
                                testInput = query
                                mappedIntent = IntentFunctionMapper.mapNaturalLanguageToIntent(query, context)
                            }
                            .testTag("test_intent_chip_${query.replace(" ", "_")}")
                    ) {
                        Text(
                            text = query,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) SamCyan else SamTextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Input field
            OutlinedTextField(
                value = testInput,
                onValueChange = {
                    testInput = it
                    mappedIntent = IntentFunctionMapper.mapNaturalLanguageToIntent(it, context)
                },
                placeholder = { Text("Type intent e.g. 'call Mom' or 'set alarm'...", fontSize = 12.sp, color = SamTextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SamSurface,
                    unfocusedContainerColor = SamSurface,
                    focusedBorderColor = SamCyan,
                    unfocusedBorderColor = SamSurfaceBorder,
                    focusedTextColor = SamTextPrimary,
                    unfocusedTextColor = SamTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("intent_tester_input_field")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Translation Result Display
            if (mappedIntent != null) {
                val act = mappedIntent!!
                Surface(
                    color = SamDarkBg.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SamPurplePrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = act.type.icon, fontSize = 16.sp)
                                Text(
                                    text = act.type.displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = SamCyan
                                    )
                                )
                            }
                            Surface(
                                color = SamPurplePrimary.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = act.actionName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SamPurpleSecondary
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Target Android Action: ${act.androidIntent?.action ?: "System Hardware Service"}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = SamTextPrimary
                            )
                        )

                        if (act.androidIntent?.data != null) {
                            Text(
                                text = "URI Data: ${act.androidIntent.data}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.5.sp,
                                    color = SamCyanGlow
                                )
                            )
                        }

                        if (act.parameters.isNotEmpty()) {
                            Text(
                                text = "Extracted Parameters: ${act.parameters}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.5.sp,
                                    color = SamTextMuted
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val result = act.execute(context)
                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SamCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("btn_execute_mapped_intent")
                        ) {
                            Text(
                                text = "🚀 Execute Actionable Intent",
                                color = SamDarkBg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                Surface(
                    color = SamSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 Type or tap a supported natural language intent above to see its actionable Android Intent translation.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SamTextMuted,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

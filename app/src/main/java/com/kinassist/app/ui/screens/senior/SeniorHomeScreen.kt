package com.kinassist.app.ui.screens.senior

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kinassist.app.core.accessibility.VoiceGuideManager
import com.kinassist.app.core.telemetry.TelemetryManager
import com.kinassist.app.core.webrtc.TelemetryData
import com.kinassist.app.ui.theme.*

@Composable
fun SeniorHomeScreen(
    seniorName: String = "Senior",
    pairedHelperName: String = "Caregiver",
    helperPhone: String = "",
    connectionStatusText: String = "Direct Link Active",
    telemetryData: TelemetryData? = null,
    onTriggerSos: (requestType: String) -> Unit,
    onStartScreenShare: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onSwitchToCaregiverView: () -> Unit = {}
) {
    val context = LocalContext.current
    val telemetryManager = remember { TelemetryManager(context) }
    val voiceGuide = remember { VoiceGuideManager(context) }

    var isCallingModalOpen by remember { mutableStateOf(false) }
    var callingRequestLabel by remember { mutableStateOf("Immediate Help") }
    var isVoiceHelpActive by remember { mutableStateOf(true) }

    // Action dialog for quick requests
    var activeQuickAction by remember { mutableStateOf<QuickActionType?>(null) }
    var volumeNotificationMessage by remember { mutableStateOf<String?>(null) }

    // Real system telemetry (live state)
    var liveTelemetry by remember { mutableStateOf(telemetryData ?: telemetryManager.gatherTelemetry()) }

    LaunchedEffect(Unit) {
        liveTelemetry = telemetryManager.gatherTelemetry()
        if (isVoiceHelpActive) {
            voiceGuide.speak("Welcome to KinAssist. Tap the big red button anytime to ask for help.")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceGuide.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. Top Brand Identity Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.kinassist.app.R.drawable.kinassist_logo_mark),
                                contentDescription = "KinAssist Brand Icon",
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "KINASSIST",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "1-Tap Family Remote Rescue",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings & Roles",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. Header Bar: Profile Card & Mode Switch
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Parent Profile",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Column {
                            val greetingText = if (seniorName.isNotBlank() && seniorName != "Senior") "Welcome, $seniorName" else "Welcome"
                            Text(
                                text = greetingText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(8.dp),
                                    shape = CircleShape,
                                    color = CareGreen
                                ) {}
                                val helperLabel = if (pairedHelperName.isNotBlank() && pairedHelperName != "Caregiver") {
                                    "Connected with $pairedHelperName"
                                } else {
                                    "Connected with Caregiver"
                                }
                                Text(
                                    text = helperLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Switch Mode Badge Button
                    Surface(
                        onClick = onSwitchToCaregiverView,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Helper",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Connection Status & Switch Role Link
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CareGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = connectionStatusText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        TextButton(
                            onClick = onSwitchToCaregiverView,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Helper Mode ›",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Volume notification banner if volume was boosted
            volumeNotificationMessage?.let { msg ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = CareGreenLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CareGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = CareGreen)
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CareGreenDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 2. HERO: Tactile Clean 1-Tap SOS Button
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .size(190.dp)
                            .shadow(8.dp, CircleShape, spotColor = SosRed.copy(alpha = 0.25f))
                            .clickable {
                                if (isVoiceHelpActive) {
                                    val promptName = if (pairedHelperName.isNotBlank() && pairedHelperName != "Caregiver") pairedHelperName else "caregiver"
                                    voiceGuide.speak("Calling $promptName for immediate help.")
                                }
                                callingRequestLabel = "Direct 1-Tap Help"
                                isCallingModalOpen = true
                                onTriggerSos("SOS_BUTTON")
                            },
                        shape = CircleShape,
                        color = SosRed,
                        border = androidx.compose.foundation.BorderStroke(4.dp, Color.White.copy(alpha = 0.85f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = "Ask for Help",
                                tint = Color.White,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Ask for Help",
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = 28.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val subLabel = if (pairedHelperName.isNotBlank() && pairedHelperName != "Caregiver") {
                                "Call $pairedHelperName"
                            } else {
                                "1-Tap Direct Call"
                            }
                            Text(
                                text = subLabel,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secure",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            val ringNotice = if (pairedHelperName.isNotBlank() && pairedHelperName != "Caregiver") {
                                "Direct & private • $pairedHelperName's phone will ring"
                            } else {
                                "Direct & private • Alerts your caregiver instantly"
                            }
                            Text(
                                text = ringNotice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Functional Quick Requests with Real Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "QUICK ASSISTANCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    CleanQuickRequestCard(
                        icon = Icons.Default.Mail,
                        title = "Check New Messages",
                        subtitle = "Open SMS, OTPs or call helper to read",
                        containerColor = PrimaryBlue.copy(alpha = 0.1f),
                        iconTint = PrimaryBlue,
                        onClick = {
                            if (isVoiceHelpActive) {
                                voiceGuide.speak("Messages assistance selected.")
                            }
                            activeQuickAction = QuickActionType.MESSAGES
                        }
                    )

                    CleanQuickRequestCard(
                        icon = Icons.Default.VolumeUp,
                        title = "Can't Hear Phone Ring",
                        subtitle = "Boost ringer to 100% or test ringtone",
                        containerColor = SosRed.copy(alpha = 0.1f),
                        iconTint = SosRed,
                        onClick = {
                            if (isVoiceHelpActive) {
                                voiceGuide.speak("Ringer and sound assistance selected.")
                            }
                            activeQuickAction = QuickActionType.RINGER
                        }
                    )

                    CleanQuickRequestCard(
                        icon = Icons.Default.RestartAlt,
                        title = "Screen Seems Stuck",
                        subtitle = "Return to Home or request remote unstick",
                        containerColor = CareGreen.copy(alpha = 0.1f),
                        iconTint = CareGreen,
                        onClick = {
                            if (isVoiceHelpActive) {
                                voiceGuide.speak("Screen recovery selected.")
                            }
                            activeQuickAction = QuickActionType.SCREEN_STUCK
                        }
                    )
                }
            }

            // 4. Real Phone Status Telemetry (Queried from Android OS)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE PHONE STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (liveTelemetry.isCharging) "Charging" else "Battery Powered",
                                style = MaterialTheme.typography.labelSmall,
                                color = CareGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CleanTelemetryBox(
                                icon = if (liveTelemetry.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                                value = "${liveTelemetry.batteryLevel}%",
                                label = if (liveTelemetry.isCharging) "Charging" else "Battery",
                                tint = if (liveTelemetry.batteryLevel < 20) SosRed else CareGreen,
                                modifier = Modifier.weight(1f)
                            )
                            CleanTelemetryBox(
                                icon = Icons.Default.VolumeUp,
                                value = liveTelemetry.ringerMode,
                                label = "Ringer Mode",
                                tint = PrimaryBlue,
                                modifier = Modifier.weight(1f)
                            )
                            CleanTelemetryBox(
                                icon = Icons.Default.Wifi,
                                value = liveTelemetry.networkType,
                                label = "Network",
                                tint = if (liveTelemetry.networkType == "Offline") SosRed else CareGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 5. Spoken Audio Announcement Toggle (Functional Text-To-Speech)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = "Voice Guide",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Voice Announcements",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Reads buttons and incoming helper status aloud",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isVoiceHelpActive,
                            onCheckedChange = { checked ->
                                isVoiceHelpActive = checked
                                if (checked) {
                                    voiceGuide.speak("Voice announcements turned on.")
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CareGreen
                            )
                        )
                    }
                }
            }

            // 6. Security Note
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Shield",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KinAssist Safe Shield Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Modal for Quick Requests (Fully Working Actions!)
        activeQuickAction?.let { action ->
            Dialog(onDismissRequest = { activeQuickAction = null }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (action) {
                            QuickActionType.MESSAGES -> {
                                Text(
                                    text = "Messages Assistance",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Choose how you'd like to check your messages:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        activeQuickAction = null
                                        try {
                                            val smsIntent = Intent(Intent.ACTION_MAIN).apply {
                                                addCategory(Intent.CATEGORY_APP_MESSAGING)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(smsIntent)
                                        } catch (_: Exception) {
                                            val viewSms = Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))
                                            context.startActivity(viewSms)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Mail, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open Messages App")
                                }

                                OutlinedButton(
                                    onClick = {
                                        activeQuickAction = null
                                        callingRequestLabel = "Help Reading Messages"
                                        isCallingModalOpen = true
                                        onTriggerSos("CHECK_MESSAGES")
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PhoneInTalk, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val readBtnLabel = if (pairedHelperName.isNotBlank() && pairedHelperName != "Caregiver") {
                                        "Ask $pairedHelperName to Read"
                                    } else {
                                        "Ask Caregiver to Read"
                                    }
                                    Text(readBtnLabel)
                                }
                            }

                            QuickActionType.RINGER -> {
                                Text(
                                    text = "Ringer & Sound Assistance",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Fix volume directly or ring test sound:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        telemetryManager.setMaxRingerVolume()
                                        telemetryManager.playTestChime()
                                        liveTelemetry = telemetryManager.gatherTelemetry()
                                        volumeNotificationMessage = "Ringtone set to 100% loud & test played"
                                        activeQuickAction = null
                                        if (isVoiceHelpActive) {
                                            voiceGuide.speak("Volume boosted to maximum. Playing test chime.")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CareGreen)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Set Ringer to Max (100%)")
                                }

                                OutlinedButton(
                                    onClick = {
                                        activeQuickAction = null
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open Sound Settings")
                                }

                                OutlinedButton(
                                    onClick = {
                                        activeQuickAction = null
                                        callingRequestLabel = "Volume / Ringer Problem"
                                        isCallingModalOpen = true
                                        onTriggerSos("VOLUME_FIX")
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PhoneInTalk, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val soundBtnLabel = if (pairedHelperName.isNotBlank() && pairedHelperName != "Caregiver") {
                                        "Call $pairedHelperName to Fix"
                                    } else {
                                        "Call Caregiver to Fix"
                                    }
                                    Text(soundBtnLabel)
                                }
                            }

                            QuickActionType.SCREEN_STUCK -> {
                                Text(
                                    text = "Screen Seems Stuck",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Try going back to home screen or ask helper to unfreeze:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        activeQuickAction = null
                                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                            addCategory(Intent.CATEGORY_HOME)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(homeIntent)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Home, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Go to Phone Home Screen")
                                }

                                OutlinedButton(
                                    onClick = {
                                        activeQuickAction = null
                                        callingRequestLabel = "Screen Stuck / Unresponsive"
                                        isCallingModalOpen = true
                                        onTriggerSos("SCREEN_STUCK")
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PhoneInTalk, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val stuckBtnLabel = if (pairedHelperName.isNotBlank() && pairedHelperName != "Caregiver") {
                                        "Ask $pairedHelperName to Unstick"
                                    } else {
                                        "Ask Caregiver to Unstick"
                                    }
                                    Text(stuckBtnLabel)
                                }
                            }
                        }

                        TextButton(
                            onClick = { activeQuickAction = null },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }

        // Calling Modal with Direct Fallback
        if (isCallingModalOpen) {
            Dialog(onDismissRequest = { isCallingModalOpen = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(76.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhoneInTalk,
                                    contentDescription = "Calling",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val alertingTarget = if (pairedHelperName.isNotBlank() && pairedHelperName != "Caregiver") {
                                "Alerting $pairedHelperName..."
                            } else {
                                "Alerting Caregiver..."
                            }
                            Text(
                                text = alertingTarget,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reason: $callingRequestLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Connecting screen link and speakerphone...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Direct Screen Share button
                        Button(
                            onClick = {
                                isCallingModalOpen = false
                                onStartScreenShare()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.ScreenShare, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Screen Now", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // Direct GSM Phone Call Fallback button
                        if (helperPhone.isNotBlank()) {
                            Button(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$helperPhone"))
                                    context.startActivity(dialIntent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CareGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Direct Phone Call ($helperPhone)")
                            }
                        }

                        Button(
                            onClick = { isCallingModalOpen = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancel",
                                tint = SosRed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cancel Alert",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class QuickActionType {
    MESSAGES,
    RINGER,
    SCREEN_STUCK
}

@Composable
fun CleanQuickRequestCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Action",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun CleanTelemetryBox(
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

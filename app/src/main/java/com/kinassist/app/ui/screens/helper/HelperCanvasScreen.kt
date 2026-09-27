package com.kinassist.app.ui.screens.helper

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinassist.app.core.webrtc.PointerEvent
import com.kinassist.app.core.webrtc.PointerType
import com.kinassist.app.core.webrtc.RescueCommand
import com.kinassist.app.core.webrtc.TelemetryData
import com.kinassist.app.ui.components.KinAssistVideoView
import com.kinassist.app.ui.theme.*
import org.webrtc.VideoTrack

@Composable
fun HelperCanvasScreen(
    assistedPersonName: String = "Family Member",
    remoteVideoTrack: VideoTrack? = null,
    isPrivacyBlackout: Boolean = false,
    telemetryData: TelemetryData? = null,
    onSendPointerEvent: (PointerEvent) -> Unit,
    onSendRescueCommand: (RescueCommand) -> Unit = {},
    onRequestScreenShare: () -> Unit = {},
    onRingLoudly: () -> Unit = {},
    onToggleMute: (Boolean) -> Unit = {},
    onDisconnect: () -> Unit,
    onNavigateBack: () -> Unit = onDisconnect
) {
    var selectedTool by remember { mutableStateOf(PointerType.ARROW_PULSE) }
    var isMuted by remember { mutableStateOf(false) }
    var currentPointerCoord by remember { mutableStateOf<Offset?>(Offset(0.5f, 0.45f)) }
    var activeStepCount by remember { mutableIntStateOf(1) }
    val stepHistory = remember { mutableStateListOf<Pair<Offset, Int>>() }
    var actionNotification by remember { mutableStateOf<String?>(null) }

    BackHandler {
        onNavigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 44.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Header Bar: Status & Controls
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Box {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.size(38.dp)
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = com.kinassist.app.R.drawable.kinassist_logo_mark),
                                    contentDescription = "KinAssist Logo",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(5.dp)
                                )
                            }
                            Surface(
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.BottomEnd),
                                shape = CircleShape,
                                color = CareGreen,
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.background)
                            ) {}
                        }
                        Column {
                            Text(
                                text = "Assisting $assistedPersonName",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "KinAssist P2P • ${telemetryData?.latencyMs ?: 28}ms Latency",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (telemetryData?.isCharging == true) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                                    contentDescription = "Battery",
                                    tint = CareGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${telemetryData?.batteryLevel ?: 100}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                        Button(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SosRedLight,
                                contentColor = SosRedDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "End Session",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Action Notification Banner
            actionNotification?.let { msg ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = CareGreenLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CareGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = msg,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CareGreenDark
                            )
                            IconButton(
                                onClick = { actionNotification = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = CareGreenDark, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // 2. Audio Intercom Strip
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Intercom",
                                tint = CareGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "$assistedPersonName is on speakerphone",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (isMuted) "Your microphone is muted" else "Voice audio active (Echo cancelled)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                onToggleMute(isMuted)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) SosRedLight else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute Toggle",
                                tint = if (isMuted) SosRed else MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 3. Screen Viewport / Live Interactive Touch Surface
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier
                            .width(310.dp)
                            .aspectRatio(9f / 17f)
                            .shadow(6.dp, RoundedCornerShape(26.dp))
                            .clip(RoundedCornerShape(26.dp))
                            .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(26.dp))
                            .pointerInput(selectedTool) {
                                detectTapGestures { offset ->
                                    val xRatio = offset.x / size.width
                                    val yRatio = offset.y / size.height
                                    currentPointerCoord = Offset(xRatio, yRatio)

                                    val event = PointerEvent(
                                        type = selectedTool,
                                        xRatio = xRatio,
                                        yRatio = yRatio,
                                        stepNumber = activeStepCount
                                    )
                                    onSendPointerEvent(event)
                                    actionNotification = "Pointer sent to $assistedPersonName's screen"

                                    if (selectedTool == PointerType.STEP_BADGE) {
                                        stepHistory.add(Offset(xRatio, yRatio) to activeStepCount)
                                        activeStepCount++
                                    }
                                }
                            },
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (isPrivacyBlackout) {
                                // Privacy Shield Activated
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(56.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Security,
                                                    contentDescription = "Shield",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(30.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Screen Paused for Privacy",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "$assistedPersonName opened a sensitive screen (PIN or Banking). Video paused.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else if (remoteVideoTrack != null) {
                                KinAssistVideoView(
                                    videoTrack = remoteVideoTrack,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // Real Interactive Canvas when screen stream is awaiting senior authorization
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Header
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Remote Touch Surface",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Active Link",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = CareGreen
                                            )
                                        }

                                        // Center guide
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(50.dp),
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.TouchApp,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Tap Anywhere to Guide",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = "Touches here project real pointing arrows and step numbers onto $assistedPersonName's phone overlay.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                            Button(
                                                onClick = {
                                                    onRequestScreenShare()
                                                    actionNotification = "Screen share request sent to $assistedPersonName"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.ScreenShare, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Request Screen Mirror", fontSize = 12.sp)
                                            }
                                        }

                                        // Bottom note
                                        Text(
                                            text = "Encrypted Family Assist Channel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Pointer Pin Overlay on Helper viewport
                            currentPointerCoord?.let { coord ->
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val posX = maxWidth * coord.x
                                    val posY = maxHeight * coord.y

                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val offset = Offset(posX.toPx(), posY.toPx())
                                        drawCircle(color = Color.White, radius = 14f, center = offset)
                                        drawCircle(color = PrimaryBlue, radius = 10f, center = offset)
                                    }

                                    Box(modifier = Modifier.offset(x = posX - 14.dp, y = posY - 36.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.NearMe,
                                            contentDescription = "Live Pointer",
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            // Step Badges
                            for ((stepOffset, stepNum) in stepHistory) {
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val posX = maxWidth * stepOffset.x
                                    val posY = maxHeight * stepOffset.y

                                    Surface(
                                        modifier = Modifier
                                            .offset(x = posX - 12.dp, y = posY - 12.dp)
                                            .size(24.dp),
                                        shape = CircleShape,
                                        color = PrimaryBlue,
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$stepNum",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "Tap on screen to project live pointer to $assistedPersonName's phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // 4. Guidance Toolset Dock
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GUIDANCE TOOLS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (stepHistory.isNotEmpty()) {
                                TextButton(
                                    onClick = { stepHistory.clear() },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text("Clear Steps", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CleanToolButton(
                                icon = Icons.Default.NearMe,
                                label = "Pointer",
                                isSelected = selectedTool == PointerType.ARROW_PULSE,
                                onClick = { selectedTool = PointerType.ARROW_PULSE },
                                modifier = Modifier.weight(1f)
                            )
                            CleanToolButton(
                                icon = Icons.Default.PinDrop,
                                label = "Step #$activeStepCount",
                                isSelected = selectedTool == PointerType.STEP_BADGE,
                                onClick = { selectedTool = PointerType.STEP_BADGE },
                                modifier = Modifier.weight(1f)
                            )
                            CleanToolButton(
                                icon = Icons.Default.Draw,
                                label = "Circle",
                                isSelected = selectedTool == PointerType.DOODLE_CIRCLE,
                                onClick = { selectedTool = PointerType.DOODLE_CIRCLE },
                                modifier = Modifier.weight(1f)
                            )
                            CleanToolButton(
                                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                label = if (isMuted) "Muted" else "Voice ON",
                                isSelected = !isMuted,
                                onClick = {
                                    isMuted = !isMuted
                                    onToggleMute(isMuted)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 5. Remote Rescue Triggers (Accessibility Service Navigation)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "REMOTE RESCUE TRIGGERS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CleanRescueButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                label = "Press Back",
                                onClick = {
                                    onSendRescueCommand(RescueCommand.GLOBAL_BACK)
                                    onSendPointerEvent(
                                        PointerEvent(type = PointerType.RESCUE_ACTION, rescueCommand = RescueCommand.GLOBAL_BACK)
                                    )
                                    actionNotification = "Sent 'Back' command to $assistedPersonName's phone"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            CleanRescueButton(
                                icon = Icons.Default.Home,
                                label = "Go Home",
                                onClick = {
                                    onSendRescueCommand(RescueCommand.GLOBAL_HOME)
                                    onSendPointerEvent(
                                        PointerEvent(type = PointerType.RESCUE_ACTION, rescueCommand = RescueCommand.GLOBAL_HOME)
                                    )
                                    actionNotification = "Sent 'Go Home' command to $assistedPersonName's phone"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            CleanRescueButton(
                                icon = Icons.Default.Notifications,
                                label = "Notifications",
                                onClick = {
                                    onSendRescueCommand(RescueCommand.GLOBAL_NOTIFICATIONS)
                                    onSendPointerEvent(
                                        PointerEvent(type = PointerType.RESCUE_ACTION, rescueCommand = RescueCommand.GLOBAL_NOTIFICATIONS)
                                    )
                                    actionNotification = "Sent 'Notifications' command to $assistedPersonName's phone"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Additional Rescue: Ring Device Loudly
                        OutlinedButton(
                            onClick = {
                                onRingLoudly()
                                actionNotification = "Sent loud ring command to locate phone"
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ring $assistedPersonName's Phone Loudly", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CleanToolButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CleanRescueButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = PrimaryBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

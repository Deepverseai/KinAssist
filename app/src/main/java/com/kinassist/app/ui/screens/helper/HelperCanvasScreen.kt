package com.kinassist.app.ui.screens.helper

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinassist.app.core.webrtc.PointerEvent
import com.kinassist.app.core.webrtc.PointerType
import com.kinassist.app.core.webrtc.RescueCommand
import com.kinassist.app.ui.theme.*

@Composable
fun HelperCanvasScreen(
    onSendPointerEvent: (PointerEvent) -> Unit,
    onDisconnect: () -> Unit
) {
    var selectedTool by remember { mutableStateOf(PointerType.ARROW_PULSE) }
    var isMuted by remember { mutableStateOf(false) }
    var currentPointerCoord by remember { mutableStateOf<Offset?>(Offset(0.5f, 0.45f)) }
    var activeStepCount by remember { mutableIntStateOf(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCanvas)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 40.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Header Bar: Live Connection & Remote Status
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(EmeraldTertiary, CircleShape)
                        )
                        Column {
                            Text(
                                text = "Assisting Mom",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextOnSurfacePrimary
                            )
                            Text(
                                text = "42ms Latency • 1080p 30fps",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextOnSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Battery Pill
                        Row(
                            modifier = Modifier
                                .background(SurfaceContainerHigh, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = "Battery",
                                tint = EmeraldTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(text = "84%", fontSize = 12.sp, color = TextOnSurfacePrimary)
                        }

                        // End Session Button
                        Button(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.buttonColors(containerColor = TerracottaSOS.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = "Disconnect", color = TerracottaSOS, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Live Intercom Audio Strip
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainer)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Waveform",
                            tint = EmeraldTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Mom is listening",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextOnSurfacePrimary
                            )
                            Text(
                                text = "Opus 48kHz AEC active",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextOnSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceContainerHigh, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = if (isMuted) TerracottaSOS else TextOnSurfacePrimary
                        )
                    }
                }
            }

            // 3. Screen Mirror Viewport with Touch Interactive Canvas
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(320.dp)
                            .aspectRatio(9f / 18.5f)
                            .clip(RoundedCornerShape(32.dp))
                            .background(SurfaceContainerLowest)
                            .border(3.dp, OutlineBorder, RoundedCornerShape(32.dp))
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
                                    if (selectedTool == PointerType.STEP_BADGE) {
                                        activeStepCount++
                                    }
                                }
                            }
                    ) {
                        // Simulated Mirror Content (Settings screen with Wi-Fi / DND)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Status bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "09:41", fontSize = 11.sp, color = TextOnSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(imageVector = Icons.Default.Wifi, contentDescription = null, tint = TextOnSurfaceVariant, modifier = Modifier.size(12.dp))
                                    Icon(imageVector = Icons.Default.BatteryFull, contentDescription = null, tint = TextOnSurfaceVariant, modifier = Modifier.size(12.dp))
                                }
                            }

                            Text(
                                text = "Settings",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextOnSurfacePrimary
                            )

                            // Item 1
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceContainer)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(imageVector = Icons.Default.Wifi, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                    Text(text = "Wi-Fi & Internet", fontSize = 13.sp, color = TextOnSurfacePrimary)
                                }
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextOnSurfaceVariant, modifier = Modifier.size(16.dp))
                            }

                            // Item 2: Target Button Mom is stuck on
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceContainerHigh)
                                    .border(1.5.dp, EmeraldTertiary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(imageVector = Icons.Default.DoNotDisturb, contentDescription = null, tint = TerracottaSOS, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text(text = "Do Not Disturb", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextOnSurfacePrimary)
                                        Text(text = "Calls are muted", fontSize = 11.sp, color = TerracottaSOS)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(EmeraldTertiary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "ON", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }

                        // Real-time Active Pointer Marker on Canvas
                        currentPointerCoord?.let { coord ->
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val posX = maxWidth * coord.x
                                val posY = maxHeight * coord.y

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val offset = Offset(posX.toPx(), posY.toPx())
                                    drawCircle(
                                        color = EmeraldPulse.copy(alpha = 0.4f),
                                        radius = 28f,
                                        center = offset,
                                        style = Stroke(width = 3f)
                                    )
                                    drawCircle(
                                        color = EmeraldPulse,
                                        radius = 8f,
                                        center = offset
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .offset(x = posX - 16.dp, y = posY - 40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NearMe,
                                        contentDescription = "Live Pointer",
                                        tint = EmeraldPulse,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Tap on screen to guide Mom's attention in real-time",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextOnSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // 4. Interactive Guidance Tool Dock
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "GUIDANCE TOOLSET",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextOnSurfaceVariant
                            )
                            Text(
                                text = "Mode: ${selectedTool.name.replace('_', ' ')}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GuidanceToolButton(
                                icon = Icons.Default.NearMe,
                                label = "Pointer",
                                isSelected = selectedTool == PointerType.ARROW_PULSE,
                                onClick = { selectedTool = PointerType.ARROW_PULSE },
                                modifier = Modifier.weight(1f)
                            )
                            GuidanceToolButton(
                                icon = Icons.Default.Draw,
                                label = "Doodle",
                                isSelected = selectedTool == PointerType.DOODLE_CIRCLE,
                                onClick = { selectedTool = PointerType.DOODLE_CIRCLE },
                                modifier = Modifier.weight(1f)
                            )
                            GuidanceToolButton(
                                icon = Icons.Default.PinDrop,
                                label = "Step #$activeStepCount",
                                isSelected = selectedTool == PointerType.STEP_BADGE,
                                onClick = { selectedTool = PointerType.STEP_BADGE },
                                modifier = Modifier.weight(1f)
                            )
                            GuidanceToolButton(
                                icon = Icons.Default.RecordVoiceOver,
                                label = "Voice ON",
                                isSelected = !isMuted,
                                onClick = { isMuted = !isMuted },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 5. Remote Assisted Rescue Triggers
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "REMOTE RESCUE TRIGGERS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextOnSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RescueActionButton(
                                icon = Icons.Default.ArrowBack,
                                label = "Press Back",
                                onClick = {
                                    onSendPointerEvent(
                                        PointerEvent(type = PointerType.RESCUE_ACTION, rescueCommand = RescueCommand.GLOBAL_BACK)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            RescueActionButton(
                                icon = Icons.Default.Home,
                                label = "Go Home",
                                onClick = {
                                    onSendPointerEvent(
                                        PointerEvent(type = PointerType.RESCUE_ACTION, rescueCommand = RescueCommand.GLOBAL_HOME)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            RescueActionButton(
                                icon = Icons.Default.Notifications,
                                label = "Notifications",
                                onClick = {
                                    onSendPointerEvent(
                                        PointerEvent(type = PointerType.RESCUE_ACTION, rescueCommand = RescueCommand.GLOBAL_NOTIFICATIONS)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuidanceToolButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) GoldPrimary else SurfaceContainerHigh)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) DeepCanvas else TextOnSurfacePrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) DeepCanvas else TextOnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RescueActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .border(1.dp, OutlineBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = GoldPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextOnSurfacePrimary
        )
    }
}

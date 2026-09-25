package com.kinassist.app.ui.screens.senior

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kinassist.app.ui.theme.*

@Composable
fun SeniorHomeScreen(
    onTriggerSos: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    var isCallingModalOpen by remember { mutableStateOf(false) }
    var isVoiceHelpActive by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCanvas)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 44.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Header Bar: Profile Card & Quick Settings
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerHigh)
                                .border(1.5.dp, GoldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Parent Profile",
                                tint = GoldPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Namaste, Mom",
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextOnSurfacePrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(EmeraldTertiary, CircleShape)
                                )
                                Text(
                                    text = "Paired with Rahul (Son)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextOnSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(44.dp)
                            .background(SurfaceContainerHigh, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextOnSurfaceVariant
                        )
                    }
                }
            }

            // 2. HERO: Massive Tactile 1-Tap SOS Button with Pulsing Ring
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.08f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1400, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(240.dp)
                    ) {
                        // Ambient Outer Pulse Glow
                        Box(
                            modifier = Modifier
                                .size(230.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.15f))
                        )

                        // Main Big Tactile Button
                        Box(
                            modifier = Modifier
                                .size(190.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(GoldPrimary, GoldContainer, SurfaceContainerHigh)
                                    )
                                )
                                .border(3.dp, GoldPrimary, CircleShape)
                                .clickable {
                                    isCallingModalOpen = true
                                    onTriggerSos()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = "Touch App",
                                    tint = DeepCanvas,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ask Rahul\nfor Help",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepCanvas,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 24.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "1-Tap Screen Link",
                                    fontSize = 12.sp,
                                    color = DeepCanvas.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Direct Reassurance Pill
                    Row(
                        modifier = Modifier
                            .background(SurfaceContainerHigh, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Secure",
                            tint = EmeraldTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Direct live share • No passwords required",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextOnSurfacePrimary
                        )
                    }
                }
            }

            // 3. Common Quick Requests
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "COMMON QUICK REQUESTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextOnSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    QuickRequestCard(
                        icon = Icons.Default.Mail,
                        title = "Check New Message",
                        subtitle = "Read SMS or photo sent by family",
                        tint = GoldPrimary,
                        onClick = onTriggerSos
                    )
                    QuickRequestCard(
                        icon = Icons.Default.HearingDisabled,
                        title = "Can't Hear Phone Ring",
                        subtitle = "Set ringer to maximum loud volume",
                        tint = TerracottaSOS,
                        onClick = onTriggerSos
                    )
                    QuickRequestCard(
                        icon = Icons.Default.RestartAlt,
                        title = "Screen Seems Stuck",
                        subtitle = "Safely clean background memory",
                        tint = EmeraldTertiary,
                        onClick = onTriggerSos
                    )
                }
            }

            // 4. Phone Status Telemetry Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "YOUR PHONE STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextOnSurfaceVariant
                            )
                            Text(
                                text = "Updated just now",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextOnSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TelemetryMetricBox(
                                icon = Icons.Default.BatteryChargingFull,
                                value = "84%",
                                label = "Good Charge",
                                tint = EmeraldTertiary,
                                modifier = Modifier.weight(1f)
                            )
                            TelemetryMetricBox(
                                icon = Icons.Default.VolumeUp,
                                value = "90%",
                                label = "Loud & Clear",
                                tint = GoldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            TelemetryMetricBox(
                                icon = Icons.Default.Wifi,
                                value = "5G",
                                label = "Home Wi-Fi",
                                tint = EmeraldTertiary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 5. Spoken Audio Announcement Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerHigh)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Voice Help",
                            tint = GoldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Voice Help Active",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextOnSurfacePrimary
                            )
                            Text(
                                text = "Speaker will read options aloud",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextOnSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isVoiceHelpActive,
                        onCheckedChange = { isVoiceHelpActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldTertiary,
                            checkedTrackColor = EmeraldTertiary.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            // 6. Safe Shield Badge
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Verified Shield",
                        tint = TextOnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KinAssist Dignified Safe Shield Enabled",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextOnSurfaceVariant
                    )
                }
            }
        }

        // 7. Connecting Feedback Modal
        if (isCallingModalOpen) {
            Dialog(onDismissRequest = { isCallingModalOpen = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerHigh)
                                .border(3.dp, GoldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Rahul",
                                tint = GoldPrimary,
                                modifier = Modifier.size(52.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Calling Rahul...",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextOnSurfacePrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Speakerphone turning on automatically. Please hold on.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextOnSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        Row(
                            modifier = Modifier
                                .background(SurfaceContainerLow, RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(EmeraldTertiary, CircleShape)
                            )
                            Text(
                                text = "Rahul's phone is chiming",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldTertiary
                            )
                        }

                        Button(
                            onClick = { isCallingModalOpen = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHigh),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancel",
                                tint = TerracottaSOS
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cancel Call",
                                color = TextOnSurfacePrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickRequestCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextOnSurfacePrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextOnSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Forward",
            tint = TextOnSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun TelemetryMetricBox(
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .padding(10.dp),
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
            style = MaterialTheme.typography.headlineSmall,
            color = TextOnSurfacePrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextOnSurfaceVariant
        )
    }
}

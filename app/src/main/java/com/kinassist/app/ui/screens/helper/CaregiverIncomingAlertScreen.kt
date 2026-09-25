package com.kinassist.app.ui.screens.helper

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinassist.app.ui.theme.*

@Composable
fun CaregiverIncomingAlertScreen(
    onAcceptCall: () -> Unit,
    onDeclineCall: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring_chime")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chime"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DeepCanvas, SurfaceContainerLowest, Color(0xFF1F0D0A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Badge
            Row(
                modifier = Modifier
                    .padding(top = 40.dp)
                    .background(TerracottaSOS.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .border(1.dp, TerracottaSOS.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(TerracottaSOS, CircleShape)
                )
                Text(
                    text = "PRIORITY ASSIST SOS",
                    color = TerracottaSOS,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Center: Avatar with Chime Rings
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(TerracottaSOS.copy(alpha = 0.15f))
                    )
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh)
                            .border(3.dp, TerracottaSOS, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Mom",
                            tint = TerracottaSOS,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                Text(
                    text = "Mom needs help",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextOnSurfacePrimary
                )

                Text(
                    text = "Screen seems stuck on Settings\nTapped 1-Tap SOS • Speakerphone Ready",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextOnSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Remote Telemetry Snapshot
                Row(
                    modifier = Modifier
                        .background(SurfaceContainer, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(imageVector = Icons.Default.Battery5Bar, contentDescription = null, tint = EmeraldTertiary, modifier = Modifier.size(16.dp))
                        Text(text = "84%", fontSize = 12.sp, color = TextOnSurfacePrimary)
                    }
                    Text(text = "•", color = TextOnSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(imageVector = Icons.Default.Wifi, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Text(text = "Home Wi-Fi (5G)", fontSize = 12.sp, color = TextOnSurfacePrimary)
                    }
                }
            }

            // Bottom Actions (Accept / Decline)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAcceptCall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldTertiary),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Accept",
                        tint = DeepCanvas,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Accept & Open Live Screen",
                        color = DeepCanvas,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onDeclineCall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineBorder)
                ) {
                    Text(
                        text = "Can't Talk Right Now • Send Quick SMS",
                        color = TextOnSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

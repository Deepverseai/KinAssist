package com.kinassist.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinassist.app.ui.theme.*

@Composable
fun RoleSelectionScreen(
    onSelectSeniorMode: () -> Unit,
    onSelectHelperMode: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCanvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                modifier = Modifier.padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = 0.15f))
                        .border(2.dp, GoldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VolunteerActivism,
                        contentDescription = "KinAssist Logo",
                        tint = GoldPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "Welcome to KinAssist",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextOnSurfacePrimary
                )

                Text(
                    text = "Dignified, 1-Tap Remote Family Assistance.\nHow will you use this phone?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextOnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Role Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                RoleSelectionCard(
                    title = "I Need Help",
                    roleName = "Senior Parent Mode",
                    description = "Ultra-simple view with big buttons, zero passwords, and 1-tap SOS to call family.",
                    icon = Icons.Default.Elderly,
                    accentColor = GoldPrimary,
                    onClick = onSelectSeniorMode
                )

                RoleSelectionCard(
                    title = "I Help My Parents",
                    roleName = "Remote Helper / Caregiver Mode",
                    description = "Live screen mirror view with remote pointing arrows, voice talk, and soft rescue keys.",
                    icon = Icons.Default.SupervisorAccount,
                    accentColor = EmeraldTertiary,
                    onClick = onSelectHelperMode
                )
            }

            // Footer note
            Text(
                text = "100% Encrypted • P2P WebRTC • No Data Leaves Family",
                style = MaterialTheme.typography.labelSmall,
                color = TextOnSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
fun RoleSelectionCard(
    title: String,
    roleName: String,
    description: String,
    icon: ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.5.dp, OutlineBorder, RoundedCornerShape(22.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(2.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextOnSurfacePrimary
                )
                Text(
                    text = roleName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextOnSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Select",
                tint = TextOnSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

package com.kinassist.app.ui.screens.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinassist.app.ui.theme.*

@Composable
fun SessionStepRecapScreen(
    title: String = "Turned OFF Do Not Disturb",
    resolvedBy: String = "Rahul (Son)",
    onDone: () -> Unit
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
                modifier = Modifier.padding(top = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(EmeraldTertiary.copy(alpha = 0.15f))
                        .border(2.dp, EmeraldTertiary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = EmeraldTertiary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Issue Solved!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextOnSurfacePrimary
                )
                Text(
                    text = "Here is what $resolvedBy did for you today,\nso you can remember for next time:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextOnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Memory Card (3 Steps)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.5.dp, OutlineBorder, RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )

                    Divider(color = OutlineBorder)

                    StepRow(number = 1, text = "Opened Phone Settings from Home screen")
                    StepRow(number = 2, text = "Tapped on 'Sound & Vibration'")
                    StepRow(number = 3, text = "Switched 'Do Not Disturb' to OFF")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerHigh)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldTertiary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Ringtone is now loud & audible at 100%",
                            fontSize = 12.sp,
                            color = EmeraldTertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Bottom Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Got It, Thanks!",
                        color = DeepCanvas,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StepRow(number: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(GoldPrimary.copy(alpha = 0.2f), CircleShape)
                .border(1.dp, GoldPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                color = GoldPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = TextOnSurfacePrimary
        )
    }
}

package com.kinassist.app.ui.screens.onboarding

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinassist.app.ui.theme.*

@Composable
fun FamilyPairingScreen(
    familyPairCode: String = "849 210",
    onPairSuccess: () -> Unit
) {
    var isQrTabSelected by remember { mutableStateOf(true) }

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
                Text(
                    text = "Pair with Family",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextOnSurfacePrimary
                )
                Text(
                    text = "1-Time setup. No passwords required ever again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextOnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Tab Selector (QR Code vs 6-Digit PIN)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainer)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { isQrTabSelected = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isQrTabSelected) GoldPrimary else Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Scan QR Code",
                        color = if (isQrTabSelected) DeepCanvas else TextOnSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { isQrTabSelected = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isQrTabSelected) GoldPrimary else Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Use 6-Digit PIN",
                        color = if (!isQrTabSelected) DeepCanvas else TextOnSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Display Box: QR or PIN
            if (isQrTabSelected) {
                Card(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(2.dp, GoldPrimary, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "QR Code",
                            tint = Color.Black,
                            modifier = Modifier.size(190.dp)
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(24.dp))
                        .border(2.dp, GoldPrimary, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "SHARE THIS FAMILY PIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextOnSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = familyPairCode,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = GoldPrimary,
                            letterSpacing = 4.sp
                        )
                        Text(
                            text = "Expires in 15 minutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextOnSurfaceVariant
                        )
                    }
                }
            }

            // Bottom CTA
            Button(
                onClick = onPairSuccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "I've Paired Successfully",
                    color = DeepCanvas,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

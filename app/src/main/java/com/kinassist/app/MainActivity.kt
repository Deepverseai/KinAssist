package com.kinassist.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kinassist.app.core.overlay.PointerOverlayService
import com.kinassist.app.core.webrtc.PointerEvent
import com.kinassist.app.ui.screens.helper.CaregiverIncomingAlertScreen
import com.kinassist.app.ui.screens.helper.GuardianPrivacyShieldScreen
import com.kinassist.app.ui.screens.helper.HelperCanvasScreen
import com.kinassist.app.ui.screens.onboarding.FamilyPairingScreen
import com.kinassist.app.ui.screens.onboarding.RoleSelectionScreen
import com.kinassist.app.ui.screens.recap.SessionStepRecapScreen
import com.kinassist.app.ui.screens.senior.SeniorHomeScreen
import com.kinassist.app.ui.theme.DeepCanvas
import com.kinassist.app.ui.theme.KinAssistTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KinAssistTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepCanvas
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "senior_home"
                    ) {
                        // Senior Mode Home (1-Tap SOS)
                        composable("senior_home") {
                            SeniorHomeScreen(
                                onTriggerSos = {
                                    // Start Overlay Service
                                    startService(Intent(this@MainActivity, PointerOverlayService::class.java))
                                    navController.navigate("helper_canvas")
                                },
                                onOpenSettings = {
                                    navController.navigate("role_selection")
                                }
                            )
                        }

                        // Helper Mode Live Remote Canvas
                        composable("helper_canvas") {
                            HelperCanvasScreen(
                                onSendPointerEvent = { event: PointerEvent ->
                                    PointerOverlayService.showPointer(event)
                                },
                                onDisconnect = {
                                    stopService(Intent(this@MainActivity, PointerOverlayService::class.java))
                                    navController.navigate("step_recap")
                                }
                            )
                        }

                        // Caregiver Incoming Alert Screen (Chime)
                        composable("incoming_alert") {
                            CaregiverIncomingAlertScreen(
                                onAcceptCall = {
                                    navController.navigate("helper_canvas")
                                },
                                onDeclineCall = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Role Selection (Senior vs Helper)
                        composable("role_selection") {
                            RoleSelectionScreen(
                                onSelectSeniorMode = {
                                    navController.navigate("senior_home")
                                },
                                onSelectHelperMode = {
                                    navController.navigate("incoming_alert")
                                }
                            )
                        }

                        // Family 1-Time Pairing Screen
                        composable("family_pairing") {
                            FamilyPairingScreen(
                                onPairSuccess = {
                                    navController.navigate("senior_home")
                                }
                            )
                        }

                        // Session Step Recap (Family Memory Card)
                        composable("step_recap") {
                            SessionStepRecapScreen(
                                onDone = {
                                    navController.navigate("senior_home")
                                }
                            )
                        }

                        // Anti-Scam Guardian Privacy Shield
                        composable("privacy_shield") {
                            GuardianPrivacyShieldScreen(
                                onVoiceGuideActive = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

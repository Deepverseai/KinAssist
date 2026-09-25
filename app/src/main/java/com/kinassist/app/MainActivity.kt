package com.kinassist.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kinassist.app.core.accessibility.KinAssistAccessibilityService
import com.kinassist.app.core.overlay.PointerOverlayService
import com.kinassist.app.core.pairing.PairingManager
import com.kinassist.app.core.signaling.SignalingClient
import com.kinassist.app.core.signaling.SignalingEvent
import com.kinassist.app.core.telemetry.TelemetryManager
import com.kinassist.app.core.webrtc.*
import com.kinassist.app.ui.screens.helper.CaregiverIncomingAlertScreen
import com.kinassist.app.ui.screens.helper.GuardianPrivacyShieldScreen
import com.kinassist.app.ui.screens.helper.HelperCanvasScreen
import com.kinassist.app.ui.screens.onboarding.FamilyPairingScreen
import com.kinassist.app.ui.screens.onboarding.RoleSelectionScreen
import com.kinassist.app.ui.screens.recap.SessionStepRecapScreen
import com.kinassist.app.ui.screens.senior.SeniorHomeScreen
import com.kinassist.app.ui.theme.DeepCanvas
import com.kinassist.app.ui.theme.KinAssistTheme
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "KinAssistMainActivity"
    }

    private lateinit var pairingManager: PairingManager
    private lateinit var signalingClient: SignalingClient
    private lateinit var telemetryManager: TelemetryManager
    private lateinit var webrtcManager: WebRtcManager

    private val defaultIceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun.services.mozilla.com").createIceServer()
    )

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            Log.d(TAG, "MediaProjection permission granted. Starting ScreenCaptureService...")
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            // Start Floating Pointer Overlay Service
            startService(Intent(this, PointerOverlayService::class.java))

            // Start periodic telemetry emission over WebRTC DataChannel
            telemetryManager.startEmitting { telemetry ->
                webrtcManager.sendTelemetry(telemetry)
            }

            // Create and send SDP Offer once screen pipeline is ready
            webrtcManager.createOffer { sdp ->
                lifecycleScope.launch {
                    val config = pairingManager.getPairingConfig()
                    signalingClient.sendSdpOffer(config.familyCode, sdp)
                }
            }
        } else {
            Log.w(TAG, "MediaProjection permission denied by user.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pairingManager = PairingManager(this)
        telemetryManager = TelemetryManager(this)
        webrtcManager = WebRtcManager.getInstance(this)
        webrtcManager.initFactory()

        val config = pairingManager.getPairingConfig()
        signalingClient = SignalingClient(config.signalingUrl)

        // Connect to Signaling Server
        signalingClient.connect(
            familyCode = config.familyCode,
            role = config.role,
            deviceName = if (config.role == "SENIOR") config.seniorName else config.helperName
        )

        // Setup WebRTC PeerConnection
        webrtcManager.createPeerConnection(defaultIceServers) { candidate ->
            lifecycleScope.launch {
                signalingClient.sendIceCandidate(
                    familyCode = config.familyCode,
                    sdpMid = candidate.sdpMid,
                    sdpMLineIndex = candidate.sdpMLineIndex,
                    sdp = candidate.sdp
                )
            }
        }

        // Senior side: Listen for incoming remote pointer events & rescue commands
        lifecycleScope.launch {
            webrtcManager.receivedPointerEvents.collect { event ->
                event?.let { PointerOverlayService.showPointer(it) }
            }
        }
        lifecycleScope.launch {
            webrtcManager.receivedRescueCommands.collect { command ->
                command?.let { KinAssistAccessibilityService.performRescue(it) }
            }
        }

        setContent {
            KinAssistTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepCanvas
                ) {
                    val navController = rememberNavController()

                    // Observe WebRTC state flows
                    val remoteVideoTrack by webrtcManager.remoteVideoTrack.collectAsState()
                    val remoteTelemetry by webrtcManager.receivedTelemetry.collectAsState()
                    val isPrivacyBlackout by webrtcManager.isPrivacyBlackoutActive.collectAsState()

                    // Handle Signaling Events
                    LaunchedEffect(Unit) {
                        signalingClient.events.collect { event ->
                            when (event) {
                                is SignalingEvent.IncomingCall -> {
                                    Log.d(TAG, "Incoming SOS from ${event.seniorName}")
                                    navController.navigate("incoming_alert")
                                }
                                is SignalingEvent.CallAccepted -> {
                                    Log.d(TAG, "Call accepted by helper. Requesting screen capture...")
                                    webrtcManager.createDataChannel()
                                    val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                    screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
                                }
                                is SignalingEvent.RemoteOffer -> {
                                    Log.d(TAG, "Helper received SDP Offer. Generating Answer...")
                                    webrtcManager.setRemoteDescription(SessionDescription.Type.OFFER, event.sdp) {
                                        webrtcManager.createAnswer { answerSdp ->
                                            lifecycleScope.launch {
                                                signalingClient.sendSdpAnswer(config.familyCode, answerSdp)
                                            }
                                        }
                                    }
                                }
                                is SignalingEvent.RemoteAnswer -> {
                                    Log.d(TAG, "Senior received SDP Answer. Live link active!")
                                    webrtcManager.setRemoteDescription(SessionDescription.Type.ANSWER, event.sdp)
                                }
                                is SignalingEvent.RemoteIceCandidate -> {
                                    webrtcManager.addRemoteIceCandidate(event.sdpMid, event.sdpMLineIndex, event.sdp)
                                }
                                is SignalingEvent.CallEnded -> {
                                    stopService(Intent(this@MainActivity, ScreenCaptureService::class.java))
                                    stopService(Intent(this@MainActivity, PointerOverlayService::class.java))
                                    telemetryManager.stopEmitting()
                                    webrtcManager.close()
                                    navController.navigate("step_recap")
                                }
                                else -> {}
                            }
                        }
                    }

                    val startDest = if (!pairingManager.isPaired()) {
                        "role_selection"
                    } else if (config.role == "HELPER") {
                        "helper_canvas"
                    } else {
                        "senior_home"
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDest
                    ) {
                        // Senior Mode Home (1-Tap SOS)
                        composable("senior_home") {
                            SeniorHomeScreen(
                                onTriggerSos = {
                                    val currentTelemetry = telemetryManager.gatherTelemetry()
                                    lifecycleScope.launch {
                                        signalingClient.triggerSos(
                                            familyCode = config.familyCode,
                                            seniorName = config.seniorName,
                                            batteryLevel = "${currentTelemetry.batteryLevel}%"
                                        )
                                    }
                                },
                                onOpenSettings = {
                                    navController.navigate("role_selection")
                                }
                            )
                        }

                        // Helper Mode Live Remote Canvas
                        composable("helper_canvas") {
                            HelperCanvasScreen(
                                remoteVideoTrack = remoteVideoTrack,
                                isPrivacyBlackout = isPrivacyBlackout,
                                telemetryData = remoteTelemetry,
                                onSendPointerEvent = { event: PointerEvent ->
                                    webrtcManager.sendPointerEvent(event)
                                },
                                onSendRescueCommand = { cmd: RescueCommand ->
                                    webrtcManager.sendRescueCommand(cmd)
                                },
                                onToggleMute = { muted: Boolean ->
                                    webrtcManager.setAudioMuted(muted)
                                },
                                onDisconnect = {
                                    lifecycleScope.launch {
                                        signalingClient.endCall(config.familyCode)
                                    }
                                    webrtcManager.close()
                                    navController.navigate("step_recap")
                                }
                            )
                        }

                        // Caregiver Incoming Alert Screen (Chime)
                        composable("incoming_alert") {
                            CaregiverIncomingAlertScreen(
                                onAcceptCall = {
                                    lifecycleScope.launch {
                                        signalingClient.acceptCall(config.familyCode, config.helperName)
                                    }
                                    navController.navigate("helper_canvas")
                                },
                                onDeclineCall = {
                                    lifecycleScope.launch {
                                        signalingClient.declineCall(config.familyCode, "Caregiver Busy")
                                    }
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Role Selection (Senior vs Helper)
                        composable("role_selection") {
                            RoleSelectionScreen(
                                onSelectSeniorMode = {
                                    pairingManager.savePairing(role = "SENIOR", familyCode = config.familyCode)
                                    navController.navigate("senior_home")
                                },
                                onSelectHelperMode = {
                                    pairingManager.savePairing(role = "HELPER", familyCode = config.familyCode)
                                    navController.navigate("helper_canvas")
                                }
                            )
                        }

                        // Family 1-Time Pairing Screen
                        composable("family_pairing") {
                            FamilyPairingScreen(
                                onPairSuccess = {
                                    pairingManager.savePairing(role = "SENIOR", familyCode = "884219")
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

    override fun onDestroy() {
        telemetryManager.stopEmitting()
        signalingClient.disconnect()
        webrtcManager.close()
        super.onDestroy()
    }
}

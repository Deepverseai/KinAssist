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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kinassist.app.core.accessibility.KinAssistAccessibilityService
import com.kinassist.app.core.overlay.PointerOverlayService
import com.kinassist.app.core.pairing.PairingManager
import com.kinassist.app.core.signaling.SignalingClient
import com.kinassist.app.core.signaling.SignalingConnectionStatus
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
import com.kinassist.app.ui.theme.KinAssistTheme
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "KinAssistMainActivity"
    }

    private lateinit var pairingManager: PairingManager
    private lateinit var telemetryManager: TelemetryManager
    private lateinit var webrtcManager: WebRtcManager
    private lateinit var signalingClient: SignalingClient

    private val defaultIceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun.services.mozilla.com").createIceServer(),
        // Coturn TURN Relay for symmetric NAT traversal across 4G/5G mobile networks
        PeerConnection.IceServer.builder("turn:kinassist.app:3478")
            .setUsername("kinuser")
            .setPassword("kinassist_secret_pass")
            .createIceServer()
    )

    private val runtimePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[android.Manifest.permission.RECORD_AUDIO] == true
        Log.d(TAG, "Audio permission result: $audioGranted")
        if (audioGranted) {
            try {
                webrtcManager.startAudio()
            } catch (e: Throwable) {
                Log.w(TAG, "startAudio after perm: ${e.message}")
            }
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val granted = android.provider.Settings.canDrawOverlays(this)
            Log.d(TAG, "Overlay permission result: $granted")
        }
    }

    private fun requestPermissionsIfNecessary() {
        val neededPerms = mutableListOf<String>()
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            neededPerms.add(android.Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                neededPerms.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (neededPerms.isNotEmpty()) {
            runtimePermissionsLauncher.launch(neededPerms.toTypedArray())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            try {
                val intent = Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot launch overlay settings: ${e.message}")
            }
        }
    }

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            Log.d(TAG, "MediaProjection permission granted. Starting ScreenCaptureService...")
            try {
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
            } catch (e: Exception) {
                Log.e(TAG, "Could not start ScreenCaptureService: ${e.message}", e)
            }

            // Start Floating Pointer Overlay Service safely
            try {
                startService(Intent(this, PointerOverlayService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Could not start PointerOverlayService: ${e.message}", e)
            }

            // Start real-time telemetry emission over WebRTC DataChannel
            try {
                telemetryManager.startEmitting { telemetry ->
                    webrtcManager.sendTelemetry(telemetry)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not start telemetry emission: ${e.message}", e)
            }

            // Create and send SDP Offer once screen pipeline is ready
            try {
                webrtcManager.createOffer { sdp ->
                    lifecycleScope.launch {
                        val config = pairingManager.getPairingConfig()
                        signalingClient.sendSdpOffer(config.familyCode, sdp)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not create WebRTC offer: ${e.message}", e)
            }
        } else {
            Log.w(TAG, "MediaProjection permission dismissed or running in emulator mode.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissionsIfNecessary()

        pairingManager = PairingManager(this)
        telemetryManager = TelemetryManager(this)
        webrtcManager = WebRtcManager.getInstance(this)
        try {
            webrtcManager.initFactory()
        } catch (e: Throwable) {
            Log.w(TAG, "WebRTC init note: ${e.message}")
        }

        val config = pairingManager.getPairingConfig()
        signalingClient = SignalingClient(config.signalingUrl)

        // Attempt connecting to signaling server (operates via direct P2P link if server offline)
        signalingClient.connect(
            familyCode = config.familyCode,
            role = config.role,
            deviceName = if (config.role == "SENIOR") config.seniorName else config.helperName
        )

        // Setup WebRTC PeerConnection
        try {
            webrtcManager.createPeerConnection(defaultIceServers) { candidate ->
                lifecycleScope.launch {
                    val currentConf = pairingManager.getPairingConfig()
                    signalingClient.sendIceCandidate(
                        familyCode = currentConf.familyCode,
                        sdpMid = candidate.sdpMid,
                        sdpMLineIndex = candidate.sdpMLineIndex,
                        sdp = candidate.sdp
                    )
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "PeerConnection creation note: ${e.message}")
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
            KinAssistTheme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Observe connection status
                    val signalingStatus by signalingClient.status.collectAsState()
                    var isServerSettingsOpen by remember { mutableStateOf(false) }

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
                                    Log.d(TAG, "Call accepted. Requesting screen capture...")
                                    webrtcManager.createDataChannel()
                                    try {
                                        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                                        projectionManager?.let {
                                            screenCaptureLauncher.launch(it.createScreenCaptureIntent())
                                        }
                                    } catch (e: Exception) {
                                        Log.w(TAG, "MediaProjection launch note: ${e.message}")
                                    }
                                }
                                is SignalingEvent.RemoteOffer -> {
                                    Log.d(TAG, "Helper received SDP Offer. Generating Answer...")
                                    webrtcManager.setRemoteDescription(SessionDescription.Type.OFFER, event.sdp) {
                                        webrtcManager.createAnswer { answerSdp ->
                                            lifecycleScope.launch {
                                                val c = pairingManager.getPairingConfig()
                                                signalingClient.sendSdpAnswer(c.familyCode, answerSdp)
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
                                    try {
                                        stopService(Intent(this@MainActivity, ScreenCaptureService::class.java))
                                        stopService(Intent(this@MainActivity, PointerOverlayService::class.java))
                                    } catch (_: Exception) {}
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
                    } else {
                        val currentConf = pairingManager.getPairingConfig()
                        if (currentConf.role == "HELPER") "helper_canvas" else "senior_home"
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDest
                    ) {
                        // Senior Mode Home (1-Tap SOS)
                        composable("senior_home") {
                            val conf = pairingManager.getPairingConfig()
                            val statusText = when (signalingStatus) {
                                is SignalingConnectionStatus.Connected -> "Direct Link Active"
                                is SignalingConnectionStatus.DirectP2P -> "Direct P2P Link Active"
                                is SignalingConnectionStatus.Connecting -> "Connecting Link..."
                                else -> "Direct Link Ready"
                            }

                            SeniorHomeScreen(
                                seniorName = conf.seniorName,
                                pairedHelperName = conf.helperName,
                                helperPhone = conf.helperPhone,
                                connectionStatusText = statusText,
                                telemetryData = telemetryManager.gatherTelemetry(),
                                onTriggerSos = { reqType ->
                                    val currentTelemetry = telemetryManager.gatherTelemetry()
                                    lifecycleScope.launch {
                                        signalingClient.triggerSos(
                                            familyCode = conf.familyCode,
                                            seniorName = conf.seniorName,
                                            batteryLevel = "${currentTelemetry.batteryLevel}%"
                                        )
                                    }
                                },
                                onStartScreenShare = {
                                    try {
                                        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                                        projectionManager?.let {
                                            screenCaptureLauncher.launch(it.createScreenCaptureIntent())
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Screen share launch error: ${e.message}", e)
                                    }
                                },
                                onOpenSettings = {
                                    navController.navigate("role_selection")
                                },
                                onSwitchToCaregiverView = {
                                    pairingManager.savePairing(
                                        role = "HELPER",
                                        familyCode = conf.familyCode,
                                        seniorName = conf.seniorName,
                                        helperName = conf.helperName,
                                        helperPhone = conf.helperPhone
                                    )
                                    navController.navigate("helper_canvas")
                                }
                            )
                        }

                        // Helper Mode Live Remote Canvas
                        composable("helper_canvas") {
                            val conf = pairingManager.getPairingConfig()
                            HelperCanvasScreen(
                                assistedPersonName = conf.seniorName,
                                remoteVideoTrack = remoteVideoTrack,
                                isPrivacyBlackout = isPrivacyBlackout,
                                telemetryData = remoteTelemetry ?: telemetryManager.gatherTelemetry(),
                                onSendPointerEvent = { event: PointerEvent ->
                                    webrtcManager.sendPointerEvent(event)
                                },
                                onSendRescueCommand = { cmd: RescueCommand ->
                                    webrtcManager.sendRescueCommand(cmd)
                                },
                                onRequestScreenShare = {
                                    try {
                                        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                                        projectionManager?.let {
                                            screenCaptureLauncher.launch(it.createScreenCaptureIntent())
                                        }
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Request screen share: ${e.message}")
                                    }
                                },
                                onRingLoudly = {
                                    telemetryManager.setMaxRingerVolume()
                                    telemetryManager.playTestChime()
                                },
                                onToggleMute = { muted: Boolean ->
                                    webrtcManager.setAudioMuted(muted)
                                },
                                onDisconnect = {
                                    lifecycleScope.launch {
                                        signalingClient.endCall(conf.familyCode)
                                    }
                                    webrtcManager.close()
                                    navController.navigate("step_recap")
                                },
                                onNavigateBack = {
                                    navController.navigate("senior_home")
                                }
                            )
                        }

                        // Caregiver Incoming Alert Screen
                        composable("incoming_alert") {
                            val conf = pairingManager.getPairingConfig()
                            val liveTelem = telemetryManager.gatherTelemetry()

                            CaregiverIncomingAlertScreen(
                                seniorName = conf.seniorName,
                                seniorBattery = "${liveTelem.batteryLevel}%",
                                networkType = liveTelem.networkType,
                                issueReason = "1-Tap Assistance Request",
                                onAcceptCall = {
                                    lifecycleScope.launch {
                                        signalingClient.acceptCall(conf.familyCode, conf.helperName)
                                    }
                                    navController.navigate("helper_canvas")
                                },
                                onDeclineCall = {
                                    lifecycleScope.launch {
                                        signalingClient.declineCall(conf.familyCode, "Caregiver Busy")
                                    }
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Role Selection (Senior vs Helper)
                        composable("role_selection") {
                            val conf = pairingManager.getPairingConfig()
                            val isConnected = signalingStatus is SignalingConnectionStatus.Connected

                            RoleSelectionScreen(
                                currentServerUrl = signalingClient.getServerUrl(),
                                connectionStatusDesc = if (isConnected) "Connected to Signaling Server" else "Direct P2P Link Ready",
                                onSelectSeniorMode = {
                                    pairingManager.savePairing(
                                        role = "SENIOR",
                                        familyCode = conf.familyCode,
                                        seniorName = conf.seniorName,
                                        helperName = conf.helperName,
                                        helperPhone = conf.helperPhone
                                    )
                                    lifecycleScope.launch {
                                        signalingClient.sendRegister(conf.familyCode, "SENIOR", conf.seniorName)
                                    }
                                    navController.navigate("senior_home")
                                },
                                onSelectHelperMode = {
                                    pairingManager.savePairing(
                                        role = "HELPER",
                                        familyCode = conf.familyCode,
                                        seniorName = conf.seniorName,
                                        helperName = conf.helperName,
                                        helperPhone = conf.helperPhone
                                    )
                                    lifecycleScope.launch {
                                        signalingClient.sendRegister(conf.familyCode, "HELPER", conf.helperName)
                                    }
                                    navController.navigate("helper_canvas")
                                },
                                onOpenPairing = {
                                    navController.navigate("family_pairing")
                                },
                                onConfigureServer = {
                                    isServerSettingsOpen = true
                                }
                            )
                        }

                        // Family 1-Time Pairing Screen
                        composable("family_pairing") {
                            val conf = pairingManager.getPairingConfig()
                            FamilyPairingScreen(
                                initialPairCode = conf.familyCode,
                                initialSeniorName = conf.seniorName,
                                initialHelperName = conf.helperName,
                                initialHelperPhone = conf.helperPhone,
                                onPairSuccess = { newCode, sName, hName, hPhone ->
                                    pairingManager.savePairing(
                                        role = conf.role,
                                        familyCode = newCode,
                                        seniorName = sName,
                                        helperName = hName,
                                        helperPhone = hPhone
                                    )
                                    lifecycleScope.launch {
                                        signalingClient.connect(newCode, conf.role, sName)
                                    }
                                    navController.navigate("senior_home")
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Session Step Recap (Family Memory Card)
                        composable("step_recap") {
                            val conf = pairingManager.getPairingConfig()
                            SessionStepRecapScreen(
                                title = "Assistance Session Completed",
                                resolvedBy = conf.helperName,
                                stepsTaken = listOf(
                                    "Connected live assistance link with ${conf.seniorName}",
                                    "Checked phone battery, volume, and connectivity",
                                    "Guided with interactive pointer navigation"
                               ),
                                onDone = {
                                    navController.navigate("senior_home")
                                }
                            )
                        }

                        // Anti-Scam Guardian Privacy Shield
                        composable("privacy_shield") {
                            GuardianPrivacyShieldScreen(
                                packageName = "Banking / PIN Screen",
                                onDismiss = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }

                    // Server Configuration Dialog
                    if (isServerSettingsOpen) {
                        ServerSettingsDialog(
                            currentUrl = signalingClient.getServerUrl(),
                            onDismiss = { isServerSettingsOpen = false },
                            onSave = { newUrl, enableDirectP2P ->
                                isServerSettingsOpen = false
                                signalingClient.updateServerUrl(newUrl)
                                if (enableDirectP2P) {
                                    signalingClient.setDirectP2PMode(true)
                                } else {
                                    signalingClient.setDirectP2PMode(false)
                                    val conf = pairingManager.getPairingConfig()
                                    signalingClient.connect(conf.familyCode, conf.role, conf.seniorName)
                                }
                            }
                        )
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

@Composable
fun ServerSettingsDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (url: String, enableDirectP2P: Boolean) -> Unit
) {
    var urlText by remember { mutableStateOf(currentUrl) }
    var useDirectP2P by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
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
                Text(
                    text = "Signaling & Network Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Configure your WebRTC signaling server URL or use Direct Local P2P Mode (no external server required).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Signaling WebSocket URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Direct Local Link (P2P)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Operate directly on local network",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useDirectP2P,
                        onCheckedChange = { useDirectP2P = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(urlText, useDirectP2P) }) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

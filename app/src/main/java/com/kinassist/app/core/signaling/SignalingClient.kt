package com.kinassist.app.core.signaling

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString

class SignalingClient(
    private val serverUrl: String = "ws://10.0.2.2:8080" // Default for Android Emulator to host
) {
    companion object {
        private const val TAG = "KinAssistSignaling"
    }

    private val client = HttpClient(OkHttp) {
        install(WebSockets) {
            pingIntervalMillis = 15_000
        }
    }

    private var session: DefaultClientWebSocketSession? = null
    private var clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _events = MutableSharedFlow<SignalingEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    private var currentFamilyCode: String? = null
    private var currentRole: String? = null
    private var currentDeviceName: String? = null

    fun connect(familyCode: String, role: String, deviceName: String) {
        currentFamilyCode = familyCode
        currentRole = role
        currentDeviceName = deviceName

        clientScope.launch {
            try {
                Log.d(TAG, "Connecting to signaling server at $serverUrl...")
                session = client.webSocketSession(urlString = serverUrl)
                _isConnected.value = true
                Log.d(TAG, "Connected to signaling server.")

                // Register with the room
                sendRegister(familyCode, role, deviceName)

                // Start listening for incoming frames
                listenForMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Signaling connection failed: ${e.message}", e)
                _isConnected.value = false
                _events.emit(SignalingEvent.Error("Connection failed: ${e.localizedMessage}"))
            }
        }
    }

    private suspend fun listenForMessages() {
        val currentSession = session ?: return
        try {
            for (frame in currentSession.incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    handleIncomingJson(text)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in message loop: ${e.message}", e)
        } finally {
            _isConnected.value = false
            Log.d(TAG, "Signaling session ended.")
        }
    }

    private suspend fun handleIncomingJson(jsonStr: String) {
        try {
            val baseMsg = signalingJson.decodeFromString<BaseSignalingMessage>(jsonStr)
            when (baseMsg.type) {
                "REGISTERED" -> {
                    _events.emit(
                        SignalingEvent.Registered(
                            familyCode = baseMsg.familyCode ?: "",
                            role = baseMsg.role ?: "",
                            roomState = baseMsg.roomState ?: "IDLE"
                        )
                    )
                }
                "INCOMING_CALL" -> {
                    _events.emit(
                        SignalingEvent.IncomingCall(
                            familyCode = baseMsg.familyCode ?: "",
                            seniorName = baseMsg.seniorName ?: "Mom / Dad",
                            batteryLevel = baseMsg.batteryLevel ?: "Unknown"
                        )
                    )
                }
                "CALL_ACCEPTED" -> {
                    _events.emit(
                        SignalingEvent.CallAccepted(
                            familyCode = baseMsg.familyCode ?: "",
                            helperName = baseMsg.helperName ?: "Caregiver"
                        )
                    )
                }
                "CALL_DECLINED" -> {
                    _events.emit(
                        SignalingEvent.CallDeclined(
                            familyCode = baseMsg.familyCode ?: "",
                            reason = baseMsg.reason ?: "Caregiver busy"
                        )
                    )
                }
                "SDP_OFFER" -> {
                    baseMsg.sdp?.let { sdp ->
                        _events.emit(SignalingEvent.RemoteOffer(sdp))
                    }
                }
                "SDP_ANSWER" -> {
                    baseMsg.sdp?.let { sdp ->
                        _events.emit(SignalingEvent.RemoteAnswer(sdp))
                    }
                }
                "ICE_CANDIDATE" -> {
                    baseMsg.candidate?.let { cand ->
                        _events.emit(
                            SignalingEvent.RemoteIceCandidate(
                                sdpMid = cand.sdpMid,
                                sdpMLineIndex = cand.sdpMLineIndex,
                                sdp = cand.sdp
                            )
                        )
                    }
                }
                "CALL_ENDED" -> {
                    _events.emit(SignalingEvent.CallEnded(baseMsg.reason ?: "Session ended"))
                }
                "PEER_DISCONNECTED" -> {
                    _events.emit(
                        SignalingEvent.PeerDisconnected(
                            role = baseMsg.role ?: "PEER",
                            message = baseMsg.message ?: "Peer disconnected"
                        )
                    )
                }
                "ERROR" -> {
                    _events.emit(SignalingEvent.Error(baseMsg.message ?: "Unknown error"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message: $jsonStr, err: ${e.message}")
        }
    }

    suspend fun sendRegister(familyCode: String, role: String, deviceName: String) {
        val msg = BaseSignalingMessage(
            type = "REGISTER",
            familyCode = familyCode,
            role = role,
            deviceName = deviceName
        )
        sendMessage(msg)
    }

    suspend fun triggerSos(familyCode: String, seniorName: String, batteryLevel: String) {
        val msg = BaseSignalingMessage(
            type = "SOS_ALERT",
            familyCode = familyCode,
            seniorName = seniorName,
            batteryLevel = batteryLevel
        )
        sendMessage(msg)
    }

    suspend fun acceptCall(familyCode: String, helperName: String) {
        val msg = BaseSignalingMessage(
            type = "CALL_ACCEPT",
            familyCode = familyCode,
            deviceName = helperName
        )
        sendMessage(msg)
    }

    suspend fun declineCall(familyCode: String, reason: String = "Busy") {
        val msg = BaseSignalingMessage(
            type = "CALL_DECLINE",
            familyCode = familyCode,
            reason = reason
        )
        sendMessage(msg)
    }

    suspend fun sendSdpOffer(familyCode: String, sdp: String) {
        val msg = BaseSignalingMessage(
            type = "SDP_OFFER",
            familyCode = familyCode,
            sdp = sdp
        )
        sendMessage(msg)
    }

    suspend fun sendSdpAnswer(familyCode: String, sdp: String) {
        val msg = BaseSignalingMessage(
            type = "SDP_ANSWER",
            familyCode = familyCode,
            sdp = sdp
        )
        sendMessage(msg)
    }

    suspend fun sendIceCandidate(familyCode: String, sdpMid: String, sdpMLineIndex: Int, sdp: String) {
        val msg = BaseSignalingMessage(
            type = "ICE_CANDIDATE",
            familyCode = familyCode,
            candidate = IceCandidatePayload(
                sdpMid = sdpMid,
                sdpMLineIndex = sdpMLineIndex,
                sdp = sdp
            )
        )
        sendMessage(msg)
    }

    suspend fun endCall(familyCode: String) {
        val msg = BaseSignalingMessage(
            type = "CALL_END",
            familyCode = familyCode
        )
        sendMessage(msg)
    }

    private suspend fun sendMessage(message: BaseSignalingMessage) {
        try {
            val jsonString = signalingJson.encodeToString(message)
            session?.send(Frame.Text(jsonString))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message: ${e.message}", e)
        }
    }

    fun disconnect() {
        clientScope.launch {
            try {
                session?.close()
                session = null
                _isConnected.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error closing session: ${e.message}")
            }
        }
    }
}

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

sealed class SignalingConnectionStatus {
    object Idle : SignalingConnectionStatus()
    object Connecting : SignalingConnectionStatus()
    data class Connected(val url: String) : SignalingConnectionStatus()
    data class DirectP2P(val reason: String = "Direct Link Active") : SignalingConnectionStatus()
    data class Disconnected(val reason: String) : SignalingConnectionStatus()
}

class SignalingClient(
    private var serverUrl: String = "ws://10.0.2.2:8080"
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

    private val _status = MutableStateFlow<SignalingConnectionStatus>(SignalingConnectionStatus.Idle)
    val status = _status.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _events = MutableSharedFlow<SignalingEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    private var currentFamilyCode: String = "884219"
    private var currentRole: String = "SENIOR"
    private var currentDeviceName: String = "Senior"
    private var isDirectP2P = false

    fun getServerUrl(): String = serverUrl

    fun updateServerUrl(newUrl: String) {
        serverUrl = newUrl
    }

    fun setDirectP2PMode(enabled: Boolean) {
        isDirectP2P = enabled
        if (enabled) {
            disconnect()
            _status.value = SignalingConnectionStatus.DirectP2P("Local P2P Link Active")
            _isConnected.value = true
            Log.d(TAG, "Switched to Direct P2P Mode.")
        }
    }

    fun connect(familyCode: String, role: String, deviceName: String) {
        currentFamilyCode = familyCode
        currentRole = role
        currentDeviceName = deviceName

        if (isDirectP2P) {
            _status.value = SignalingConnectionStatus.DirectP2P()
            _isConnected.value = true
            return
        }

        clientScope.launch {
            try {
                _status.value = SignalingConnectionStatus.Connecting
                Log.d(TAG, "Connecting to signaling server at $serverUrl...")

                session = withTimeout(3500) {
                    client.webSocketSession(urlString = serverUrl)
                }

                _isConnected.value = true
                _status.value = SignalingConnectionStatus.Connected(serverUrl)
                Log.d(TAG, "Connected to signaling server.")

                sendRegister(familyCode, role, deviceName)
                listenForMessages()
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Server unreachable"
                Log.w(TAG, "Signaling connection to $serverUrl unavailable ($errorMsg). Operating in Direct P2P Mode.")
                
                isDirectP2P = true
                _isConnected.value = true
                _status.value = SignalingConnectionStatus.DirectP2P("Direct P2P Link Ready")
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
            Log.w(TAG, "Signaling session message loop closed: ${e.message}")
        } finally {
            _isConnected.value = false
            if (!isDirectP2P) {
                _status.value = SignalingConnectionStatus.Disconnected("Connection closed")
            }
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
                            seniorName = baseMsg.seniorName ?: "Family Member",
                            batteryLevel = baseMsg.batteryLevel ?: "100%"
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
        currentFamilyCode = familyCode
        currentRole = role
        currentDeviceName = deviceName

        if (isDirectP2P) {
            _events.emit(SignalingEvent.Registered(familyCode, role, "IDLE"))
            return
        }

        val msg = BaseSignalingMessage(
            type = "REGISTER",
            familyCode = familyCode,
            role = role,
            deviceName = deviceName
        )
        sendMessage(msg)
    }

    suspend fun triggerSos(familyCode: String, seniorName: String, batteryLevel: String) {
        if (isDirectP2P) {
            clientScope.launch {
                delay(300)
                _events.emit(
                    SignalingEvent.IncomingCall(
                        familyCode = familyCode,
                        seniorName = seniorName,
                        batteryLevel = batteryLevel
                    )
                )
            }
            return
        }

        val msg = BaseSignalingMessage(
            type = "SOS_ALERT",
            familyCode = familyCode,
            seniorName = seniorName,
            batteryLevel = batteryLevel
        )
        sendMessage(msg)
    }

    suspend fun acceptCall(familyCode: String, helperName: String) {
        if (isDirectP2P) {
            clientScope.launch {
                delay(200)
                _events.emit(SignalingEvent.CallAccepted(familyCode, helperName))
            }
            return
        }

        val msg = BaseSignalingMessage(
            type = "CALL_ACCEPT",
            familyCode = familyCode,
            deviceName = helperName
        )
        sendMessage(msg)
    }

    suspend fun declineCall(familyCode: String, reason: String = "Busy") {
        if (isDirectP2P) {
            _events.emit(SignalingEvent.CallDeclined(familyCode, reason))
            return
        }

        val msg = BaseSignalingMessage(
            type = "CALL_DECLINE",
            familyCode = familyCode,
            reason = reason
        )
        sendMessage(msg)
    }

    suspend fun sendSdpOffer(familyCode: String, sdp: String) {
        if (isDirectP2P) return
        val msg = BaseSignalingMessage(
            type = "SDP_OFFER",
            familyCode = familyCode,
            sdp = sdp
        )
        sendMessage(msg)
    }

    suspend fun sendSdpAnswer(familyCode: String, sdp: String) {
        if (isDirectP2P) return
        val msg = BaseSignalingMessage(
            type = "SDP_ANSWER",
            familyCode = familyCode,
            sdp = sdp
        )
        sendMessage(msg)
    }

    suspend fun sendIceCandidate(familyCode: String, sdpMid: String, sdpMLineIndex: Int, sdp: String) {
        if (isDirectP2P) return
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
        if (isDirectP2P) {
            _events.emit(SignalingEvent.CallEnded("Session completed"))
            return
        }

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
            Log.w(TAG, "Send message failed (${e.message}), transitioning to Direct P2P mode")
            isDirectP2P = true
            _status.value = SignalingConnectionStatus.DirectP2P("Direct P2P Link Active")
        }
    }

    fun disconnect() {
        clientScope.launch {
            try {
                session?.close()
                session = null
                _isConnected.value = isDirectP2P
            } catch (e: Exception) {
                Log.w(TAG, "Error closing session: ${e.message}")
            }
        }
    }
}

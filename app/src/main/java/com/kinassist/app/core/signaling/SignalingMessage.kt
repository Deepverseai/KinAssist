package com.kinassist.app.core.signaling

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val signalingJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

@Serializable
data class BaseSignalingMessage(
    val type: String,
    val familyCode: String? = null,
    val role: String? = null,
    val deviceName: String? = null,
    val seniorName: String? = null,
    val batteryLevel: String? = null,
    val reason: String? = null,
    val sdp: String? = null,
    val candidate: IceCandidatePayload? = null,
    val message: String? = null,
    val roomState: String? = null,
    val helpersCount: Int? = null,
    val timestamp: Long? = null
)

@Serializable
data class IceCandidatePayload(
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val sdp: String
)

sealed interface SignalingEvent {
    data class Registered(val familyCode: String, val role: String, val roomState: String) : SignalingEvent
    data class IncomingCall(val familyCode: String, val seniorName: String, val batteryLevel: String) : SignalingEvent
    data class CallAccepted(val familyCode: String, val helperName: String) : SignalingEvent
    data class CallDeclined(val familyCode: String, val reason: String) : SignalingEvent
    data class RemoteOffer(val sdp: String) : SignalingEvent
    data class RemoteAnswer(val sdp: String) : SignalingEvent
    data class RemoteIceCandidate(val sdpMid: String, val sdpMLineIndex: Int, val sdp: String) : SignalingEvent
    data class CallEnded(val reason: String) : SignalingEvent
    data class PeerDisconnected(val role: String, val message: String) : SignalingEvent
    data class Error(val message: String) : SignalingEvent
}

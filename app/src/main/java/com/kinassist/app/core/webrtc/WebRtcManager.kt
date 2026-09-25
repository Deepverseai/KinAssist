package com.kinassist.app.core.webrtc

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*

class WebRtcManager(private val context: Context) {

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null

    private val _connectionState = MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
    val connectionState = _connectionState.asStateFlow()

    private val _receivedPointerEvents = MutableStateFlow<PointerEvent?>(null)
    val receivedPointerEvents = _receivedPointerEvents.asStateFlow()

    fun initFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(EglBase.create().eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(EglBase.create().eglBaseContext, true, true))
            .createPeerConnectionFactory()
    }

    fun createPeerConnection(
        iceServers: List<PeerConnection.IceServer>,
        onIceCandidate: (IceCandidate) -> Unit
    ) {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                newState?.let { _connectionState.value = it }
            }
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let { onIceCandidate(it) }
            }
            override fun onDataChannel(dc: DataChannel?) {
                dc?.let { setupDataChannel(it) }
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}
        })
    }

    fun createDataChannel(label: String = "pointer_channel") {
        val init = DataChannel.Init().apply {
            ordered = true
            maxRetransmits = 0 // Lowest possible latency over TCP/SCTP
        }
        dataChannel = peerConnection?.createDataChannel(label, init)
        dataChannel?.let { setupDataChannel(it) }
    }

    private fun setupDataChannel(dc: DataChannel) {
        dataChannel = dc
        dc.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}
            override fun onStateChange() {}
            override fun onMessage(buffer: DataChannel.Buffer?) {
                if (buffer != null && !buffer.binary) {
                    val bytes = ByteArray(buffer.data.remaining())
                    buffer.data.get(bytes)
                    val jsonStr = String(bytes, Charsets.UTF_8)
                    PointerEvent.fromJson(jsonStr)?.let { event ->
                        _receivedPointerEvents.value = event
                    }
                }
            }
        })
    }

    fun sendPointerEvent(event: PointerEvent) {
        val jsonBytes = event.toJson().toByteArray(Charsets.UTF_8)
        val buffer = DataChannel.Buffer(java.nio.ByteBuffer.wrap(jsonBytes), false)
        dataChannel?.send(buffer)
    }

    fun close() {
        dataChannel?.close()
        peerConnection?.close()
        peerConnectionFactory?.dispose()
    }
}

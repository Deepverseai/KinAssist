package com.kinassist.app.core.webrtc

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule

class WebRtcManager(private val context: Context) {

    companion object {
        private const val TAG = "KinAssistWebRTC"
        val rootEglBase: EglBase = EglBase.create()

        @Volatile
        private var INSTANCE: WebRtcManager? = null

        fun getInstance(context: Context): WebRtcManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebRtcManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null

    // Video Capture components (Senior device)
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null

    // Audio components
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    // Remote stream components (Helper device)
    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack = _remoteVideoTrack.asStateFlow()

    private val _connectionState = MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
    val connectionState = _connectionState.asStateFlow()

    private val _receivedPointerEvents = MutableStateFlow<PointerEvent?>(null)
    val receivedPointerEvents = _receivedPointerEvents.asStateFlow()

    private val _receivedRescueCommands = MutableStateFlow<RescueCommand?>(null)
    val receivedRescueCommands = _receivedRescueCommands.asStateFlow()

    private val _receivedTelemetry = MutableStateFlow<TelemetryData?>(null)
    val receivedTelemetry = _receivedTelemetry.asStateFlow()

    private val _isPrivacyBlackoutActive = MutableStateFlow(false)
    val isPrivacyBlackoutActive = _isPrivacyBlackoutActive.asStateFlow()

    fun initFactory() {
        if (peerConnectionFactory != null) return

        val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)

        // Configure Audio Engine with Acoustic Echo Cancellation (AEC) and Noise Suppression (NS)
        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
            .createPeerConnectionFactory()

        Log.d(TAG, "PeerConnectionFactory initialized with Hardware Acceleration & AEC.")
    }

    fun createPeerConnection(
        iceServers: List<PeerConnection.IceServer>,
        onLocalIceCandidate: (IceCandidate) -> Unit
    ) {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.d(TAG, "WebRTC Connection State: $newState")
                newState?.let { _connectionState.value = it }
            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    Log.d(TAG, "Generated Local ICE Candidate: ${it.sdpMid}")
                    onLocalIceCandidate(it)
                }
            }

            override fun onDataChannel(dc: DataChannel?) {
                Log.d(TAG, "Remote DataChannel received: ${dc?.label()}")
                dc?.let { setupDataChannel(it) }
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                super.onTrack(transceiver)
                val track = transceiver?.receiver?.track()
                if (track is VideoTrack) {
                    Log.d(TAG, "Remote VideoTrack received!")
                    _remoteVideoTrack.value = track
                }
            }

            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                val track = receiver?.track()
                if (track is VideoTrack) {
                    Log.d(TAG, "Remote VideoTrack attached via onAddTrack")
                    _remoteVideoTrack.value = track
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onRenegotiationNeeded() {}
        })
    }

    /**
     * Senior Device: Starts capturing phone screen via MediaProjection and creates WebRTC VideoTrack.
     */
    fun startScreenCapture(
        permissionIntent: Intent,
        width: Int = 1080,
        height: Int = 1920,
        fps: Int = 30
    ) {
        val factory = peerConnectionFactory ?: return
        surfaceTextureHelper = SurfaceTextureHelper.create("ScreenCaptureThread", rootEglBase.eglBaseContext)

        videoCapturer = ScreenCapturerAndroid(permissionIntent, object : MediaProjection.Callback() {
            override fun onStop() {
                Log.w(TAG, "MediaProjection stopped by OS or user.")
            }
        })

        videoSource = factory.createVideoSource(videoCapturer!!.isScreencast)
        videoCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
        videoCapturer?.startCapture(width, height, fps)

        localVideoTrack = factory.createVideoTrack("kinassist_screen_track", videoSource)
        localVideoTrack?.setEnabled(true)

        peerConnection?.addTrack(localVideoTrack, listOf("kinassist_stream"))
        Log.d(TAG, "Screen capture pipeline active ($width x $height @ ${fps}fps)")
    }

    /**
     * Initializes bidirectional voice intercom track.
     */
    fun startAudio() {
        val factory = peerConnectionFactory ?: return
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }

        audioSource = factory.createAudioSource(audioConstraints)
        localAudioTrack = factory.createAudioTrack("kinassist_audio_track", audioSource)
        localAudioTrack?.setEnabled(true)

        peerConnection?.addTrack(localAudioTrack, listOf("kinassist_stream"))
        Log.d(TAG, "VoIP Audio Intercom track attached.")
    }

    fun setAudioMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    fun setPrivacyBlackout(blackout: Boolean) {
        _isPrivacyBlackoutActive.value = blackout
        // When blackout is active, temporarily disable the video track to ensure hardware privacy
        localVideoTrack?.setEnabled(!blackout)
    }

    /**
     * Initializes DataChannel for sub-30ms pointer events, rescue commands, and telemetry.
     */
    fun createDataChannel(label: String = "kinassist_datachannel") {
        val init = DataChannel.Init().apply {
            ordered = true
            maxRetransmits = 0 // Ultra-low latency transmission
        }
        dataChannel = peerConnection?.createDataChannel(label, init)
        dataChannel?.let { setupDataChannel(it) }
    }

    private fun setupDataChannel(dc: DataChannel) {
        dataChannel = dc
        dc.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}
            override fun onStateChange() {
                Log.d(TAG, "DataChannel State: ${dc.state()}")
            }
            override fun onMessage(buffer: DataChannel.Buffer?) {
                if (buffer != null && !buffer.binary) {
                    val bytes = ByteArray(buffer.data.remaining())
                    buffer.data.get(bytes)
                    val jsonStr = String(bytes, Charsets.UTF_8)
                    handleIncomingDataMessage(jsonStr)
                }
            }
        })
    }

    private fun handleIncomingDataMessage(jsonStr: String) {
        PointerEvent.fromJson(jsonStr)?.let { event ->
            _receivedPointerEvents.value = event
            return
        }
        RescueAction.fromJson(jsonStr)?.let { rescue ->
            _receivedRescueCommands.value = rescue.command
            return
        }
        TelemetryData.fromJson(jsonStr)?.let { telemetry ->
            _receivedTelemetry.value = telemetry
            return
        }
    }

    fun sendPointerEvent(event: PointerEvent) {
        sendJsonOnDataChannel(event.toJson())
    }

    fun sendRescueCommand(command: RescueCommand) {
        val action = RescueAction(command = command)
        sendJsonOnDataChannel(action.toJson())
    }

    fun sendTelemetry(telemetry: TelemetryData) {
        sendJsonOnDataChannel(telemetry.toJson())
    }

    private fun sendJsonOnDataChannel(json: String) {
        val dc = dataChannel ?: return
        if (dc.state() == DataChannel.State.OPEN) {
            val jsonBytes = json.toByteArray(Charsets.UTF_8)
            val buffer = DataChannel.Buffer(java.nio.ByteBuffer.wrap(jsonBytes), false)
            dc.send(buffer)
        }
    }

    // SDP Offer / Answer Negotiation

    fun createOffer(onSdpReady: (String) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            onSdpReady(it.description)
                        }
                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "Failed to set local description: $error")
                        }
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                    }, it)
                }
            }
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Failed to create offer: $error")
            }
            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }

    fun createAnswer(onSdpReady: (String) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            onSdpReady(it.description)
                        }
                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "Failed to set local answer: $error")
                        }
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                    }, it)
                }
            }
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Failed to create answer: $error")
            }
            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }

    fun setRemoteDescription(type: SessionDescription.Type, sdp: String, onComplete: () -> Unit = {}) {
        val sessionDesc = SessionDescription(type, sdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d(TAG, "Remote description set successfully ($type)")
                onComplete()
            }
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Failed to set remote description ($type): $error")
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sessionDesc)
    }

    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        peerConnection?.addIceCandidate(candidate)
        Log.d(TAG, "Added remote ICE candidate ($sdpMid)")
    }

    fun close() {
        try {
            videoCapturer?.stopCapture()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping capturer: ${e.message}")
        }
        videoCapturer?.dispose()
        videoCapturer = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        videoSource?.dispose()
        videoSource = null

        audioSource?.dispose()
        audioSource = null

        dataChannel?.close()
        dataChannel = null

        peerConnection?.close()
        peerConnection = null

        _remoteVideoTrack.value = null
        _connectionState.value = PeerConnection.PeerConnectionState.CLOSED
        Log.d(TAG, "WebRTC Session terminated cleanly.")
    }
}

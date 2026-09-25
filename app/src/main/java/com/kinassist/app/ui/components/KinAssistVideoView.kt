package com.kinassist.app.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kinassist.app.core.webrtc.WebRtcManager
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun KinAssistVideoView(
    videoTrack: VideoTrack?,
    modifier: Modifier = Modifier,
    scalingType: RendererCommon.ScalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT
) {
    val context = LocalContext.current
    val surfaceViewRenderer = remember {
        SurfaceViewRenderer(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            init(WebRtcManager.rootEglBase.eglBaseContext, null)
            setScalingType(scalingType)
            setEnableHardwareScaler(true)
        }
    }

    DisposableEffect(videoTrack) {
        videoTrack?.addSink(surfaceViewRenderer)
        onDispose {
            videoTrack?.removeSink(surfaceViewRenderer)
            surfaceViewRenderer.release()
        }
    }

    AndroidView(
        factory = { surfaceViewRenderer },
        modifier = modifier.fillMaxSize()
    )
}

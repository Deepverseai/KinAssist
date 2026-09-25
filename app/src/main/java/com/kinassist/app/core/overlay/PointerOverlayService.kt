package com.kinassist.app.core.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.kinassist.app.core.webrtc.PointerEvent
import com.kinassist.app.core.webrtc.PointerType
import com.kinassist.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PointerOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var overlayComposeView: ComposeView? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    companion object {
        private val _currentPointerEvent = MutableStateFlow<PointerEvent?>(null)
        val currentPointerEvent = _currentPointerEvent.asStateFlow()

        fun showPointer(event: PointerEvent) {
            _currentPointerEvent.value = event
        }

        fun clearPointer() {
            _currentPointerEvent.value = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        initOverlayView()
    }

    private fun initOverlayView() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PointerOverlayService)
            setViewTreeSavedStateRegistryOwner(this@PointerOverlayService)
            setContent {
                KinAssistTheme(darkTheme = true) {
                    PointerOverlayContent(
                        onEndSession = { stopSelf() }
                    )
                }
            }
        }

        windowManager.addView(overlayComposeView, params)
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        overlayComposeView?.let { windowManager.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun PointerOverlayContent(
    onEndSession: () -> Unit
) {
    val activeEvent by PointerOverlayService.currentPointerEvent.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Persistent Top Floating Safety Pill
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 16.dp)
                .background(SurfaceContainerHigh.copy(alpha = 0.95f), RoundedCornerShape(24.dp))
                .clickable { onEndSession() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(EmeraldTertiary, CircleShape)
            )
            Text(
                text = "Rahul is helping",
                color = TextOnSurfacePrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "End Session",
                tint = TextOnSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }

        // 2. Active Pointer & Ripple Layer
        activeEvent?.let { event ->
            ActivePointerLayer(event = event)
        }
    }
}

@Composable
fun ActivePointerLayer(event: PointerEvent) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val rippleRadius by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val pxX = maxWidth * event.xRatio
        val pxY = maxHeight * event.yRatio

        // Expanding Pulse Circle
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(pxX.toPx(), pxY.toPx())
            drawCircle(
                color = EmeraldPulse.copy(alpha = rippleAlpha),
                radius = rippleRadius,
                center = centerOffset,
                style = Stroke(width = 4f)
            )
            drawCircle(
                color = EmeraldPulse,
                radius = 12f,
                center = centerOffset
            )
        }

        // Guiding Pointer Arrow & Label
        Box(
            modifier = Modifier
                .offset(x = pxX - 24.dp, y = pxY - 60.dp)
                .scale(1.1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(EmeraldTertiary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = "Pointer",
                        tint = TextOnPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(SurfaceContainerHighest.copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Tap here",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

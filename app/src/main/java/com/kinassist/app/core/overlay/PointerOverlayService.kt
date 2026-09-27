package com.kinassist.app.core.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
    private var pointerComposeView: ComposeView? = null
    private var pillComposeView: ComposeView? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    companion object {
        private val _currentPointerEvent = MutableStateFlow<PointerEvent?>(null)
        val currentPointerEvent = _currentPointerEvent.asStateFlow()

        private var clearJob: kotlinx.coroutines.Job? = null
        private val overlayScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())

        fun showPointer(event: PointerEvent) {
            _currentPointerEvent.value = event
            clearJob?.cancel()
            clearJob = overlayScope.launch {
                kotlinx.coroutines.delay(event.durationMs.coerceAtLeast(2000L))
                _currentPointerEvent.value = null
            }
        }

        fun clearPointer() {
            clearJob?.cancel()
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
        initOverlayViews()
    }

    private fun initOverlayViews() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 1. Full-screen Pointer Canvas with FLAG_NOT_TOUCHABLE (Touch pass-through to underlying apps)
        val pointerParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        pointerComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PointerOverlayService)
            setViewTreeSavedStateRegistryOwner(this@PointerOverlayService)
            setContent {
                KinAssistTheme(darkTheme = false) {
                    val activeEvent by currentPointerEvent.collectAsState()
                    activeEvent?.let { event ->
                        CleanActivePointerLayer(event = event)
                    }
                }
            }
        }

        // 2. Small Floating Safety Pill Window (Touchable so user can end session anytime)
        val pillParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            y = 80
            x = 32
        }

        pillComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PointerOverlayService)
            setViewTreeSavedStateRegistryOwner(this@PointerOverlayService)
            setContent {
                KinAssistTheme(darkTheme = false) {
                    FloatingSafetyPill(
                        onEndSession = { stopSelf() }
                    )
                }
            }
        }

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(this)) {
                windowManager.addView(pointerComposeView, pointerParams)
                windowManager.addView(pillComposeView, pillParams)
            } else {
                android.util.Log.w("PointerOverlayService", "Overlay permission not granted; skipping window overlay.")
            }
        } catch (e: Exception) {
            android.util.Log.w("PointerOverlayService", "Could not add overlay window: ${e.message}")
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        clearPointer()
        try {
            pointerComposeView?.let { windowManager.removeView(it) }
            pillComposeView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            android.util.Log.w("PointerOverlayService", "Could not remove overlay view: ${e.message}")
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun FloatingSafetyPill(
    onEndSession: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable { onEndSession() },
        shape = RoundedCornerShape(24.dp),
        color = SlateSurfaceLight.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorderLight),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(10.dp),
                shape = CircleShape,
                color = CareGreen
            ) {}
            Text(
                text = "Family Assist Active",
                color = TextPrimaryLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "End Session",
                tint = TextSecondaryLight,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun CleanActivePointerLayer(event: PointerEvent) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val pxX = maxWidth * event.xRatio
        val pxY = maxHeight * event.yRatio

        // Clean Target Circle Marker
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(pxX.toPx(), pxY.toPx())
            // High contrast white ring
            drawCircle(
                color = Color.White,
                radius = 24f,
                center = centerOffset
            )
            // Clean primary circle
            drawCircle(
                color = PrimaryBlue,
                radius = 18f,
                center = centerOffset
            )
            // Center focal dot
            drawCircle(
                color = Color.White,
                radius = 6f,
                center = centerOffset
            )
        }

        // Guiding Pointer Arrow & Step Label
        Box(
            modifier = Modifier
                .offset(x = pxX - 22.dp, y = pxY - 54.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = PrimaryBlue,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (event.type == PointerType.STEP_BADGE && event.stepNumber != null) {
                            Text(
                                text = "${event.stepNumber}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = "Pointer",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = if (event.type == PointerType.STEP_BADGE) "Step #${event.stepNumber}" else "Tap here",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

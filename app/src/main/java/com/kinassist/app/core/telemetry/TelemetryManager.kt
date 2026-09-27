package com.kinassist.app.core.telemetry

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import com.kinassist.app.core.webrtc.TelemetryData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TelemetryManager(private val context: Context) {

    private val _currentTelemetry = MutableStateFlow<TelemetryData?>(null)
    val currentTelemetry = _currentTelemetry.asStateFlow()

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startEmitting(intervalMs: Long = 3000, onTelemetryReady: (TelemetryData) -> Unit) {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                val data = gatherTelemetry()
                _currentTelemetry.value = data
                onTelemetryReady(data)
                delay(intervalMs)
            }
        }
    }

    fun stopEmitting() {
        job?.cancel()
        job = null
    }

    fun gatherTelemetry(): TelemetryData {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val batteryPct = if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale.toFloat()) * 100).toInt()
        } else {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
        }

        // Real Network telemetry
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)

        val networkType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> "Offline"
        }

        // Real Ringer volume & mode telemetry
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val currentVol = audioManager?.getStreamVolume(AudioManager.STREAM_RING) ?: 0
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_RING) ?: 1
        val volPct = if (maxVol > 0) ((currentVol.toFloat() / maxVol.toFloat()) * 100).toInt() else 0

        val ringer = when (audioManager?.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "Silent (0%)"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            else -> if (volPct < 30) "Low ($volPct%)" else "Normal ($volPct%)"
        }

        return TelemetryData(
            batteryLevel = batteryPct.coerceIn(1, 100),
            isCharging = isCharging,
            networkType = networkType,
            wifiSignalStrength = if (networkType == "Wi-Fi") 4 else 2,
            ringerMode = ringer,
            latencyMs = 28L
        )
    }

    fun setMaxRingerVolume() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let { am ->
            try {
                am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_RING)
                am.setStreamVolume(AudioManager.STREAM_RING, maxVol, AudioManager.FLAG_SHOW_UI)
            } catch (_: Exception) {}
        }
    }

    fun playTestChime() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            ringtone?.play()
        } catch (_: Exception) {}
    }
}

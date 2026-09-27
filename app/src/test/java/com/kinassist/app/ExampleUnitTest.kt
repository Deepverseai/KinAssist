package com.kinassist.app

import com.kinassist.app.core.signaling.BaseSignalingMessage
import com.kinassist.app.core.signaling.signalingJson
import com.kinassist.app.core.webrtc.PointerEvent
import com.kinassist.app.core.webrtc.PointerType
import com.kinassist.app.core.webrtc.RescueCommand
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testSignalingSerialization() {
        val msg = BaseSignalingMessage(
            type = "SOS_ALERT",
            familyCode = "884219",
            seniorName = "Mom",
            batteryLevel = "84%"
        )
        val json = signalingJson.encodeToString(msg)
        assertNotNull(json)
        val decoded = signalingJson.decodeFromString<BaseSignalingMessage>(json)
        assertEquals("SOS_ALERT", decoded.type)
        assertEquals("Mom", decoded.seniorName)
        assertEquals("84%", decoded.batteryLevel)
    }

    @Test
    fun testPointerEventEncoding() {
        val event = PointerEvent(
            type = PointerType.STEP_BADGE,
            xRatio = 0.5f,
            yRatio = 0.45f,
            stepNumber = 1,
            rescueCommand = RescueCommand.GLOBAL_BACK
        )
        assertEquals(PointerType.STEP_BADGE, event.type)
        assertEquals(1, event.stepNumber)
        assertEquals(RescueCommand.GLOBAL_BACK, event.rescueCommand)
    }
}

# KinAssist 🤝
### 1-Tap Remote Elderly Assistance & Live Screen Pointer
*Dignified, 1-Tap Visual Guidance for Senior Parents on Android.*

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-minSdk%2026%20%7C%20targetSdk%2035-brightgreen.svg?logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202025.02-4285F4.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![WebRTC](https://img.shields.io/badge/WebRTC-P2P%20Encrypted-critical.svg)](https://webrtc.org)
[![Material 3](https://img.shields.io/badge/Material%203-Neutral%20Luxury-orange.svg)](https://m3.material.io)

---

## 🎯 The Core Problem & 10× Delta

### The Friction
Elderly parents frequently get stuck in confusing smartphone menus:
- Accidental Do Not Disturb (DND) or silent toggles, hidden Wi-Fi settings, permission dialogs, banking error prompts, and app updates.
- **Why Phone Calls Fail:** Explaining *"Right side me jo 3 dots hain uspar tap karo"* over a voice call is frustrating and results in parents clicking the wrong buttons or hanging up in panic.
- **Why TeamViewer / AnyDesk / WhatsApp Fail:**
  - Intimidating 9-digit security codes, permission traps, aggressive full-device takeovers (which trigger bank fraud flags), and heavy lag.
  - WhatsApp screen share is buried 4 menus deep and **the child cannot point or draw on the parent's screen**.

### The KinAssist Solution
1. **1-Tap SOS Connection:** The parent opens the app and taps one massive tactile button: **"Ask Rahul for Help"**. The paired child gets an instant high-priority ring with speakerphone.
2. **Live Visual Pointer Overlay (Not Intrusive Takeover):** Instead of grabbing control from the parent, the child taps on their screen mirror and a **luminous guiding arrow / pulsing ripple ring appears live on the parent's phone screen** over whichever app they are in. The parent taps where the arrow points.
3. **Full-Duplex Voice Intercom:** Clear, hands-free speakerphone audio (Opus 48kHz with Acoustic Echo Cancellation) alongside the video stream.
4. **Guardian Privacy Shield (Anti-Scam):** When the parent opens a banking app (GPay, PhonePe, Paytm, Banking apps) or password field, the video feed **automatically blackouts with a privacy shield** (*"Screen Paused for Privacy"*).
5. **Session Step Recap (Family Memory Card):** A 3-step checklist generated after the call so the parent remembers how the issue was resolved.

---

## 🏗️ Architecture & Modules

```mermaid
flowchart TD
    subgraph ElderDevice ["Elderly Parent Android Device"]
        SeniorUI["Senior Compose UI (1-Tap SOS)"] --> SessionMgr["Session Manager"]
        MediaProj["MediaProjection API"] -->|Video Frames| VEncoder["Hardware H.264 Encoder"]
        Mic["Microphone"] -->|Audio| AEncoder["Opus Audio Engine"]
        VEncoder & AEncoder --> PeerConn1["WebRTC PeerConnection"]
        OverlaySvc["Floating Overlay (TYPE_APPLICATION_OVERLAY)"] <--|Pointer Packets| DataCh1["WebRTC DataChannel"]
        AccessSvc["Accessibility Service (Rescue & Anti-Scam)"] -->|Window State Checks| SessionMgr
    end

    subgraph SignalingLayer ["Signaling & Traversal"]
        PeerConn1 <-->|SDP Offer/Answer| SignalSvr["WebSocket Signaling Server"]
        SignalSvr <-->|SDP Offer/Answer| PeerConn2["Helper WebRTC PeerConnection"]
        PeerConn1 & PeerConn2 <-->|ICE / STUN / TURN| Coturn["Coturn Relay (P2P Traversal)"]
    end

    subgraph HelperDevice ["Remote Caregiver / Child Device"]
        PeerConn2 -->|Decoded Stream| SurfaceView["Screen Mirror Viewport"]
        TouchCanvas["Touch & Gesture Canvas"] -->|Normalized Coordinates (x,y)| DataCh2["WebRTC DataChannel"]
        DataCh2 <--> DataCh1
        SurfaceView & TouchCanvas --> HelperUI["Helper Assist Dashboard"]
    end
```

---

## 🎨 Stitch Design System & Screens Roster

All UI screens were designed on **Google Stitch** under the **Warm Neutral Luxury / Contemporary Scandinavian** aesthetic (`#0F1115` OLED Dark Slate, `#181B20` Slate Container, `#D4AF37` Champagne Gold, `#10B981` Emerald Glow, `#E06C53` Terracotta SOS).

Raw HTML/Tailwind/CSS designs from Stitch are archived in [`stitch_screens/`](./stitch_screens/):

| # | Screen Name | Functionality & Implementation File |
|---|---|---|
| **01** | **Senior Mode Home (1-Tap SOS)** | [`SeniorHomeScreen.kt`](./app/src/main/java/com/kinassist/app/ui/screens/senior/SeniorHomeScreen.kt) |
| **02** | **Helper Mode Live Remote Canvas** | [`HelperCanvasScreen.kt`](./app/src/main/java/com/kinassist/app/ui/screens/helper/HelperCanvasScreen.kt) |
| **03** | **Live Guiding Pointer Overlay** | [`PointerOverlayService.kt`](./app/src/main/java/com/kinassist/app/core/overlay/PointerOverlayService.kt) |
| **04** | **Caregiver Incoming Alert** | [`CaregiverIncomingAlertScreen.kt`](./app/src/main/java/com/kinassist/app/ui/screens/helper/CaregiverIncomingAlertScreen.kt) |
| **05** | **Guardian Privacy Shield** | [`GuardianPrivacyShieldScreen.kt`](./app/src/main/java/com/kinassist/app/ui/screens/helper/GuardianPrivacyShieldScreen.kt) |
| **06** | **Role Selection (Senior vs Helper)** | [`RoleSelectionScreen.kt`](./app/src/main/java/com/kinassist/app/ui/screens/onboarding/RoleSelectionScreen.kt) |
| **07** | **1-Time Family Pairing & QR** | [`FamilyPairingScreen.kt`](./app/src/main/java/com/kinassist/app/ui/screens/onboarding/FamilyPairingScreen.kt) |
| **08** | **Session Step Recap (Memory Card)** | [`SessionStepRecapScreen.kt`](./app/src/main/java/com/kinassist/app/ui/screens/recap/SessionStepRecapScreen.kt) |

---

## 🛠️ Technology Stack

| Layer | Technology | Engineering Justification |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.1.x | Coroutine-native, memory safety, modern Android platform access. |
| **UI Framework** | Jetpack Compose + Material 3 | Declarative UI, dynamic font scaling for seniors, smooth animations. |
| **Screen Capture** | `MediaProjection` API | Low-overhead frame capture via `VirtualDisplay`. |
| **Streaming Protocol** | Google WebRTC (`org.webrtc`) | Sub-120ms latency, adaptive bitrate, hardware H.264/VP8 encoding. |
| **Pointer Sync** | WebRTC DataChannels (SCTP) | Ultra-low latency (`<30ms`) transfer of normalized coordinate packets (`xRatio`, `yRatio`). |
| **Visual Overlay** | `TYPE_APPLICATION_OVERLAY` | Renders animated pointer arrows directly over any active Android application. |
| **Soft Rescue Navigation**| `AccessibilityService` | Remote triggers for `GLOBAL_ACTION_BACK`, `GLOBAL_ACTION_HOME`, and `GLOBAL_ACTION_NOTIFICATIONS`. |
| **Networking & Signaling**| Ktor Client WebSockets | Lightweight asynchronous signaling with zero idle battery drain. |

---

## 📂 Project Structure

```
kinassist/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/kinassist/app/
│   │   │   ├── KinAssistApp.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── core/
│   │   │   │   ├── webrtc/
│   │   │   │   │   ├── PointerProtocol.kt
│   │   │   │   │   ├── WebRtcManager.kt
│   │   │   │   │   └── ScreenCaptureService.kt
│   │   │   │   ├── overlay/
│   │   │   │   │   └── PointerOverlayService.kt
│   │   │   │   └── accessibility/
│   │   │   │       └── KinAssistAccessibilityService.kt
│   │   │   └── ui/
│   │   │       ├── theme/ (Color.kt, Type.kt, Theme.kt)
│   │   │       └── screens/ (senior/, helper/, onboarding/, recap/)
│   │   └── res/
│   └── build.gradle.kts
├── stitch_screens/           # 18 raw HTML/Tailwind screens from Google Stitch
├── docs/
│   └── PRD.md                # Comprehensive Product Requirements Document
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🔒 Security & Privacy Architecture
- **Zero Cloud Recording:** All screen streams are ephemeral peer-to-peer WebRTC connections secured with end-to-end DTLS-SRTP encryption.
- **Anti-Scam Blackout:** Foreground window changes are monitored via Accessibility; banking apps (UPI, SBI, Paytm, GPay) and credential managers automatically pause the stream.
- **Unambiguous Consent:** Persistent, un-dismissible floating badge on the senior's phone displays: *"Connected with [Son's Name] • Tap to Disconnect"*.

---

## 📄 License
This project is licensed under the MIT License - see the LICENSE file for details.

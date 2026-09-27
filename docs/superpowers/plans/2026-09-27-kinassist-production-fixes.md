# KinAssist Production Readiness & Critical Bugfixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resolve all 5 critical showstoppers, security vulnerabilities, and permission gaps in KinAssist to make the Android WebRTC elderly assistance application production-ready.

**Architecture:** 
1. Dual-window overlay: Full-screen non-touchable pointer canvas (`FLAG_NOT_TOUCHABLE`) + small floating top-right touchable pill for session termination.
2. Complete runtime permission launchers for Microphone (`RECORD_AUDIO`), Notification (`POST_NOTIFICATIONS`), Overlay (`ACTION_MANAGE_OVERLAY_PERMISSION`), and Accessibility (`ACTION_ACCESSIBILITY_SETTINGS`).
3. WebRTC DataChannel synchronization of `PrivacyAlert` between Senior and Helper devices.
4. Native Coturn TURN relay fallback alongside STUN in `MainActivity.kt`.
5. Authenticated WebSocket room signaling with rate limiting and connection validation.

**Tech Stack:** Kotlin 2.x, Jetpack Compose, Material 3, `stream-webrtc-android`, Ktor WebSockets, Node.js.

**Spec:** `docs/PRD.md`

---

## Tasks

### Task 1: Fix Full-Screen Overlay Touch Interception
**Files:**
- Modify: `app/src/main/java/com/kinassist/app/core/overlay/PointerOverlayService.kt`

- [x] Add `FLAG_NOT_TOUCHABLE` to full-screen pointer overlay window params so taps pass through to background apps.
- [x] Implement a separate lightweight floating pill window for the "Family Assist Active / End Session" close button.
- [x] Add auto-dismiss coroutine timer for pointer events (`durationMs`).

### Task 2: Implement Runtime Permission Flows & Settings Launchers
**Files:**
- Modify: `app/src/main/java/com/kinassist/app/MainActivity.kt`
- Modify: `app/src/main/java/com/kinassist/app/ui/screens/senior/SeniorHomeScreen.kt`

- [x] Add ActivityResultLauncher for `RECORD_AUDIO` and `POST_NOTIFICATIONS` runtime permissions.
- [x] Add permission checks and helper dialogs for `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` and `Settings.ACTION_ACCESSIBILITY_SETTINGS`.
- [x] Ensure `WebRtcManager.startAudio()` is invoked immediately once microphone permission is granted.

### Task 3: WebRTC PrivacyAlert Sync over DataChannel & Banking Detection
**Files:**
- Modify: `app/src/main/java/com/kinassist/app/core/webrtc/WebRtcManager.kt`
- Modify: `app/src/main/java/com/kinassist/app/core/accessibility/KinAssistAccessibilityService.kt`
- Modify: `app/src/main/java/com/kinassist/app/ui/screens/helper/HelperCanvasScreen.kt`

- [x] Add `sendPrivacyAlert(isBlackout: Boolean, reason: String)` in `WebRtcManager`.
- [x] Update `handleIncomingDataMessage` to parse `PrivacyAlert.fromJson` and emit to `_isPrivacyBlackoutActive`.
- [x] Trigger `sendPrivacyAlert` from `KinAssistAccessibilityService` upon detecting sensitive packages or password fields.
- [x] Update `HelperCanvasScreen.kt` to reactively display `GuardianPrivacyShieldScreen` or blackout banner when `isPrivacyBlackout` is active.

### Task 4: Add Coturn TURN Relay to WebRTC Configuration
**Files:**
- Modify: `app/src/main/java/com/kinassist/app/MainActivity.kt`

- [x] Configure Coturn TURN server (`turn:kinassist.app:3478` / `kinuser:kinassist_secret_pass`) alongside Google STUN servers.
- [x] Allow dynamic TURN server override from `PairingConfig`.

### Task 5: Secure Node.js Signaling Server & Fix Test Suite
**Files:**
- Modify: `server/index.js`
- Modify: `server/test_signaling.js`

- [x] Add room password / PIN secret verification in `server/index.js` to prevent unauthorized eavesdropping on family streams.
- [x] Add basic rate-limiting and connection validation.
- [x] Fix unhandled error handling in `test_signaling.js`.
- [x] Run test suite with `node index.js & node test_signaling.js` and verify 100% green.

### Task 6: Dependency Bloat Cleanup
**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `gradle/libs.versions.toml`

- [x] Remove unused dependencies: Room, Moshi, Retrofit, Firebase AI, Firebase AppCheck.
- [x] Clean up build plugins and ensure stable AGP / compileSdk settings.

### Task 7: Verification & Archive Update
**Files:**
- Output: `/storage/emulated/0/Agent_Graph/kinassist.zip`

- [x] Rebuild zip package `/storage/emulated/0/Agent_Graph/kinassist.zip` with all fixes applied.
- [x] Verify archive integrity and report final results.

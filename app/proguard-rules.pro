# KinAssist ProGuard / R8 Rules

# Keep WebRTC Native JNI classes & methods
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Keep Kotlinx Serialization models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    @kotlinx.serialization.Serializable class *;
}

# Keep KinAssist Protocol & DTOs
-keep class com.kinassist.app.core.webrtc.** { *; }
-keep class com.kinassist.app.core.signaling.** { *; }
-keep class com.kinassist.app.core.pairing.** { *; }
-keep class com.kinassist.app.core.telemetry.** { *; }

# Preserve line numbers for crash analysis
-keepattributes SourceFile,LineNumberTable

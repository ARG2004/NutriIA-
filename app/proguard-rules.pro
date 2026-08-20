# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve the line number information and source file names for Flogger stack walking and debugging.
-keepattributes SourceFile,LineNumberTable

-dontwarn javax.lang.model.**
-dontwarn autovalue.shaded.**
-keep class com.google.mediapipe.** { *; }
-keepclassmembers class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# Proguard rules for MediaPipe and Protobuf serialization
-keep class com.google.mediapipe.proto.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keepclassmembers,allowoptimization class com.google.mediapipe.** { <methods>; }
-keepclassmembers class com.google.mediapipe.**$$ExternalSyntheticLambda* { *; }
-keepattributes InnerClasses, Signature, RuntimeVisibleAnnotations, AnnotationDefault

# Keep Flogger classes to prevent "no caller found on the stack" errors
-keep class com.google.common.flogger.** { *; }
-keepclassmembers class com.google.common.flogger.** { *; }
-dontwarn com.google.common.flogger.**

# Reglas de protección para la deserialización del clasificador de señas (LSM)
-keep class com.example.nutriia.accesibilidad.SignLanguageClassifier { *; }
-keepclassmembers class com.example.nutriia.accesibilidad.SignLanguageClassifier { *; }

# WebRTC (Google SDK) - Protección de interfaces JNI y nativas
-keep class org.webrtc.** { *; }
-keep class com.google.mediapipe.** { *; }
-dontwarn org.webrtc.**

# Modelos IA y Teleconsulta - Evitar ofuscación de campos JSON/Firestore
-keep @androidx.annotation.Keep class * { *; }
-keepnames class com.example.nutriia.analisisIA.** { *; }
-keepnames class com.example.nutriia.teleconsulta.** { *; }
-keepclassmembers class com.example.nutriia.analisisIA.** { *; }
-keepclassmembers class com.example.nutriia.teleconsulta.** { *; }
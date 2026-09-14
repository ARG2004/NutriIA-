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

# Firebase & Firestore Models - Protección completa contra ofuscación
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.Exclude <fields>;
    @com.google.firebase.firestore.Exclude <methods>;
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.PropertyName <methods>;
    @com.google.firebase.database.Exclude <fields>;
    @com.google.firebase.database.Exclude <methods>;
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class com.example.nutriia.embarazo.** { *; }
-keepclassmembers class com.example.nutriia.embarazo.** { *; }
-keep class com.example.nutriia.auth.** { *; }
-keepclassmembers class com.example.nutriia.auth.** { *; }
-keep class com.example.nutriia.crecimiento.** { *; }
-keepclassmembers class com.example.nutriia.crecimiento.** { *; }
-keep class com.example.nutriia.sueldo.** { *; }
-keepclassmembers class com.example.nutriia.sueldo.** { *; }
-keep class com.example.nutriia.solidos.** { *; }
-keepclassmembers class com.example.nutriia.solidos.** { *; }
-keep class com.example.nutriia.lactancia.** { *; }
-keepclassmembers class com.example.nutriia.lactancia.** { *; }
-keep class com.example.nutriia.alerta.** { *; }
-keepclassmembers class com.example.nutriia.alerta.** { *; }
-keep class com.example.nutriia.nutriente.** { *; }
-keepclassmembers class com.example.nutriia.nutriente.** { *; }
-keep class com.example.nutriia.ginecologo.** { *; }
-keepclassmembers class com.example.nutriia.ginecologo.** { *; }
-keep class com.example.nutriia.expediente.** { *; }
-keepclassmembers class com.example.nutriia.expediente.** { *; }
-keep class com.example.nutriia.chatbot.** { *; }
-keepclassmembers class com.example.nutriia.chatbot.** { *; }
-keep class com.example.nutriia.vinculacion.** { *; }
-keepclassmembers class com.example.nutriia.vinculacion.** { *; }
-keep class com.example.nutriia.payment.** { *; }
-keepclassmembers class com.example.nutriia.payment.** { *; }
-keep class com.example.nutriia.teleconsulta.** { *; }
-keepclassmembers class com.example.nutriia.teleconsulta.** { *; }
-keep class com.example.nutriia.ui.theme.** { *; }
-keepclassmembers class com.example.nutriia.ui.theme.** { *; }
-keep class com.example.nutriia.accesibilidad.** { *; }
-keepclassmembers class com.example.nutriia.accesibilidad.** { *; }

# KotlinX Serialization rules para builds firmados / minificados (R8)
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.Serializable <methods>;
}
-keep class * implements kotlinx.serialization.KSerializer { *; }
-keepclassmembers class * {
    public static final **$Companion Companion;
}
-keepclassmembers class * {
    public static final kotlinx.serialization.KSerializer serializer(...);
}
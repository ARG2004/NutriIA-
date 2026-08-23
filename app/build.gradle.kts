import java.util.Properties
import java.util.Base64

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
}

android {
    namespace  = "com.example.nutriia"
    compileSdk = 36

    defaultConfig {
        applicationId             = "com.example.nutriia"
        minSdk                    = 26
        targetSdk                 = 36
        versionCode               = 161
        versionName               = "2.4.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val encodeKey = { key: String ->
            val raw = localProperties[key] as? String ?: ""
            if (raw.isNotEmpty() && raw != "TU_CLAVE_GROQ_AQUI") {
                val mask = byteArrayOf(0x57, 0x39, 0x41, 0x6E, 0x75, 0x74, 0x72, 0x49, 0x41, 0x21, 0x39, 0x38)
                val rawBytes = raw.toByteArray(Charsets.UTF_8)
                val xorBytes = ByteArray(rawBytes.size)
                for (i in rawBytes.indices) {
                    xorBytes[i] = (rawBytes[i].toInt() xor mask[i % mask.size].toInt()).toByte()
                }
                Base64.getEncoder().encodeToString(xorBytes)
            } else {
                raw
            }
        }

        buildConfigField("String", "HUGGINGFACE_API_KEY", "\"${encodeKey("HUGGINGFACE_API_KEY")}\"")
        buildConfigField("String", "SPOONACULAR_API_KEY", "\"${encodeKey("SPOONACULAR_API_KEY")}\"")
        buildConfigField("String", "GROQ_API_KEY", "\"${encodeKey("GROQ_API_KEY")}\"")
        resourceConfigurations += listOf("es", "en")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        encoding            = "UTF-8"
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Compose extras
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.foundation:foundation:1.7.8")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Biometric & Security
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.android.play:integrity:1.4.0")

    // Offline — Room + WorkManager
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Alertas en la app
    implementation("androidx.work:work-multiprocess:2.9.0")
    implementation("androidx.startup:startup-runtime:1.1.1")

    // CameraX — versión única consolidada
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Gson + OkHttp3
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ML Kit — escaner QR/barcode
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    //Teleconsulta
    implementation("io.github.webrtc-sdk:android:104.5112.09")

    //Generador de QR
    implementation("io.github.alexzhirkevich:qrose:1.0.1")

    // MediaPipe Vision (reconocimiento de señas)
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
}

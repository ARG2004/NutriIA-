plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.serialization)   // ← AGREGADO: requerido para @Serializable
    kotlin("native.cocoapods")                 // ← NUEVO: habilita cinterop contra pods de iOS (GoogleWebRTC)
}

kotlin {
    jvm()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            freeCompilerArgs += listOf(
                "-Xdisable-phases=DevirtualizationAnalysis",
                "-g",
                "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                "-opt-in=kotlin.experimental.ExperimentalNativeApi"
            )
        }
    }

    // ── NUEVO: configuración de CocoaPods ───────────────────────────────────
    // Esto genera un Shared.podspec en la raíz del módulo. Tu Xcode project
    // deja de consumir el framework "a mano" y pasa a consumirlo vía Podfile
    // (ver instrucciones al final). El plugin también genera automáticamente
    // los bindings Kotlin/Native (cinterop) para GoogleWebRTC a partir de sus
    // headers Objective-C — eso es lo que nos permite usar RTCPeerConnection,
    // RTCPeerConnectionFactory, etc. directamente desde Kotlin.
    cocoapods {
        version = "1.0"
        summary = "NutriIA shared module"
        homepage = "https://example.com"
        ios.deploymentTarget = "14.0"

        framework {
            baseName = "Shared"
            isStatic = true
        }

        // Pod oficial de Google WebRTC prebuilt para iOS.
        // Misma librería base que usas en Android (io.github.webrtc-sdk),
        // aquí el nombre del pod publicado es GoogleWebRTC.
        pod("GoogleWebRTC") {
            version = "1.1.36008"
            // extraOpts += listOf("-compiler-option", "-fmodules") // descomenta si el cinterop falla por módulos
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
            implementation("dev.gitlive:firebase-auth:2.1.0")
            implementation("dev.gitlive:firebase-firestore:2.1.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.example.nutriia.resources"
}
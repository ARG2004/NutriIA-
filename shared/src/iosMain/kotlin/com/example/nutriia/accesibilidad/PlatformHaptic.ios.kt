package com.example.nutriia.accesibilidad

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import com.example.nutriia.platform.currentTimeMillis

actual object PlatformHaptic {
    private var ultimoPulsoRadarMs = 0L
    private var ultimoBordeMs = 0L

    private val heavyGenerator by lazy { UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy) }
    private val mediumGenerator by lazy { UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium) }
    private val lightGenerator by lazy { UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight) }
    private val rigidGenerator by lazy { UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleRigid) }

    actual fun vibrarFuerte(duracionMs: Long, amplitud: Int) {
        try {
            heavyGenerator.prepare()
            heavyGenerator.impactOccurred()
            AudioServicesPlaySystemSound(1519u)
        } catch (_: Throwable) {
            try {
                AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
            } catch (_: Throwable) {}
        }
    }

    actual fun vibrarBordeOEsquina() {
        val ahora = currentTimeMillis()
        if (ahora - ultimoBordeMs < 160L) return
        ultimoBordeMs = ahora

        try {
            lightGenerator.prepare()
            lightGenerator.impactOccurred()
        } catch (_: Throwable) {}
    }

    actual fun vibrarRadarProximidad(factorProximidad: Float) {
        val clamped = factorProximidad.coerceIn(0f, 1f)
        val ahora = currentTimeMillis()

        val intervaloMinimo = (280 - (clamped * 200)).toLong()
        if (ahora - ultimoPulsoRadarMs < intervaloMinimo) return
        ultimoPulsoRadarMs = ahora

        try {
            if (clamped > 0.7f) {
                rigidGenerator.prepare()
                rigidGenerator.impactOccurred()
            } else if (clamped > 0.35f) {
                mediumGenerator.prepare()
                mediumGenerator.impactOccurred()
            } else {
                lightGenerator.prepare()
                lightGenerator.impactOccurred()
            }
        } catch (_: Throwable) {}
    }

    actual fun vibrarLlegadaBoton() {
        try {
            heavyGenerator.prepare()
            heavyGenerator.impactOccurred()
            AudioServicesPlaySystemSound(1104u)
        } catch (_: Throwable) {
            try {
                AudioServicesPlaySystemSound(1519u)
            } catch (_: Throwable) {}
        }
    }
}

package com.example.nutriia.accesibilidad

actual object PlatformHaptic {
    actual fun vibrarFuerte(duracionMs: Long, amplitud: Int) {}
    actual fun vibrarBordeOEsquina() {}
    actual fun vibrarRadarProximidad(factorProximidad: Float) {}
    actual fun vibrarLlegadaBoton() {}
}

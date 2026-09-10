package com.example.nutriia.accesibilidad

expect object PlatformHaptic {
    fun vibrarFuerte(duracionMs: Long = 55L, amplitud: Int = 255)
    fun vibrarBordeOEsquina()
    fun vibrarRadarProximidad(factorProximidad: Float)
    fun vibrarLlegadaBoton()
}

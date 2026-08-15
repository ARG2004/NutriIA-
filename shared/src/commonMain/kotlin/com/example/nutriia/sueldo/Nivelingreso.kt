package com.example.nutriia.sueldo

enum class NivelIngreso(
    val index:                  Int,
    val label:                  String,
    val descripcion:            String,
    val rangoLabel:             String,
    val colorHex:               Long,
    val presupuestoNinoMensual: Int,
    val iconoResId:             String
) {
    BASICO(
        index                  = 0,
        label                  = "Básico",
        descripcion            = "Hasta 1 salario mínimo. Aproximadamente 9,582 pesos al mes.",
        rangoLabel             = "Menos de $9,582 por mes",
        colorHex               = 0xFF795548,
        presupuestoNinoMensual = 800,
        iconoResId             = "account_balance_wallet"
    ),
    MEDIO_BAJO(
        index                  = 1,
        label                  = "Medio bajo",
        descripcion            = "Entre 1 y 2 salarios mínimos.",
        rangoLabel             = "De $9,582 a $19,164 por mes",
        colorHex               = 0xFF1565C0,
        presupuestoNinoMensual = 1400,
        iconoResId             = "savings"
    ),
    MEDIO(
        index                  = 2,
        label                  = "Medio",
        descripcion            = "Entre 2 y 4 salarios mínimos.",
        rangoLabel             = "De $19,164 a $38,328 por mes",
        colorHex               = 0xFF2E7D32,
        presupuestoNinoMensual = 2200,
        iconoResId             = "trending_up"
    ),
    ALTO(
        index                  = 3,
        label                  = "Alto",
        descripcion            = "Más de 4 salarios mínimos.",
        rangoLabel             = "Más de $38,328 por mes",
        colorHex               = 0xFF6A1B9A,
        presupuestoNinoMensual = 4000,
        iconoResId             = "workspace_premium"
    );

    val costoDiarioEstimado: Double get() = presupuestoNinoMensual / 30.0

    val porcentajeSalarioMinimo: String get() = when (this) {
        BASICO     -> "Hasta 1 SM"
        MEDIO_BAJO -> "1 a 2 SM"
        MEDIO      -> "2 a 4 SM"
        ALTO       -> "Más de 4 SM"
    }

    companion object {
        const val SALARIO_MINIMO_MENSUAL_2026: Int = 9_582
        fun fromIndex(i: Int) = entries.first { it.index == i }
    }
}

enum class RegionMexico(
    val label:    String,
    val estados:  String,
    val colorHex: Long
) {
    GENERAL("General",  "Todo México",                                                0xFF795548),
    NORTE  ("Norte",    "Baja California, Sonora, Chihuahua, Nuevo León, Tamaulipas", 0xFF1565C0),
    CENTRO ("Centro",   "CDMX, Estado de México, Jalisco, Puebla, Guanajuato",        0xFF2E7D32),
    SUR    ("Sur",      "Oaxaca, Chiapas, Veracruz, Guerrero, Yucatán",               0xFFE65100),
    PUEBLA ("Puebla",   "Puebla y zona centro-sur",                                   0xFF6A1B9A)
}

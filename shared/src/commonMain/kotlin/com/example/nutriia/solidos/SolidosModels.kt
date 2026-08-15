package com.example.nutriia.solidos

import com.example.nutriia.shared.Timestamp
import com.example.nutriia.sueldo.Alergeno

// ═══════════════════════════════════════════════════════════════════════════
// MODELOS — MÓDULO SÓLIDOS
// Fuente principal: WHO — Infant and Young Child Feeding (2023)
// Fuente secundaria: UNICEF — Early Childhood Nutrition (2023)
// Guía clínica: ESPGHAN Complementary Feeding Guidelines (2017)
// ═══════════════════════════════════════════════════════════════════════════

data class AlimentoIntroducido(
    val id:                String           = "",
    val childId:           String           = "",
    val userId:            String           = "",
    val nombre:            String           = "",
    val grupo:             GrupoAlimento    = GrupoAlimento.VERDURAS,
    val fechaIntroduccion: String           = "",
    val reaccion:          ReaccionAlimento = ReaccionAlimento.NINGUNA,
    val notas:             String           = "",
    val creadoEn:          Timestamp?       = null
)

enum class GrupoAlimento(val label: String, val colorHex: Long) {
    VERDURAS  ("Verduras",   0xFF43A047),
    FRUTAS    ("Frutas",     0xFFFF8F00),
    CEREALES  ("Cereales",   0xFFD4A017),
    PROTEINAS ("Proteínas",  0xFFE53935),
    LACTEOS   ("Lácteos",    0xFF1E88E5),
    LEGUMBRES ("Legumbres",  0xFF8D6E63),
    OTROS     ("Otros",      0xFF9E9E9E)
}

enum class ReaccionAlimento(val label: String) {
    NINGUNA ("Sin reacción"),
    LEVE    ("Reacción leve"),
    ALERGIA ("Posible alergia"),
    RECHAZO ("Rechazó el alimento"),
    ACEPTADO(label = "Aceptado"),
}

data class AlimentoPermitido(
    val nombre:          String,
    val grupo:           GrupoAlimento,
    val edadMesesMinima: Int,
    val esAlergeno:      Boolean   = false,
    val tipoAlergeno:    Alergeno? = null,
    val consejo:         String    = ""
) {
    fun esContraIndicadoPara(alergenosNino: List<Alergeno>): Boolean {
        if (!esAlergeno) return false
        return tipoAlergeno != null && tipoAlergeno in alergenosNino
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GUÍA DE EDAD — Texturas, porciones y frecuencia OMS mes a mes
// ═══════════════════════════════════════════════════════════════════════════

data class GuiaEdad(
    val rangoLabel:         String,
    val texturaLabel:       String,
    val texturaDescripcion: String,
    val texturaEjemplos:    String,
    val porcionMl:          Int,
    val porcionLabel:       String,
    val porcionProgresion:  String,
    val comidasPorDia:      Int,
    val snacksPorDia:       Int,
    val frecuenciaLabel:    String,
    val lactanciaLabel:     String,
    val fuente:             String = "WHO — Infant and Young Child Feeding, 2023"
)

fun guiaParaEdad(meses: Int): GuiaEdad = when {

    meses == 6 -> GuiaEdad(
        rangoLabel         = "6 meses — inicio de complementaria",
        texturaLabel       = "Puré muy liso",
        texturaDescripcion = "Alimentos perfectamente licuados, sin grumos ni fibras. " +
                "Consistencia: se desliza suavemente de la cuchara.",
        texturaEjemplos    = "Puré de zanahoria licuado y colado · Puré de papa · Puré de plátano aplastado",
        porcionMl          = 30,
        porcionLabel       = "1-2 cdas (15-30 ml) por comida",
        porcionProgresion  = "Aumentar de 1 cda la primera semana hasta 2-3 cdas. " +
                "Estómago del bebé: ~50-80 ml de capacidad.",
        comidasPorDia      = 2,
        snacksPorDia       = 0,
        frecuenciaLabel    = "2 comidas complementarias/día + lactancia a demanda",
        lactanciaLabel     = "Lactancia materna a demanda — sigue siendo la fuente principal (OMS)"
    )

    meses == 7 -> GuiaEdad(
        rangoLabel         = "7 meses",
        texturaLabel       = "Puré liso con algo de cuerpo",
        texturaDescripcion = "Puré bien licuado pero ligeramente más denso. " +
                "Se pueden mezclar 2 ingredientes.",
        texturaEjemplos    = "Puré de pollo con papa · Puré de zanahoria con aceite · Puré de frijol colado",
        porcionMl          = 60,
        porcionLabel       = "3-4 cdas (45-60 ml) por comida",
        porcionProgresion  = "Aumentar gradualmente hasta llenar 1/4 de taza. Respetar señales de saciedad.",
        comidasPorDia      = 2,
        snacksPorDia       = 0,
        frecuenciaLabel    = "2 comidas complementarias/día + lactancia a demanda",
        lactanciaLabel     = "Lactancia materna a demanda — cubre ~70% de las necesidades energéticas"
    )

    meses == 8 -> GuiaEdad(
        rangoLabel         = "8 meses",
        texturaLabel       = "Puré grumoso / triturado con tenedor",
        texturaDescripcion = "Textura irregular con grumos suaves. El bebé practica masticación " +
                "aunque aún no tiene dientes.",
        texturaEjemplos    = "Pollo desmenuzado muy fino · Lentejas aplastadas con tenedor · Plátano con grumos",
        porcionMl          = 125,
        porcionLabel       = "1/2 taza (125 ml) por comida",
        porcionProgresion  = "Objetivo al final del mes: 125 ml. Llegar progresivamente desde 60 ml.",
        comidasPorDia      = 3,
        snacksPorDia       = 0,
        frecuenciaLabel    = "3 comidas complementarias/día + lactancia a demanda",
        lactanciaLabel     = "Lactancia materna a demanda — OMS: mínimo 8 tomas/día"
    )

    meses == 9 -> GuiaEdad(
        rangoLabel         = "9 meses",
        texturaLabel       = "Trozos muy suaves (finger foods iniciales)",
        texturaDescripcion = "Trozos que se deshacen con presión de los dedos contra el paladar. " +
                "El bebé empieza a usar la pinza (índice + pulgar).",
        texturaEjemplos    = "Zanahoria cocida muy blanda en trozos 1cm · Plátano en rodajas · Papa en cubos blandos",
        porcionMl          = 150,
        porcionLabel       = "1/2-3/4 taza (125-150 ml) por comida",
        porcionProgresion  = "Aumentar de 125 ml al inicio hacia 150 ml. Respetar señales de saciedad.",
        comidasPorDia      = 3,
        snacksPorDia       = 1,
        frecuenciaLabel    = "3 comidas + 1 snack/día + lactancia",
        lactanciaLabel     = "Lactancia materna a demanda — mínimo 3-4 tomas/día (OMS)"
    )

    meses == 10 -> GuiaEdad(
        rangoLabel         = "10 meses",
        texturaLabel       = "Trozos suaves y comida familiar modificada",
        texturaDescripcion = "Trozos de 1-2 cm que el bebé aplasta con la encía. " +
                "Versiones modificadas de la comida familiar (sin sal, sin chile, sin azúcar).",
        texturaEjemplos    = "Caldo familiar sin sal con verduras blandas · Arroz suave · Frijoles enteros bien cocidos",
        porcionMl          = 165,
        porcionLabel       = "3/4 taza (165 ml) por comida",
        porcionProgresion  = "Mantener 165 ml por comida. Variar ingredientes más que cantidad.",
        comidasPorDia      = 3,
        snacksPorDia       = 1,
        frecuenciaLabel    = "3 comidas + 1-2 snacks/día + lactancia",
        lactanciaLabel     = "Lactancia materna a demanda — importante antes de dormir"
    )

    meses == 11 -> GuiaEdad(
        rangoLabel         = "11 meses",
        texturaLabel       = "Alimentos picados y comida familiar blanda",
        texturaDescripcion = "Come casi todo picado fino. Puede morder trozos blandos. " +
                "Retirar la porción del bebé antes de añadir sal/condimentos.",
        texturaEjemplos    = "Pollo desmenuzado en guiso · Tortilla blanda en pedacitos · Fruta madura en trozos",
        porcionMl          = 180,
        porcionLabel       = "3/4 taza (180 ml) por comida",
        porcionProgresion  = "Consolidar 180 ml. Preparar para transición a 1 taza al cumplir el año.",
        comidasPorDia      = 3,
        snacksPorDia       = 2,
        frecuenciaLabel    = "3 comidas + 2 snacks/día + lactancia",
        lactanciaLabel     = "Lactancia materna a demanda — OMS recomienda continuar hasta 2 años o más"
    )

    meses in 12..17 -> GuiaEdad(
        rangoLabel         = "12-17 meses",
        texturaLabel       = "Comida familiar picada / trozos",
        texturaDescripcion = "Comida familiar en trozos pequeños. Ya mastica con molares primarios. " +
                "Evitar: alimentos enteros redondos (uva entera), alimentos duros (zanahoria cruda).",
        texturaEjemplos    = "Guisos familiares · Arroz · Frijoles enteros · Fruta en trozos · Pan blandito",
        porcionMl          = 250,
        porcionLabel       = "3/4-1 taza (180-250 ml) por comida",
        porcionProgresion  = "Progresar de 180 ml al año hasta 250 ml a los 17 meses. El apetito variable es normal.",
        comidasPorDia      = 3,
        snacksPorDia       = 2,
        frecuenciaLabel    = "3 comidas + 2 snacks/día + lactancia",
        lactanciaLabel     = "Lactancia materna a demanda — OMS: continuar hasta 2 años como mínimo"
    )

    meses in 18..23 -> GuiaEdad(
        rangoLabel         = "18-23 meses",
        texturaLabel       = "Comida familiar completa",
        texturaDescripcion = "Come prácticamente todo igual que la familia. Ya tiene 12-16 dientes. " +
                "Precaución: uvas enteras, salchichas en rueda, palomitas — riesgo de asfixia.",
        texturaEjemplos    = "Platillos mexicanos típicos sin chile picoso · Ensaladas blandas · Frutas en trozos",
        porcionMl          = 250,
        porcionLabel       = "1 taza (250 ml) por comida",
        porcionProgresion  = "Mantener 250 ml. El apetito puede ser irregular — es normal. No forzar.",
        comidasPorDia      = 3,
        snacksPorDia       = 2,
        frecuenciaLabel    = "3 comidas + 2 snacks/día + lactancia si continúa",
        lactanciaLabel     = "Lactancia si continúa — OMS: hasta 2 años o más según deseo de madre y niño"
    )

    meses in 24..35 -> GuiaEdad(
        rangoLabel         = "2-3 años",
        texturaLabel       = "Comida familiar sin restricciones",
        texturaDescripcion = "Dieta totalmente familiar. 20 dientes de leche completos hacia los 30 meses. " +
                "Continuar vigilancia en alimentos de asfixia hasta los 4 años.",
        texturaEjemplos    = "Comida familiar completa · Frutas y verduras crudas · Carnes en trozos",
        porcionMl          = 315,
        porcionLabel       = "1-1¼ taza (250-315 ml) por comida",
        porcionProgresion  = "Aumentar hasta 315 ml. El niño regula su propia ingesta — confiar en sus señales.",
        comidasPorDia      = 3,
        snacksPorDia       = 2,
        frecuenciaLabel    = "3 comidas principales + 2 snacks/día",
        lactanciaLabel     = "Lactancia si continúa — OMS no establece límite superior"
    )

    else -> GuiaEdad(
        rangoLabel         = "3+ años",
        texturaLabel       = "Alimentación familiar plena",
        texturaDescripcion = "Sin restricciones de textura. Dieta familiar variada y balanceada. " +
                "Importante: desayuno siempre, 5 comidas al día, agua como bebida principal.",
        texturaEjemplos    = "Cualquier platillo familiar · Frutas y verduras crudas y cocidas",
        porcionMl          = 375,
        porcionLabel       = "1¼-1½ taza (315-375 ml) por comida",
        porcionProgresion  = "Guiarse por apetito. OMS: no restringir grasa en menores de 5 años.",
        comidasPorDia      = 3,
        snacksPorDia       = 2,
        frecuenciaLabel    = "3 comidas principales + 2 snacks/día + agua simple",
        lactanciaLabel     = "Lactancia si continúa — beneficios inmunológicos documentados hasta los 4 años"
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// PLAN SEMANAL
//
// FIX v2.3: campo colacion2 añadido para mostrar la segunda colación del día
// ═══════════════════════════════════════════════════════════════════════════

data class PlanSemanalSolidos(
    val diaSemana:       String,
    val desayuno:        String,
    val almuerzo:        String,
    val merienda:        String,            // colacion1 (mañana)
    val colacion2:       String = "",       // FIX v2.3: colacion2 (tarde)
    val cena:            String,
    val porcionLabel:    String = "",
    val texturaLabel:    String = "",
    val frecuenciaLabel: String = ""
)

object FuentesSolidos {
    const val WHO_URL       = "https://www.who.int/news-room/fact-sheets/detail/infant-and-young-child-feeding"
    const val UNICEF_URL    = "https://www.unicef.org/nutrition/early-childhood-nutrition"
    const val WHO_LABEL     = "WHO — Infant and Young Child Feeding, 2023"
    const val UNICEF_LABEL  = "UNICEF — Early Childhood Nutrition, 2023"
    const val ESPGHAN_URL   = "https://espghan.org/knowledge-center/publications/nutrition-publications/espghan-committee-on-nutrition-complementary-feeding"
    const val ESPGHAN_LABEL = "ESPGHAN — Complementary Feeding Guidelines, 2017"
}

// ═══════════════════════════════════════════════════════════════════════════
// ALIMENTOS POR EDAD
// ═══════════════════════════════════════════════════════════════════════════

val ALIMENTOS_POR_EDAD: List<AlimentoPermitido> = listOf(
    // 6 meses
    AlimentoPermitido("Puré de zanahoria",       GrupoAlimento.VERDURAS,  6, false, null,              "Textura muy suave. Rico en betacarotenos."),
    AlimentoPermitido("Puré de calabaza",         GrupoAlimento.VERDURAS,  6, false, null,              "Rica en betacarotenos y vitamina A."),
    AlimentoPermitido("Puré de papa",             GrupoAlimento.VERDURAS,  6, false, null,              "Fuente de energía de fácil digestión."),
    AlimentoPermitido("Puré de camote",           GrupoAlimento.VERDURAS,  6, false, null,              "Rico en betacarotenos. Tradicional latinoamericano."),
    AlimentoPermitido("Puré de chayote",          GrupoAlimento.VERDURAS,  6, false, null,              "Textura muy suave. Muy consumido en México."),
    AlimentoPermitido("Puré de chícharo",         GrupoAlimento.VERDURAS,  6, false, null,              "Fuente de proteína vegetal y hierro. Colar bien."),
    AlimentoPermitido("Puré de plátano",          GrupoAlimento.FRUTAS,    6, false, null,              "Alto en potasio. No requiere cocción."),
    AlimentoPermitido("Puré de manzana",          GrupoAlimento.FRUTAS,    6, false, Alergeno.FRUCTOSA, "Cocida y sin cáscara. Rica en vitamina C."),
    AlimentoPermitido("Puré de pera",             GrupoAlimento.FRUTAS,    6, false, Alergeno.FRUCTOSA, "Suave y dulce. Fácil digestión."),
    AlimentoPermitido("Puré de papaya",           GrupoAlimento.FRUTAS,    6, false, null,              "Rica en vitamina C. Enzimas digestivas."),
    AlimentoPermitido("Puré de mango",            GrupoAlimento.FRUTAS,    6, false, null,              "Rico en vitamina A y C."),
    AlimentoPermitido("Arroz papilla",            GrupoAlimento.CEREALES,  6, false, null,              "Muy bajo riesgo alérgico. OMS recomienda cereales con hierro."),
    AlimentoPermitido("Papilla de maíz",          GrupoAlimento.CEREALES,  6, false, Alergeno.MAIZ,     "Atole de maíz sin azúcar. Básico latinoamericano."),
    AlimentoPermitido("Puré de pollo",            GrupoAlimento.PROTEINAS, 6, false, null,              "Hierro hemo de alta biodisponibilidad."),
    AlimentoPermitido("Puré de res",              GrupoAlimento.PROTEINAS, 6, false, null,              "Excelente fuente de hierro hemo."),
    AlimentoPermitido("Puré de hígado de pollo",  GrupoAlimento.PROTEINAS, 6, false, null,              "Fuente más concentrada de hierro hemo. OMS lo recomienda."),
    AlimentoPermitido("Lentejas en puré",         GrupoAlimento.LEGUMBRES, 6, false, null,              "Hierro no hemo + proteína vegetal. Combinar con vitamina C."),
    AlimentoPermitido("Puré de frijol negro",     GrupoAlimento.LEGUMBRES, 6, false, null,              "Rico en hierro y proteína. Bien cocido y colado."),
    AlimentoPermitido("Puré de frijol pinto",     GrupoAlimento.LEGUMBRES, 6, false, null,              "Similar al frijol negro."),
    AlimentoPermitido("Yema de huevo cocida",     GrupoAlimento.PROTEINAS, 6, true,  Alergeno.HUEVO,    "ALERGENO. ESPGHAN: desde 6m. Observar 3 días."),
    AlimentoPermitido("Pescado blanco",           GrupoAlimento.PROTEINAS, 6, true,  Alergeno.PESCADO,  "ALERGENO. ESPGHAN: desde 6m. Merluza o tilapia cocida."),
    // 7-8 meses
    AlimentoPermitido("Puré de brócoli",          GrupoAlimento.VERDURAS,  7, false, null,              "Rico en calcio y vitamina C."),
    AlimentoPermitido("Puré de aguacate",         GrupoAlimento.FRUTAS,    7, false, null,              "Grasas para neurodesarrollo. Sin cocción."),
    AlimentoPermitido("Avena papilla",            GrupoAlimento.CEREALES,  7, false, Alergeno.TRIGO,    "Rica en fibra y hierro. Puede tener trazas de gluten."),
    AlimentoPermitido("Huevo entero cocido",      GrupoAlimento.PROTEINAS, 7, true,  Alergeno.HUEVO,    "ALERGENO. Si toleró la yema, añadir clara. Observar 3 días."),
    AlimentoPermitido("Nopal cocido en puré",     GrupoAlimento.VERDURAS,  7, false, null,              "Rico en calcio. Cocinar bien, sin espinas."),
    AlimentoPermitido("Quelites en puré",         GrupoAlimento.VERDURAS,  7, false, null,              "Hierbas mexicanas (quintonil, verdolaga). Ricas en hierro."),
    AlimentoPermitido("Epazote cocido",           GrupoAlimento.VERDURAS,  7, false, null,              "Hierba mexicana. Facilita digestión de frijoles."),
    // 9-11 meses
    AlimentoPermitido("Yogur natural",            GrupoAlimento.LACTEOS,   9, true,  Alergeno.LACTEOS,  "ALERGENO (lácteo). Sin azúcar."),
    AlimentoPermitido("Queso fresco",             GrupoAlimento.LACTEOS,   9, true,  Alergeno.LACTEOS,  "ALERGENO (lácteo). Pequeñas cantidades. Bajo en sal."),
    AlimentoPermitido("Tomate cocido",            GrupoAlimento.VERDURAS,  9, false, null,              "Sin piel ni semillas. Rico en licopeno."),
    AlimentoPermitido("Ejotes tiernos cocidos",   GrupoAlimento.VERDURAS,  9, false, null,              "Ricos en hierro. Trozos muy blandos."),
    AlimentoPermitido("Betabel cocido",           GrupoAlimento.VERDURAS,  9, false, null,              "Rico en folatos. Color en pañal es normal."),
    AlimentoPermitido("Guayaba sin semillas",     GrupoAlimento.FRUTAS,    9, false, null,              "Rica en vitamina C. Colar semillas."),
    AlimentoPermitido("Mamey",                    GrupoAlimento.FRUTAS,    9, false, null,              "Rica en betacarotenos. Textura cremosa."),
    AlimentoPermitido("Zapote negro",             GrupoAlimento.FRUTAS,    9, false, null,              "Textura muy suave. Rica en vitamina C."),
    // 12+ meses
    AlimentoPermitido("Leche de vaca entera",     GrupoAlimento.LACTEOS,   12, true,  Alergeno.LACTEOS,  "ALERGENO. Solo después del primer año. NO sustituye lactancia."),
    AlimentoPermitido("Miel",                     GrupoAlimento.OTROS,     12, false, null,              "Prohibida antes del año — botulismo infantil (OMS)."),
    AlimentoPermitido("Nueces molidas",           GrupoAlimento.PROTEINAS, 12, true,  Alergeno.NUECES,   "ALERGENO. Siempre trituradas. NUNCA enteras."),
    AlimentoPermitido("Almendras molidas",        GrupoAlimento.PROTEINAS, 12, true,  Alergeno.NUECES,   "ALERGENO. Solo en harina o pasta. Ricas en calcio."),
    AlimentoPermitido("Pan integral",             GrupoAlimento.CEREALES,  12, false, Alergeno.TRIGO,    "Bajo en sal. Trozos blandos."),
    AlimentoPermitido("Tortilla de maíz",         GrupoAlimento.CEREALES,  12, false, Alergeno.MAIZ,     "Básico mexicano. Rica en calcio por nixtamalización."),
    AlimentoPermitido("Mariscos",                 GrupoAlimento.PROTEINAS, 12, true,  Alergeno.MARISCOS, "ALERGENO. Bien cocidos. Ricos en zinc y yodo."),
    AlimentoPermitido("Espinaca",                 GrupoAlimento.VERDURAS,  12, false, null,              "Bien cocida. Moderar hasta 3 años por nitratos (ESPGHAN)."),
    AlimentoPermitido("Uvas",                     GrupoAlimento.FRUTAS,    12, false, null,              "Cortar en cuartos SIEMPRE. Nunca enteras."),
    AlimentoPermitido("Tamarindo (pulpa)",         GrupoAlimento.FRUTAS,    12, false, null,              "Rico en hierro. Solo pulpa sin semillas."),
    AlimentoPermitido("Chile (mínimo picor)",     GrupoAlimento.OTROS,     12, false, null,              "Cantidades muy pequeñas de chiles suaves."),
    // 24+ meses
    AlimentoPermitido("Leche descremada",         GrupoAlimento.LACTEOS,   24, false, Alergeno.LACTEOS,  "Solo a partir de 2 años si hay sobrepeso (OMS)."),
    AlimentoPermitido("Cacahuates",               GrupoAlimento.PROTEINAS, 24, true,  Alergeno.CACAHUATE,"ALERGENO. Solo pasta o molidos. NUNCA enteros."),
    AlimentoPermitido("Cacahuate en pasta",       GrupoAlimento.PROTEINAS, 24, true,  Alergeno.CACAHUATE,"ALERGENO. Forma segura según estudio LEAP.")
)

fun alimentosParaEdad(meses: Int): List<AlimentoPermitido> =
    ALIMENTOS_POR_EDAD.filter { it.edadMesesMinima <= meses }

// ═══════════════════════════════════════════════════════════════════════════
// GENERADOR DE PLAN SEMANAL (basado en OMS — sin DietaEngine)
// Usado solo como fallback local; el plan principal usa DietaEngine.
// ═══════════════════════════════════════════════════════════════════════════

fun generarPlanSemanal(
    meses:         Int,
    alergenosNino: List<Alergeno> = emptyList()
): List<PlanSemanalSolidos> {
    val dias = listOf("Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo")
    val guia = guiaParaEdad(meses)

    val proteinas = mutableListOf("pollo","res","lentejas","frijol","hígado de pollo")
    if (Alergeno.HUEVO   !in alergenosNino) proteinas.add("huevo")
    if (Alergeno.PESCADO !in alergenosNino) proteinas.add("pescado blanco")

    val lacteos = if (Alergeno.LACTEOS !in alergenosNino)
        listOf("yogur natural","queso fresco") else listOf("aguacate","leche materna")

    val cereales = mutableListOf("arroz","papilla de maíz")
    if (Alergeno.TRIGO !in alergenosNino) cereales.add("avena")
    if (Alergeno.MAIZ  !in alergenosNino) cereales.addAll(listOf("tortilla de maíz","papilla de maíz"))

    return when {

        meses <= 7 -> dias.mapIndexed { i, dia ->
            val vegetal = listOf("zanahoria","calabaza","chayote","camote","chícharo","papa","camote")[i % 7]
            val fruta   = listOf("plátano","papaya","manzana","mango","pera","papaya","plátano")[i % 7]
            val hierro  = proteinas[i % proteinas.size]
            PlanSemanalSolidos(dia,
                desayuno        = "[${guia.porcionLabel}] Puré liso de $vegetal + $hierro + lactancia",
                almuerzo        = "[${guia.porcionLabel}] Puré de $fruta + lactancia a demanda",
                merienda        = "Lactancia materna a demanda",
                colacion2       = "",
                cena            = "Lactancia materna a demanda",
                porcionLabel    = guia.porcionLabel,
                texturaLabel    = guia.texturaLabel,
                frecuenciaLabel = guia.frecuenciaLabel
            )
        }

        meses == 8 -> dias.mapIndexed { i, dia ->
            val vegetal  = listOf("zanahoria","brócoli","chayote","camote","chícharo","papa","calabaza")[i % 7]
            val fruta    = listOf("plátano","papaya","mango","aguacate","pera","papaya","plátano")[i % 7]
            val proteina = proteinas[i % proteinas.size]
            val cereal   = cereales[i % cereales.size]
            PlanSemanalSolidos(dia,
                desayuno        = "[${guia.porcionLabel}] $cereal con grumos + $fruta aplastada + lactancia",
                almuerzo        = "[${guia.porcionLabel}] $vegetal aplastado + $proteina desmenuzado fino",
                merienda        = "Lactancia materna a demanda",
                colacion2       = "",
                cena            = "[${guia.porcionLabel}] Puré grumoso de $vegetal con $proteina + lactancia",
                porcionLabel    = guia.porcionLabel,
                texturaLabel    = guia.texturaLabel,
                frecuenciaLabel = guia.frecuenciaLabel
            )
        }

        meses in 9..11 -> dias.mapIndexed { i, dia ->
            val vegetal  = listOf("zanahoria","brócoli","chayote","betabel","ejotes","nopal","calabaza")[i % 7]
            val fruta    = listOf("plátano","papaya","mango","guayaba","pera","mamey","manzana")[i % 7]
            val proteina = proteinas[i % proteinas.size]
            val cereal   = cereales[i % cereales.size]
            val snack    = lacteos[i % lacteos.size]
            PlanSemanalSolidos(dia,
                desayuno        = "[${guia.porcionLabel}] $cereal + $fruta en trozos blandos + lactancia",
                almuerzo        = "[${guia.porcionLabel}] $vegetal en trozos 1cm + $proteina + caldo",
                merienda        = "Snack: $snack o $fruta en trozos + lactancia",
                colacion2       = "",
                cena            = "[${guia.porcionLabel}] Sopa de $vegetal con $proteina + $cereal",
                porcionLabel    = guia.porcionLabel,
                texturaLabel    = guia.texturaLabel,
                frecuenciaLabel = guia.frecuenciaLabel
            )
        }

        meses in 12..23 -> dias.mapIndexed { i, dia ->
            val proteina = proteinas[i % proteinas.size]
            val verdura  = listOf("brócoli","zanahoria","calabaza","quelites","nopal","chayote","betabel")[i % 7]
            val snack    = listOf("fruta picada",lacteos[i % lacteos.size],"aguacate + tortilla","mamey","guayaba","plátano","papaya")[i % 7]
            val cereal   = cereales[i % cereales.size]
            PlanSemanalSolidos(dia,
                desayuno        = "[${guia.porcionLabel}] $cereal + ${lacteos[i % lacteos.size]} + fruta + lactancia",
                almuerzo        = "[${guia.porcionLabel}] $proteina con $verdura + $cereal (comida familiar sin sal)",
                merienda        = "Snack: $snack + lactancia (OMS: hasta 2 años)",
                colacion2       = "",
                cena            = "[${guia.porcionLabel}] Sopa o guiso de $verdura con $proteina",
                porcionLabel    = guia.porcionLabel,
                texturaLabel    = guia.texturaLabel,
                frecuenciaLabel = guia.frecuenciaLabel
            )
        }

        else -> dias.mapIndexed { i, dia ->
            val proteina = proteinas[i % proteinas.size]
            val verdura  = listOf("brócoli","nopal","calabaza","espinaca","zanahoria","ejotes","quelites")[i % 7]
            val snack    = listOf("fruta + ${lacteos[i % lacteos.size]}","fruta picada","aguacate","nuez molida + fruta","tortilla + frijol")[i % 5]
            val cereal   = cereales[i % cereales.size]
            PlanSemanalSolidos(dia,
                desayuno        = "[${guia.porcionLabel}] $cereal + ${lacteos[i % lacteos.size]} + fruta",
                almuerzo        = "[${guia.porcionLabel}] $proteina con $verdura + $cereal",
                merienda        = "Snack: $snack",
                colacion2       = "",
                cena            = "[${guia.porcionLabel}] Guiso de $verdura con $proteina",
                porcionLabel    = guia.porcionLabel,
                texturaLabel    = guia.texturaLabel,
                frecuenciaLabel = guia.frecuenciaLabel
            )
        }
    }
}
package com.example.nutriia.crecimiento

import com.example.nutriia.shared.Timestamp
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

// ═══════════════════════════════════════════════════════════════════════════
// MODELOS — CRECIMIENTO
//
// FUENTES OFICIALES OMS:
//   • 0–60 meses  → WHO Child Growth Standards 2006
//     https://www.who.int/tools/child-growth-standards
//   • 61–144 meses → WHO Growth Reference 2007 (5–19 años)
//     https://www.who.int/tools/growth-reference-data-for-5to19-years
//     de Onis M et al. Bull World Health Organ 2007;85(9):660-667.
//     https://pmc.ncbi.nlm.nih.gov/articles/PMC2636412/
//
// Tablas descargadas directamente de cdn.who.int (PDFs oficiales):
//   Peso niños   (5–10 a): cdn.who.int/.../wfa-boys--5-10years-per.pdf
//   Peso niñas   (5–10 a): cdn.who.int/.../wfa-girls-5-10years-per.pdf
//   Talla niños  (5–19 a): cdn.who.int/.../hfa-boys-5-19years-per.pdf
//   Talla niñas  (5–19 a): cdn.who.int/.../hfa-girls-5-19years-per.pdf
//   IMC niños    (5–19 a): cdn.who.int/.../bmifa-boys-5-19years-per.pdf
//   IMC niñas    (5–19 a): cdn.who.int/.../bmifa-girls-5-19years-per.pdf
//
// Nota sobre peso ≥ 10 años:
//   La OMS solo publica peso-para-edad hasta 10 años (120 meses) porque
//   más allá el peso solo no distingue talla vs masa. Para > 120 m se
//   usa IMC-para-edad como indicador primario de estado nutricional.
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
enum class Sexo { NINO, NINA }

@Serializable
data class MedicionCrecimiento(
    val id:        String     = "",
    val childId:   String     = "",
    val userId:    String     = "",
    val fecha:     String     = "",
    val pesoKg:    Double     = 0.0,
    val tallaCm:   Double     = 0.0,
    val circCefCm: Double     = 0.0,
    val notas:     String     = "",
    val creadoEn:  Timestamp? = null
) {
    val imc: Double get() =
        if (tallaCm > 0) pesoKg / ((tallaCm / 100.0) * (tallaCm / 100.0)) else 0.0

    fun fechaEpoch(): Long {
        val f = fecha.trim()
        if (f.isBlank()) return creadoEn?.seconds ?: 0L
        val yyyymmdd = Regex("""^(\d{4})[-/](\d{1,2})[-/](\d{1,2})""").find(f)
        if (yyyymmdd != null) {
            val y = yyyymmdd.groupValues[1].toLongOrNull() ?: 0L
            val m = yyyymmdd.groupValues[2].toLongOrNull() ?: 0L
            val d = yyyymmdd.groupValues[3].toLongOrNull() ?: 0L
            return y * 10000 + m * 100 + d
        }
        val ddmmyyyy = Regex("""^(\d{1,2})[-/](\d{1,2})[-/](\d{4})""").find(f)
        if (ddmmyyyy != null) {
            val d = ddmmyyyy.groupValues[1].toLongOrNull() ?: 0L
            val m = ddmmyyyy.groupValues[2].toLongOrNull() ?: 0L
            val y = ddmmyyyy.groupValues[3].toLongOrNull() ?: 0L
            return y * 10000 + m * 100 + d
        }
        return creadoEn?.seconds ?: 0L
    }
}

data class PuntoOMS(
    val meses: Int,
    val p3:    Double,
    val p15:   Double,
    val p50:   Double,
    val p85:   Double,
    val p97:   Double
)

data class PuntoOMSLms(
    val meses:  Int,
    val l:      Double,
    val m:      Double,
    val s:      Double,
    val sd3neg: Double = 0.0,
    val sd2neg: Double = 0.0,
    val sd1neg: Double = 0.0,
    val sd0:    Double = 0.0,
    val sd1pos: Double = 0.0,
    val sd2pos: Double = 0.0,
    val sd3pos: Double = 0.0
)

data class PuntoOMSLongitud(
    val longitudCm: Double,
    val p3:         Double,
    val p15:        Double,
    val p50:        Double,
    val p85:        Double,
    val p97:        Double,
    val sd3neg:     Double = 0.0,
    val sd2neg:     Double = 0.0,
    val sd1neg:     Double = 0.0,
    val sd0:        Double = 0.0,
    val sd1pos:     Double = 0.0,
    val sd2pos:     Double = 0.0,
    val sd3pos:     Double = 0.0
)

data class InterpretacionIMC(
    val categoria:   String,
    val descripcion: String,
    val color:       Long,
    /** true cuando se calculó sin sexo registrado — la UI lo muestra con aviso */
    val esSinSexo:   Boolean = false,
    /** Indicador antropométrico oficial OMS utilizado para el cálculo */
    val indicador:   String = ""
)

// ═══════════════════════════════════════════════════════════════════════════
// METADATOS DE FUENTES
// ═══════════════════════════════════════════════════════════════════════════

object FuentesCrecimiento {
    // WHO Child Growth Standards 2006 (0–5 años)
    const val WHO_GROWTH_URL    = "https://www.who.int/tools/child-growth-standards"
    const val WHO_WEIGHT_URL    = "https://www.who.int/tools/child-growth-standards/standards/weight-for-age"
    const val WHO_HEIGHT_URL    = "https://www.who.int/tools/child-growth-standards/standards/length-height-for-age"
    const val WHO_WFL_URL       = "https://www.who.int/tools/child-growth-standards/standards/weight-for-length-height"
    const val WHO_BMI_URL       = "https://www.who.int/tools/child-growth-standards/standards/body-mass-index-for-age"
    const val WHO_FEEDING_URL   = "https://www.who.int/news-room/fact-sheets/detail/infant-and-young-child-feeding"
    const val WHO_GROWTH_LABEL  = "WHO Child Growth Standards, 2006 (0–5 años)"

    // WHO Growth Reference 2007 (5–19 años)
    const val WHO_REF_2007_URL   = "https://www.who.int/tools/growth-reference-data-for-5to19-years"
    const val WHO_REF_BMI_URL    = "https://www.who.int/tools/growth-reference-data-for-5to19-years/indicators/bmi-for-age"
    const val WHO_REF_HEIGHT_URL = "https://www.who.int/tools/growth-reference-data-for-5to19-years/indicators/height-for-age"
    const val WHO_REF_WEIGHT_URL = "https://www.who.int/tools/growth-reference-data-for-5to19-years/indicators/weight-for-age-5to10-years"
    const val WHO_REF_2007_LABEL = "WHO Growth Reference 2007 (5–19 años) — de Onis et al., Bull WHO 2007"

    // PDFs fuente (tablas de percentiles descargadas directamente)
    const val PDF_PESO_NINOS    = "https://cdn.who.int/media/docs/default-source/child-growth/growth-reference-5-19-years/weight-for-age-(5-10-years)/wfa-boys--5-10years-per.pdf"
    const val PDF_PESO_NINAS    = "https://cdn.who.int/media/docs/default-source/child-growth/growth-reference-5-19-years/weight-for-age-(5-10-years)/wfa-girls-5-10years-per.pdf"
    const val PDF_TALLA_NINOS   = "https://cdn.who.int/media/docs/default-source/child-growth/growth-reference-5-19-years/height-for-age-(5-19-years)/hfa-boys-5-19years-per.pdf"
    const val PDF_TALLA_NINAS   = "https://cdn.who.int/media/docs/default-source/child-growth/growth-reference-5-19-years/height-for-age-(5-19-years)/hfa-girls-5-19years-per.pdf"
    const val PDF_IMC_NINOS     = "https://cdn.who.int/media/docs/default-source/child-growth/growth-reference-5-19-years/bmi-for-age-(5-19-years)/bmifa-boys-5-19years-per.pdf"
    const val PDF_IMC_NINAS     = "https://cdn.who.int/media/docs/default-source/child-growth/growth-reference-5-19-years/bmi-for-age-(5-19-years)/bmifa-girls-5-19years-per.pdf"
}

// ═══════════════════════════════════════════════════════════════════════════
// CURVAS OMS PESO POR EDAD — NIÑOS (boys)
//
// 0–60 m  → WHO Child Growth Standards 2006 (wfa-boys-percentiles)
// 61–120 m → WHO Growth Reference 2007 (wfa-boys-5-10years-per.pdf)
//   Fuente PDF: cdn.who.int/.../wfa-boys--5-10years-per.pdf
//   Columnas usadas: Month | P3 | P15 | P50 | P85 | P97
//   Puntos cada 6 meses (suficiente para interpolación lineal).
// ═══════════════════════════════════════════════════════════════════════════

val OMS_PESO_NINOS = listOf(
    // ── 0–60 m: WHO Child Growth Standards 2006 (wfa-boys-percentiles) ──
    PuntoOMS(0,   2.5,  2.9,  3.3,  3.9,  4.4),
    PuntoOMS(1,   3.4,  3.9,  4.5,  5.1,  5.8),
    PuntoOMS(2,   4.3,  4.9,  5.6,  6.3,  7.1),
    PuntoOMS(3,   5.0,  5.7,  6.4,  7.2,  8.0),
    PuntoOMS(4,   5.6,  6.2,  7.0,  7.8,  8.7),
    PuntoOMS(5,   6.0,  6.7,  7.5,  8.4,  9.3),
    PuntoOMS(6,   6.4,  7.1,  7.9,  8.8,  9.8),
    PuntoOMS(7,   6.7,  7.4,  8.3,  9.2, 10.3),
    PuntoOMS(8,   6.9,  7.7,  8.6,  9.6, 10.7),
    PuntoOMS(9,   7.1,  8.0,  8.9,  9.9, 11.0),
    PuntoOMS(10,  7.4,  8.2,  9.2, 10.2, 11.4),
    PuntoOMS(11,  7.6,  8.4,  9.4, 10.5, 11.7),
    PuntoOMS(12,  7.7,  8.6,  9.6, 10.8, 12.0),
    PuntoOMS(15,  8.4,  9.4, 10.6, 11.8, 12.7),
    PuntoOMS(18,  8.8,  9.9, 11.1, 12.4, 13.4),
    PuntoOMS(21,  9.2, 10.4, 11.7, 13.0, 14.0),
    PuntoOMS(24,  9.7, 10.8, 12.2, 13.6, 14.8),
    PuntoOMS(27, 10.0, 11.3, 12.7, 14.2, 15.4),
    PuntoOMS(30, 10.4, 11.7, 13.2, 14.7, 16.0),
    PuntoOMS(33, 10.7, 12.1, 13.6, 15.2, 16.6),
    PuntoOMS(36, 11.0, 12.4, 14.3, 16.2, 17.8),
    PuntoOMS(42, 11.7, 13.2, 15.3, 17.2, 18.9),
    PuntoOMS(48, 12.3, 13.9, 16.3, 18.6, 20.5),
    PuntoOMS(54, 13.0, 14.7, 17.3, 19.8, 21.9),
    PuntoOMS(60, 13.7, 15.5, 18.3, 21.0, 23.2),
    // ── 61–120 m: WHO Growth Reference 2007 (wfa-boys-5-10years-per.pdf) ──
    PuntoOMS(66,  15.3, 16.8, 19.4, 22.3, 25.1),
    PuntoOMS(72,  16.1, 17.9, 20.5, 23.6, 26.7),
    PuntoOMS(78,  17.0, 18.9, 21.7, 25.0, 28.3),
    PuntoOMS(84,  17.9, 19.9, 22.9, 26.5, 30.1),
    PuntoOMS(90,  18.8, 21.0, 24.1, 28.1, 32.0),
    PuntoOMS(96,  19.8, 22.0, 25.4, 29.7, 34.0),
    PuntoOMS(102, 20.7, 23.1, 26.7, 31.4, 36.2),
    PuntoOMS(108, 21.6, 24.2, 28.1, 33.2, 38.6),
    PuntoOMS(114, 22.6, 25.3, 29.6, 35.2, 41.1),
    PuntoOMS(120, 23.6, 26.6, 31.2, 37.3, 43.9)
)

// ═══════════════════════════════════════════════════════════════════════════
// CURVAS OMS PESO POR EDAD — NIÑAS (girls)
//
// 0–60 m  → WHO Child Growth Standards 2006 (wfa-girls-percentiles)
// 61–120 m → WHO Growth Reference 2007 (wfa-girls-5-10years-per.pdf)
// ═══════════════════════════════════════════════════════════════════════════

val OMS_PESO_NINAS = listOf(
    // ── 0–60 m: WHO Child Growth Standards 2006 ──────────────────────────
    PuntoOMS(0,   2.4,  2.8,  3.2,  3.7,  4.2),
    PuntoOMS(1,   3.2,  3.6,  4.2,  4.8,  5.5),
    PuntoOMS(2,   3.9,  4.5,  5.1,  5.8,  6.6),
    PuntoOMS(3,   4.5,  5.2,  5.8,  6.6,  7.5),
    PuntoOMS(4,   5.0,  5.7,  6.4,  7.3,  8.2),
    PuntoOMS(5,   5.4,  6.1,  6.9,  7.8,  8.8),
    PuntoOMS(6,   5.7,  6.5,  7.3,  8.2,  9.3),
    PuntoOMS(7,   6.0,  6.8,  7.6,  8.6,  9.8),
    PuntoOMS(8,   6.3,  7.0,  7.9,  9.0, 10.2),
    PuntoOMS(9,   6.5,  7.3,  8.2,  9.3, 10.5),
    PuntoOMS(10,  6.7,  7.5,  8.5,  9.6, 10.9),
    PuntoOMS(11,  6.9,  7.7,  8.7,  9.9, 11.2),
    PuntoOMS(12,  7.0,  7.9,  8.9, 10.1, 11.5),
    PuntoOMS(15,  7.6,  8.5,  9.6, 10.9, 12.2),
    PuntoOMS(18,  8.1,  9.1, 10.2, 11.5, 12.9),
    PuntoOMS(21,  8.6,  9.6, 10.9, 12.2, 13.7),
    PuntoOMS(24,  9.0, 10.2, 11.5, 12.9, 14.5),
    PuntoOMS(27,  9.4, 10.6, 12.0, 13.5, 15.2),
    PuntoOMS(30,  9.8, 11.0, 12.5, 14.0, 15.8),
    PuntoOMS(33, 10.1, 11.4, 12.9, 14.5, 16.4),
    PuntoOMS(36, 10.8, 12.2, 13.9, 15.7, 17.7),
    PuntoOMS(42, 11.5, 13.0, 14.9, 16.9, 19.2),
    PuntoOMS(48, 12.3, 13.9, 16.1, 18.3, 20.9),
    PuntoOMS(54, 13.0, 14.8, 17.2, 19.7, 22.7),
    PuntoOMS(60, 13.7, 15.7, 18.2, 21.0, 24.3),
    // ── 61–120 m: WHO Growth Reference 2007 ─────────────────────────────
    PuntoOMS(66,  14.8, 17.4, 19.1, 22.4, 25.7),
    PuntoOMS(72,  15.5, 17.4, 20.2, 23.7, 27.3),
    PuntoOMS(78,  16.3, 18.2, 21.2, 25.0, 28.9),
    PuntoOMS(84,  17.0, 19.2, 22.4, 26.5, 30.8),
    PuntoOMS(90,  17.9, 20.2, 23.6, 28.1, 32.8),
    PuntoOMS(96,  18.9, 21.3, 25.0, 29.8, 34.9),
    PuntoOMS(102, 20.0, 22.6, 26.6, 31.8, 37.4),
    PuntoOMS(108, 21.1, 23.9, 28.2, 33.9, 40.0),
    PuntoOMS(114, 22.3, 25.3, 29.7, 36.1, 42.7),
    PuntoOMS(120, 23.7, 26.9, 31.9, 38.5, 45.7)
)

// ═══════════════════════════════════════════════════════════════════════════
// CURVAS OMS TALLA POR EDAD — NIÑOS (boys)
//
// 0–60 m  → WHO Child Growth Standards 2006 (lhfa-boys-percentiles)
// 61–144 m → WHO Growth Reference 2007 (hfa-boys-5-19years-per.pdf)
// ═══════════════════════════════════════════════════════════════════════════

val OMS_TALLA_NINOS = listOf(
    // ── 0–60 m: WHO Child Growth Standards 2006 ──────────────────────────
    PuntoOMS(0,   46.1,  47.9,  49.9,  51.8,  53.7),
    PuntoOMS(1,   50.8,  52.8,  54.7,  56.7,  58.6),
    PuntoOMS(2,   54.4,  56.4,  58.4,  60.4,  62.4),
    PuntoOMS(3,   57.3,  59.4,  61.4,  63.5,  65.5),
    PuntoOMS(4,   59.7,  61.8,  63.9,  66.0,  68.0),
    PuntoOMS(5,   61.7,  63.8,  65.9,  68.0,  70.1),
    PuntoOMS(6,   63.3,  65.5,  67.6,  69.8,  71.9),
    PuntoOMS(7,   64.8,  67.0,  69.2,  71.3,  73.5),
    PuntoOMS(8,   66.2,  68.4,  70.6,  72.8,  75.0),
    PuntoOMS(9,   67.5,  69.7,  72.0,  74.2,  76.5),
    PuntoOMS(10,  68.7,  71.0,  73.3,  75.6,  77.9),
    PuntoOMS(11,  69.9,  72.2,  74.5,  76.9,  79.2),
    PuntoOMS(12,  71.0,  73.4,  75.7,  78.1,  80.5),
    PuntoOMS(15,  74.8,  77.1,  79.8,  82.5,  84.6),
    PuntoOMS(18,  77.5,  79.9,  82.3,  84.7,  86.8),
    PuntoOMS(21,  80.0,  82.4,  85.1,  87.7,  89.9),
    PuntoOMS(24,  82.3,  84.9,  87.8,  90.4,  92.9),
    PuntoOMS(27,  84.6,  87.2,  90.1,  92.9,  95.4),
    PuntoOMS(30,  86.7,  89.5,  92.5,  95.3,  97.9),
    PuntoOMS(33,  88.7,  91.5,  94.7,  97.6, 100.2),
    PuntoOMS(36,  89.0,  91.9,  96.1,  98.7, 101.7),
    PuntoOMS(42,  93.0,  96.1, 100.5, 103.8, 107.0),
    PuntoOMS(48,  95.0,  98.1, 102.9, 106.2, 109.4),
    PuntoOMS(54,  98.2, 101.5, 106.7, 110.3, 113.8),
    PuntoOMS(60, 100.7, 103.9, 109.2, 112.7, 116.2),
    // ── 61–144 m: WHO Growth Reference 2007 ─────────────────────────────
    PuntoOMS(66,  104.0, 107.5, 112.9, 116.1, 119.5),
    PuntoOMS(72,  106.7, 110.3, 116.0, 119.3, 122.8),
    PuntoOMS(78,  109.3, 113.0, 118.9, 122.2, 125.7),
    PuntoOMS(84,  111.8, 115.7, 121.7, 125.2, 128.7),
    PuntoOMS(90,  114.3, 118.3, 124.5, 128.2, 131.7),
    PuntoOMS(96,  116.6, 120.8, 127.3, 131.1, 134.8),
    PuntoOMS(102, 119.0, 123.3, 129.9, 133.9, 137.5),
    PuntoOMS(108, 121.3, 125.7, 132.6, 136.6, 140.4),
    PuntoOMS(114, 123.5, 128.1, 135.2, 139.4, 143.2),
    PuntoOMS(120, 125.8, 130.5, 137.8, 142.1, 146.0),
    PuntoOMS(126, 128.1, 132.9, 140.4, 144.8, 148.7),
    PuntoOMS(132, 130.5, 135.4, 143.1, 147.7, 151.8),
    PuntoOMS(138, 133.0, 138.0, 146.0, 150.6, 155.0),
    PuntoOMS(144, 135.8, 140.7, 149.1, 153.9, 158.4)
)

// ═══════════════════════════════════════════════════════════════════════════
// CURVAS OMS TALLA POR EDAD — NIÑAS (girls)
//
// 0–60 m  → WHO Child Growth Standards 2006 (lhfa-girls-percentiles)
// 61–144 m → WHO Growth Reference 2007 (hfa-girls-5-19years-per.pdf)
// ═══════════════════════════════════════════════════════════════════════════

val OMS_TALLA_NINAS = listOf(
    // ── 0–60 m: WHO Child Growth Standards 2006 ──────────────────────────
    PuntoOMS(0,   45.4,  47.1,  49.1,  51.2,  52.9),
    PuntoOMS(1,   49.8,  51.7,  53.7,  55.8,  57.6),
    PuntoOMS(2,   53.0,  55.0,  57.1,  59.3,  61.1),
    PuntoOMS(3,   55.6,  57.7,  59.8,  61.9,  63.8),
    PuntoOMS(4,   57.8,  59.9,  62.1,  64.3,  66.2),
    PuntoOMS(5,   59.6,  61.8,  64.0,  66.2,  68.2),
    PuntoOMS(6,   61.2,  63.5,  65.7,  68.0,  70.0),
    PuntoOMS(7,   62.7,  65.0,  67.3,  69.6,  71.6),
    PuntoOMS(8,   64.0,  66.4,  68.7,  71.1,  73.2),
    PuntoOMS(9,   65.3,  67.7,  70.1,  72.6,  74.7),
    PuntoOMS(10,  66.5,  69.0,  71.5,  73.9,  76.1),
    PuntoOMS(11,  67.7,  70.3,  72.8,  75.3,  77.5),
    PuntoOMS(12,  68.9,  71.4,  74.0,  76.6,  78.9),
    PuntoOMS(15,  72.0,  74.5,  77.0,  79.7,  82.0),
    PuntoOMS(18,  74.9,  77.5,  80.2,  82.9,  85.3),
    PuntoOMS(21,  77.5,  80.2,  83.1,  85.9,  88.4),
    PuntoOMS(24,  80.0,  82.8,  85.7,  88.7,  91.2),
    PuntoOMS(27,  82.3,  85.2,  88.3,  91.4,  94.0),
    PuntoOMS(30,  84.5,  87.6,  90.7,  93.9,  96.7),
    PuntoOMS(33,  86.7,  89.8,  93.1,  96.4,  99.3),
    PuntoOMS(36,  88.3,  91.9,  95.1,  98.5, 101.5),
    PuntoOMS(42,  92.5,  95.9,  99.7, 103.3, 106.5),
    PuntoOMS(48,  95.0,  98.7, 102.7, 106.5, 110.0),
    PuntoOMS(54,  98.3, 102.1, 106.4, 110.5, 114.2),
    PuntoOMS(60, 101.0, 104.9, 109.4, 113.7, 117.7),
    // ── 61–144 m: WHO Growth Reference 2007 ─────────────────────────────
    PuntoOMS(66,  102.9, 106.6, 112.2, 116.3, 119.6),
    PuntoOMS(72,  105.5, 109.3, 115.1, 119.2, 122.7),
    PuntoOMS(78,  108.0, 112.0, 118.0, 122.0, 125.6),
    PuntoOMS(84,  110.5, 114.6, 120.8, 124.9, 128.5),
    PuntoOMS(90,  113.1, 117.3, 123.7, 127.8, 131.5),
    PuntoOMS(96,  115.7, 120.1, 126.6, 130.8, 134.5),
    PuntoOMS(102, 118.3, 122.8, 129.5, 133.7, 137.5),
    PuntoOMS(108, 121.0, 125.7, 132.5, 136.8, 140.7),
    PuntoOMS(114, 123.8, 128.6, 135.5, 140.0, 143.9),
    PuntoOMS(120, 126.6, 131.5, 138.6, 143.2, 147.2),
    PuntoOMS(126, 129.5, 134.5, 141.8, 146.4, 150.4),
    PuntoOMS(132, 132.5, 137.6, 145.0, 149.6, 153.6),
    PuntoOMS(138, 135.5, 140.7, 148.2, 152.7, 156.7),
    PuntoOMS(144, 138.4, 143.6, 151.2, 155.8, 159.8)
)

// ═══════════════════════════════════════════════════════════════════════════
// TABLAS IMC-PARA-EDAD OMS — NIÑOS (boys)
//
// 0–60 m  → WHO Child Growth Standards 2006 (bmi-for-age-boys-percentiles)
// 61–144 m → WHO Growth Reference 2007 (bmifa-boys-5-19years-per.pdf)
// ═══════════════════════════════════════════════════════════════════════════

private val OMS_IMC_NINOS = listOf(
    // ── 0–60 m: WHO Child Growth Standards 2006 ──────────────────────────
    PuntoOMS(0,  11.0, 12.0, 13.4, 15.1, 16.5),
    PuntoOMS(1,  12.8, 13.8, 15.3, 17.0, 18.3),
    PuntoOMS(2,  13.9, 15.0, 16.5, 18.2, 19.6),
    PuntoOMS(3,  14.3, 15.4, 16.9, 18.6, 20.0),
    PuntoOMS(4,  14.4, 15.5, 17.0, 18.7, 20.1),
    PuntoOMS(5,  14.3, 15.4, 16.9, 18.6, 20.0),
    PuntoOMS(6,  14.1, 15.2, 16.7, 18.4, 19.8),
    PuntoOMS(7,  13.9, 15.0, 16.5, 18.2, 19.6),
    PuntoOMS(8,  13.8, 14.8, 16.3, 18.0, 19.4),
    PuntoOMS(9,  13.7, 14.7, 16.2, 17.9, 19.3),
    PuntoOMS(10, 13.6, 14.6, 16.0, 17.7, 19.1),
    PuntoOMS(11, 13.5, 14.5, 15.9, 17.6, 19.0),
    PuntoOMS(12, 13.4, 14.4, 15.9, 17.4, 18.7),
    PuntoOMS(15, 13.4, 14.3, 15.7, 17.2, 18.4),
    PuntoOMS(18, 13.4, 14.3, 15.7, 17.1, 18.3),
    PuntoOMS(21, 13.4, 14.2, 15.6, 17.0, 18.2),
    PuntoOMS(24, 13.4, 14.2, 15.5, 16.9, 18.1),
    PuntoOMS(30, 13.2, 14.0, 15.3, 16.7, 18.0),
    PuntoOMS(36, 13.1, 13.8, 15.1, 16.6, 17.9),
    PuntoOMS(42, 13.0, 13.7, 15.0, 16.6, 18.0),
    PuntoOMS(48, 12.9, 13.6, 14.9, 16.6, 18.1),
    PuntoOMS(54, 12.8, 13.5, 14.9, 16.6, 18.2),
    PuntoOMS(60, 12.7, 13.4, 14.8, 16.6, 18.3),
    // ── 61–144 m: WHO Growth Reference 2007 ─────────────────────────────
    PuntoOMS(66,  13.1, 13.8, 15.3, 17.0, 18.5),
    PuntoOMS(72,  13.2, 14.0, 15.3, 16.9, 18.5),
    PuntoOMS(78,  13.2, 14.1, 15.4, 16.9, 18.5),
    PuntoOMS(84,  13.3, 14.2, 15.5, 17.1, 18.8),
    PuntoOMS(90,  13.3, 14.2, 15.6, 17.3, 19.0),
    PuntoOMS(96,  13.4, 14.4, 15.7, 17.5, 19.4),
    PuntoOMS(102, 13.5, 14.5, 15.9, 17.7, 19.7),
    PuntoOMS(108, 13.6, 14.6, 16.0, 18.0, 20.1),
    PuntoOMS(114, 13.7, 14.8, 16.2, 18.3, 20.5),
    PuntoOMS(120, 13.9, 14.9, 16.4, 18.6, 21.0),
    PuntoOMS(126, 14.0, 15.1, 16.7, 18.9, 21.5),
    PuntoOMS(132, 14.2, 15.3, 16.9, 19.3, 22.0),
    PuntoOMS(138, 14.4, 15.5, 17.2, 19.6, 22.5),
    PuntoOMS(144, 14.6, 15.7, 17.5, 20.1, 23.1)
)

// ═══════════════════════════════════════════════════════════════════════════
// TABLAS IMC-PARA-EDAD OMS — NIÑAS (girls)
//
// 0–60 m  → WHO Child Growth Standards 2006 (bmi-for-age-girls-percentiles)
// 61–144 m → WHO Growth Reference 2007 (bmifa-girls-5-19years-per.pdf)
// ═══════════════════════════════════════════════════════════════════════════

private val OMS_IMC_NINAS = listOf(
    // ── 0–60 m: WHO Child Growth Standards 2006 ──────────────────────────
    PuntoOMS(0,  10.8, 11.8, 13.3, 15.0, 16.4),
    PuntoOMS(1,  12.3, 13.3, 14.9, 16.6, 18.0),
    PuntoOMS(2,  13.4, 14.5, 16.0, 17.7, 19.1),
    PuntoOMS(3,  13.8, 14.9, 16.4, 18.1, 19.5),
    PuntoOMS(4,  13.9, 15.0, 16.5, 18.2, 19.6),
    PuntoOMS(5,  13.8, 14.9, 16.4, 18.1, 19.5),
    PuntoOMS(6,  13.6, 14.7, 16.2, 17.9, 19.3),
    PuntoOMS(7,  13.5, 14.5, 16.0, 17.7, 19.1),
    PuntoOMS(8,  13.3, 14.3, 15.9, 17.5, 19.0),
    PuntoOMS(9,  13.2, 14.2, 15.7, 17.4, 18.8),
    PuntoOMS(10, 13.1, 14.1, 15.6, 17.2, 18.6),
    PuntoOMS(11, 13.0, 14.0, 15.4, 17.1, 18.5),
    PuntoOMS(12, 12.9, 13.9, 15.3, 17.0, 18.4),
    PuntoOMS(15, 12.8, 13.7, 15.2, 16.8, 18.2),
    PuntoOMS(18, 12.8, 13.6, 15.1, 16.6, 18.1),
    PuntoOMS(21, 12.8, 13.6, 15.0, 16.5, 17.9),
    PuntoOMS(24, 12.9, 13.7, 15.0, 16.4, 17.8),
    PuntoOMS(30, 12.9, 13.6, 14.9, 16.3, 17.6),
    PuntoOMS(36, 12.9, 13.6, 14.8, 16.2, 17.6),
    PuntoOMS(42, 12.8, 13.5, 14.8, 16.2, 17.6),
    PuntoOMS(48, 12.7, 13.4, 14.7, 16.2, 17.7),
    PuntoOMS(54, 12.7, 13.3, 14.7, 16.2, 17.8),
    PuntoOMS(60, 12.6, 13.3, 14.7, 16.2, 17.9),
    // ── 61–144 m: WHO Growth Reference 2007 ─────────────────────────────
    PuntoOMS(66,  12.8, 13.8, 15.2, 17.0, 18.7),
    PuntoOMS(72,  12.8, 13.8, 15.3, 17.1, 18.9),
    PuntoOMS(78,  12.8, 13.9, 15.3, 17.2, 19.2),
    PuntoOMS(84,  12.9, 13.9, 15.4, 17.4, 19.4),
    PuntoOMS(90,  12.9, 14.0, 15.5, 17.6, 19.8),
    PuntoOMS(96,  13.0, 14.1, 15.7, 17.8, 20.2),
    PuntoOMS(102, 13.1, 14.2, 15.9, 18.1, 20.6),
    PuntoOMS(108, 13.3, 14.4, 16.1, 18.4, 21.1),
    PuntoOMS(114, 13.4, 14.6, 16.4, 18.8, 21.6),
    PuntoOMS(120, 13.6, 14.8, 16.6, 19.1, 22.1),
    PuntoOMS(126, 13.8, 15.0, 16.9, 19.5, 22.6),
    PuntoOMS(132, 14.0, 15.3, 17.2, 20.0, 23.2),
    PuntoOMS(138, 14.3, 15.6, 17.6, 20.4, 23.8),
    PuntoOMS(144, 14.6, 15.9, 18.0, 20.9, 24.4)
)

// ═══════════════════════════════════════════════════════════════════════════
// TABLAS PESO-PARA-LONGITUD (WFL) — WHO Child Growth Standards 2006 (0–2 años / 45–110 cm)
// https://www.who.int/tools/child-growth-standards/standards/weight-for-length-height
// ═══════════════════════════════════════════════════════════════════════════

val OMS_WFL_NINAS: List<PuntoOMSLongitud> = listOf(
    PuntoOMSLongitud(45.0, 2.1, 2.2, 2.5, 2.7, 2.9, 1.9, 2.1, 2.3, 2.5, 2.7, 3.0, 3.3),
    PuntoOMSLongitud(45.5, 2.2, 2.3, 2.5, 2.8, 3.0, 2.0, 2.1, 2.3, 2.5, 2.8, 3.1, 3.4),
    PuntoOMSLongitud(46.0, 2.2, 2.4, 2.6, 2.9, 3.1, 2.0, 2.2, 2.4, 2.6, 2.9, 3.2, 3.5),
    PuntoOMSLongitud(46.5, 2.3, 2.5, 2.7, 3.0, 3.2, 2.1, 2.3, 2.5, 2.7, 3.0, 3.3, 3.6),
    PuntoOMSLongitud(47.0, 2.4, 2.6, 2.8, 3.1, 3.3, 2.2, 2.4, 2.6, 2.8, 3.1, 3.4, 3.7),
    PuntoOMSLongitud(47.5, 2.4, 2.6, 2.9, 3.2, 3.4, 2.2, 2.4, 2.6, 2.9, 3.2, 3.5, 3.8),
    PuntoOMSLongitud(48.0, 2.5, 2.7, 3.0, 3.3, 3.5, 2.3, 2.5, 2.7, 3.0, 3.3, 3.6, 4.0),
    PuntoOMSLongitud(48.5, 2.6, 2.8, 3.1, 3.4, 3.7, 2.4, 2.6, 2.8, 3.1, 3.4, 3.7, 4.1),
    PuntoOMSLongitud(49.0, 2.7, 2.9, 3.2, 3.5, 3.8, 2.4, 2.6, 2.9, 3.2, 3.5, 3.8, 4.2),
    PuntoOMSLongitud(49.5, 2.8, 3.0, 3.3, 3.6, 3.9, 2.5, 2.7, 3.0, 3.3, 3.6, 3.9, 4.3),
    PuntoOMSLongitud(50.0, 2.8, 3.1, 3.4, 3.7, 4.0, 2.6, 2.8, 3.1, 3.4, 3.7, 4.0, 4.5),
    PuntoOMSLongitud(50.5, 2.9, 3.2, 3.5, 3.8, 4.1, 2.7, 2.9, 3.2, 3.5, 3.8, 4.2, 4.6),
    PuntoOMSLongitud(51.0, 3.0, 3.2, 3.6, 3.9, 4.3, 2.8, 3.0, 3.3, 3.6, 3.9, 4.3, 4.8),
    PuntoOMSLongitud(51.5, 3.1, 3.4, 3.7, 4.0, 4.4, 2.8, 3.1, 3.4, 3.7, 4.0, 4.4, 4.9),
    PuntoOMSLongitud(52.0, 3.2, 3.5, 3.8, 4.2, 4.5, 2.9, 3.2, 3.5, 3.8, 4.2, 4.6, 5.1),
    PuntoOMSLongitud(52.5, 3.3, 3.6, 3.9, 4.3, 4.7, 3.0, 3.3, 3.6, 3.9, 4.3, 4.7, 5.2),
    PuntoOMSLongitud(53.0, 3.4, 3.7, 4.0, 4.4, 4.8, 3.1, 3.4, 3.7, 4.0, 4.4, 4.9, 5.4),
    PuntoOMSLongitud(53.5, 3.5, 3.8, 4.2, 4.6, 5.0, 3.2, 3.5, 3.8, 4.2, 4.6, 5.0, 5.5),
    PuntoOMSLongitud(54.0, 3.6, 3.9, 4.3, 4.7, 5.1, 3.3, 3.6, 3.9, 4.3, 4.7, 5.2, 5.7),
    PuntoOMSLongitud(54.5, 3.7, 4.0, 4.4, 4.9, 5.3, 3.4, 3.7, 4.0, 4.4, 4.8, 5.3, 5.9),
    PuntoOMSLongitud(55.0, 3.9, 4.1, 4.5, 5.0, 5.4, 3.5, 3.8, 4.2, 4.5, 5.0, 5.5, 6.1),
    PuntoOMSLongitud(55.5, 4.0, 4.3, 4.7, 5.2, 5.6, 3.6, 3.9, 4.3, 4.7, 5.1, 5.7, 6.3),
    PuntoOMSLongitud(56.0, 4.1, 4.4, 4.8, 5.3, 5.8, 3.7, 4.0, 4.4, 4.8, 5.3, 5.8, 6.4),
    PuntoOMSLongitud(56.5, 4.2, 4.5, 5.0, 5.5, 5.9, 3.8, 4.1, 4.5, 5.0, 5.4, 6.0, 6.6),
    PuntoOMSLongitud(57.0, 4.3, 4.6, 5.1, 5.6, 6.1, 3.9, 4.3, 4.6, 5.1, 5.6, 6.1, 6.8),
    PuntoOMSLongitud(57.5, 4.4, 4.8, 5.2, 5.7, 6.2, 4.0, 4.4, 4.8, 5.2, 5.7, 6.3, 7.0),
    PuntoOMSLongitud(58.0, 4.5, 4.9, 5.4, 5.9, 6.4, 4.1, 4.5, 4.9, 5.4, 5.9, 6.5, 7.1),
    PuntoOMSLongitud(58.5, 4.6, 5.0, 5.5, 6.0, 6.5, 4.2, 4.6, 5.0, 5.5, 6.0, 6.6, 7.3),
    PuntoOMSLongitud(59.0, 4.8, 5.1, 5.6, 6.2, 6.7, 4.3, 4.7, 5.1, 5.6, 6.2, 6.8, 7.5),
    PuntoOMSLongitud(59.5, 4.9, 5.2, 5.7, 6.3, 6.9, 4.4, 4.8, 5.3, 5.7, 6.3, 6.9, 7.7),
    PuntoOMSLongitud(60.0, 5.0, 5.4, 5.9, 6.5, 7.0, 4.5, 4.9, 5.4, 5.9, 6.4, 7.1, 7.8),
    PuntoOMSLongitud(60.5, 5.1, 5.5, 6.0, 6.6, 7.2, 4.6, 5.0, 5.5, 6.0, 6.6, 7.3, 8.0),
    PuntoOMSLongitud(61.0, 5.2, 5.6, 6.1, 6.7, 7.3, 4.7, 5.1, 5.6, 6.1, 6.7, 7.4, 8.2),
    PuntoOMSLongitud(61.5, 5.3, 5.7, 6.3, 6.9, 7.5, 4.8, 5.2, 5.7, 6.3, 6.9, 7.6, 8.4),
    PuntoOMSLongitud(62.0, 5.4, 5.8, 6.4, 7.0, 7.6, 4.9, 5.3, 5.8, 6.4, 7.0, 7.7, 8.5),
    PuntoOMSLongitud(62.5, 5.5, 5.9, 6.5, 7.2, 7.8, 5.0, 5.4, 5.9, 6.5, 7.1, 7.8, 8.7),
    PuntoOMSLongitud(63.0, 5.6, 6.0, 6.6, 7.3, 7.9, 5.1, 5.5, 6.0, 6.6, 7.3, 8.0, 8.8),
    PuntoOMSLongitud(63.5, 5.7, 6.1, 6.7, 7.4, 8.0, 5.2, 5.6, 6.2, 6.7, 7.4, 8.1, 9.0),
    PuntoOMSLongitud(64.0, 5.8, 6.2, 6.9, 7.5, 8.2, 5.3, 5.7, 6.3, 6.9, 7.5, 8.3, 9.1),
    PuntoOMSLongitud(64.5, 5.9, 6.3, 7.0, 7.7, 8.3, 5.4, 5.8, 6.4, 7.0, 7.6, 8.4, 9.3),
    PuntoOMSLongitud(65.0, 6.0, 6.5, 7.1, 7.8, 8.5, 5.5, 5.9, 6.5, 7.1, 7.8, 8.6, 9.5),
    PuntoOMSLongitud(65.5, 6.1, 6.6, 7.2, 7.9, 8.6, 5.5, 6.0, 6.6, 7.2, 7.9, 8.7, 9.6),
    PuntoOMSLongitud(66.0, 6.2, 6.7, 7.3, 8.0, 8.7, 5.6, 6.1, 6.7, 7.3, 8.0, 8.8, 9.8),
    PuntoOMSLongitud(66.5, 6.3, 6.8, 7.4, 8.2, 8.9, 5.7, 6.2, 6.8, 7.4, 8.1, 9.0, 9.9),
    PuntoOMSLongitud(67.0, 6.4, 6.9, 7.5, 8.3, 9.0, 5.8, 6.3, 6.9, 7.5, 8.3, 9.1, 10.0),
    PuntoOMSLongitud(67.5, 6.5, 7.0, 7.6, 8.4, 9.1, 5.9, 6.4, 7.0, 7.6, 8.4, 9.2, 10.2),
    PuntoOMSLongitud(68.0, 6.6, 7.1, 7.7, 8.5, 9.2, 6.0, 6.5, 7.1, 7.7, 8.5, 9.4, 10.3),
    PuntoOMSLongitud(68.5, 6.7, 7.2, 7.9, 8.6, 9.4, 6.1, 6.6, 7.2, 7.9, 8.6, 9.5, 10.5),
    PuntoOMSLongitud(69.0, 6.7, 7.3, 8.0, 8.8, 9.5, 6.1, 6.7, 7.3, 8.0, 8.7, 9.6, 10.6),
    PuntoOMSLongitud(69.5, 6.8, 7.3, 8.1, 8.9, 9.6, 6.2, 6.8, 7.4, 8.1, 8.8, 9.7, 10.7),
    PuntoOMSLongitud(70.0, 6.9, 7.4, 8.2, 9.0, 9.7, 6.3, 6.9, 7.5, 8.2, 9.0, 9.9, 10.9),
    PuntoOMSLongitud(70.5, 7.0, 7.5, 8.3, 9.1, 9.9, 6.4, 6.9, 7.6, 8.3, 9.1, 10.0, 11.0),
    PuntoOMSLongitud(71.0, 7.1, 7.6, 8.4, 9.2, 10.0, 6.5, 7.0, 7.7, 8.4, 9.2, 10.1, 11.1),
    PuntoOMSLongitud(71.5, 7.2, 7.7, 8.5, 9.3, 10.1, 6.5, 7.1, 7.7, 8.5, 9.3, 10.2, 11.3),
    PuntoOMSLongitud(72.0, 7.3, 7.8, 8.6, 9.4, 10.2, 6.6, 7.2, 7.8, 8.6, 9.4, 10.3, 11.4),
    PuntoOMSLongitud(72.5, 7.4, 7.9, 8.7, 9.5, 10.3, 6.7, 7.3, 7.9, 8.7, 9.5, 10.5, 11.5),
    PuntoOMSLongitud(73.0, 7.4, 8.0, 8.8, 9.6, 10.4, 6.8, 7.4, 8.0, 8.8, 9.6, 10.6, 11.7),
    PuntoOMSLongitud(73.5, 7.5, 8.1, 8.9, 9.7, 10.6, 6.9, 7.4, 8.1, 8.9, 9.7, 10.7, 11.8),
    PuntoOMSLongitud(74.0, 7.6, 8.2, 9.0, 9.9, 10.7, 6.9, 7.5, 8.2, 9.0, 9.8, 10.8, 11.9),
    PuntoOMSLongitud(74.5, 7.7, 8.3, 9.1, 10.0, 10.8, 7.0, 7.6, 8.3, 9.1, 9.9, 10.9, 12.0),
    PuntoOMSLongitud(75.0, 7.8, 8.3, 9.1, 10.1, 10.9, 7.1, 7.7, 8.4, 9.1, 10.0, 11.0, 12.2),
    PuntoOMSLongitud(75.5, 7.8, 8.4, 9.2, 10.2, 11.0, 7.1, 7.8, 8.5, 9.2, 10.1, 11.1, 12.3),
    PuntoOMSLongitud(76.0, 7.9, 8.5, 9.3, 10.3, 11.1, 7.2, 7.8, 8.5, 9.3, 10.2, 11.2, 12.4),
    PuntoOMSLongitud(76.5, 8.0, 8.6, 9.4, 10.4, 11.2, 7.3, 7.9, 8.6, 9.4, 10.3, 11.4, 12.5),
    PuntoOMSLongitud(77.0, 8.1, 8.7, 9.5, 10.5, 11.3, 7.4, 8.0, 8.7, 9.5, 10.4, 11.5, 12.6),
    PuntoOMSLongitud(77.5, 8.2, 8.8, 9.6, 10.6, 11.4, 7.4, 8.1, 8.8, 9.6, 10.5, 11.6, 12.8),
    PuntoOMSLongitud(78.0, 8.2, 8.9, 9.7, 10.7, 11.5, 7.5, 8.2, 8.9, 9.7, 10.6, 11.7, 12.9),
    PuntoOMSLongitud(78.5, 8.3, 8.9, 9.8, 10.8, 11.7, 7.6, 8.2, 9.0, 9.8, 10.7, 11.8, 13.0),
    PuntoOMSLongitud(79.0, 8.4, 9.0, 9.9, 10.9, 11.8, 7.7, 8.3, 9.1, 9.9, 10.8, 11.9, 13.1),
    PuntoOMSLongitud(79.5, 8.5, 9.1, 10.0, 11.0, 11.9, 7.7, 8.4, 9.1, 10.0, 10.9, 12.0, 13.3),
    PuntoOMSLongitud(80.0, 8.6, 9.2, 10.1, 11.1, 12.0, 7.8, 8.5, 9.2, 10.1, 11.0, 12.1, 13.4),
    PuntoOMSLongitud(80.5, 8.7, 9.3, 10.2, 11.2, 12.1, 7.9, 8.6, 9.3, 10.2, 11.2, 12.3, 13.5),
    PuntoOMSLongitud(81.0, 8.8, 9.4, 10.3, 11.3, 12.2, 8.0, 8.7, 9.4, 10.3, 11.3, 12.4, 13.7),
    PuntoOMSLongitud(81.5, 8.8, 9.5, 10.4, 11.4, 12.4, 8.1, 8.8, 9.5, 10.4, 11.4, 12.5, 13.8),
    PuntoOMSLongitud(82.0, 8.9, 9.6, 10.5, 11.6, 12.5, 8.1, 8.8, 9.6, 10.5, 11.5, 12.6, 13.9),
    PuntoOMSLongitud(82.5, 9.0, 9.7, 10.6, 11.7, 12.6, 8.2, 8.9, 9.7, 10.6, 11.6, 12.8, 14.1),
    PuntoOMSLongitud(83.0, 9.1, 9.8, 10.7, 11.8, 12.8, 8.3, 9.0, 9.8, 10.7, 11.8, 12.9, 14.2),
    PuntoOMSLongitud(83.5, 9.2, 9.9, 10.9, 11.9, 12.9, 8.4, 9.1, 9.9, 10.9, 11.9, 13.1, 14.4),
    PuntoOMSLongitud(84.0, 9.3, 10.0, 11.0, 12.1, 13.1, 8.5, 9.2, 10.1, 11.0, 12.0, 13.2, 14.5),
    PuntoOMSLongitud(84.5, 9.4, 10.1, 11.1, 12.2, 13.2, 8.6, 9.3, 10.2, 11.1, 12.1, 13.3, 14.7),
    PuntoOMSLongitud(85.0, 9.5, 10.2, 11.2, 12.3, 13.3, 8.7, 9.4, 10.3, 11.2, 12.3, 13.5, 14.9),
    PuntoOMSLongitud(85.5, 9.6, 10.4, 11.3, 12.5, 13.5, 8.8, 9.5, 10.4, 11.3, 12.4, 13.6, 15.0),
    PuntoOMSLongitud(86.0, 9.8, 10.5, 11.5, 12.6, 13.6, 8.9, 9.7, 10.5, 11.5, 12.6, 13.8, 15.2),
    PuntoOMSLongitud(86.5, 9.9, 10.6, 11.6, 12.7, 13.8, 9.0, 9.8, 10.6, 11.6, 12.7, 13.9, 15.4),
    PuntoOMSLongitud(87.0, 10.0, 10.7, 11.7, 12.9, 13.9, 9.1, 9.9, 10.7, 11.7, 12.8, 14.1, 15.5),
    PuntoOMSLongitud(87.5, 10.1, 10.8, 11.8, 13.0, 14.1, 9.2, 10.0, 10.9, 11.8, 13.0, 14.2, 15.7),
    PuntoOMSLongitud(88.0, 10.2, 10.9, 12.0, 13.2, 14.2, 9.3, 10.1, 11.0, 12.0, 13.1, 14.4, 15.9),
    PuntoOMSLongitud(88.5, 10.3, 11.0, 12.1, 13.3, 14.4, 9.4, 10.2, 11.1, 12.1, 13.2, 14.5, 16.0),
    PuntoOMSLongitud(89.0, 10.4, 11.2, 12.2, 13.4, 14.5, 9.5, 10.3, 11.2, 12.2, 13.4, 14.7, 16.2),
    PuntoOMSLongitud(89.5, 10.5, 11.3, 12.3, 13.6, 14.7, 9.6, 10.4, 11.3, 12.3, 13.5, 14.8, 16.4),
    PuntoOMSLongitud(90.0, 10.6, 11.4, 12.5, 13.7, 14.8, 9.7, 10.5, 11.4, 12.5, 13.7, 15.0, 16.5),
    PuntoOMSLongitud(90.5, 10.7, 11.5, 12.6, 13.8, 15.0, 9.8, 10.6, 11.5, 12.6, 13.8, 15.1, 16.7),
    PuntoOMSLongitud(91.0, 10.8, 11.6, 12.7, 14.0, 15.1, 9.9, 10.7, 11.7, 12.7, 13.9, 15.3, 16.9),
    PuntoOMSLongitud(91.5, 10.9, 11.7, 12.8, 14.1, 15.3, 10.0, 10.8, 11.8, 12.8, 14.1, 15.5, 17.0),
    PuntoOMSLongitud(92.0, 11.0, 11.8, 13.0, 14.2, 15.4, 10.1, 10.9, 11.9, 13.0, 14.2, 15.6, 17.2),
    PuntoOMSLongitud(92.5, 11.1, 12.0, 13.1, 14.4, 15.6, 10.1, 11.0, 12.0, 13.1, 14.3, 15.8, 17.4),
    PuntoOMSLongitud(93.0, 11.2, 12.1, 13.2, 14.5, 15.7, 10.2, 11.1, 12.1, 13.2, 14.5, 15.9, 17.5),
    PuntoOMSLongitud(93.5, 11.3, 12.2, 13.3, 14.7, 15.9, 10.3, 11.2, 12.2, 13.3, 14.6, 16.1, 17.7),
    PuntoOMSLongitud(94.0, 11.4, 12.3, 13.5, 14.8, 16.0, 10.4, 11.3, 12.3, 13.5, 14.7, 16.2, 17.9),
    PuntoOMSLongitud(94.5, 11.5, 12.4, 13.6, 14.9, 16.2, 10.5, 11.4, 12.4, 13.6, 14.9, 16.4, 18.0),
    PuntoOMSLongitud(95.0, 11.6, 12.5, 13.7, 15.1, 16.3, 10.6, 11.5, 12.6, 13.7, 15.0, 16.5, 18.2),
    PuntoOMSLongitud(95.5, 11.8, 12.6, 13.8, 15.2, 16.5, 10.7, 11.6, 12.7, 13.8, 15.2, 16.7, 18.4),
    PuntoOMSLongitud(96.0, 11.9, 12.7, 14.0, 15.4, 16.6, 10.8, 11.7, 12.8, 14.0, 15.3, 16.8, 18.6),
    PuntoOMSLongitud(96.5, 12.0, 12.9, 14.1, 15.5, 16.8, 10.9, 11.8, 12.9, 14.1, 15.4, 17.0, 18.7),
    PuntoOMSLongitud(97.0, 12.1, 13.0, 14.2, 15.6, 16.9, 11.0, 12.0, 13.0, 14.2, 15.6, 17.1, 18.9),
    PuntoOMSLongitud(97.5, 12.2, 13.1, 14.4, 15.8, 17.1, 11.1, 12.1, 13.1, 14.4, 15.7, 17.3, 19.1),
    PuntoOMSLongitud(98.0, 12.3, 13.2, 14.5, 15.9, 17.3, 11.2, 12.2, 13.3, 14.5, 15.9, 17.5, 19.3),
    PuntoOMSLongitud(98.5, 12.4, 13.3, 14.6, 16.1, 17.4, 11.3, 12.3, 13.4, 14.6, 16.0, 17.6, 19.5),
    PuntoOMSLongitud(99.0, 12.5, 13.5, 14.8, 16.2, 17.6, 11.4, 12.4, 13.5, 14.8, 16.2, 17.8, 19.6),
    PuntoOMSLongitud(99.5, 12.6, 13.6, 14.9, 16.4, 17.8, 11.5, 12.5, 13.6, 14.9, 16.3, 18.0, 19.8),
    PuntoOMSLongitud(100.0, 12.7, 13.7, 15.0, 16.5, 17.9, 11.6, 12.6, 13.7, 15.0, 16.5, 18.1, 20.0),
    PuntoOMSLongitud(100.5, 12.9, 13.8, 15.2, 16.7, 18.1, 11.7, 12.7, 13.9, 15.2, 16.6, 18.3, 20.2),
    PuntoOMSLongitud(101.0, 13.0, 14.0, 15.3, 16.9, 18.3, 11.8, 12.8, 14.0, 15.3, 16.8, 18.5, 20.4),
    PuntoOMSLongitud(101.5, 13.1, 14.1, 15.5, 17.0, 18.5, 11.9, 13.0, 14.1, 15.5, 17.0, 18.7, 20.6),
    PuntoOMSLongitud(102.0, 13.2, 14.2, 15.6, 17.2, 18.6, 12.0, 13.1, 14.3, 15.6, 17.1, 18.9, 20.8),
    PuntoOMSLongitud(102.5, 13.3, 14.4, 15.8, 17.4, 18.8, 12.1, 13.2, 14.4, 15.8, 17.3, 19.0, 21.0),
    PuntoOMSLongitud(103.0, 13.5, 14.5, 15.9, 17.5, 19.0, 12.3, 13.3, 14.5, 15.9, 17.5, 19.2, 21.3),
    PuntoOMSLongitud(103.5, 13.6, 14.6, 16.1, 17.7, 19.2, 12.4, 13.5, 14.7, 16.1, 17.6, 19.4, 21.5),
    PuntoOMSLongitud(104.0, 13.7, 14.8, 16.2, 17.9, 19.4, 12.5, 13.6, 14.8, 16.2, 17.8, 19.6, 21.7),
    PuntoOMSLongitud(104.5, 13.9, 14.9, 16.4, 18.1, 19.6, 12.6, 13.7, 15.0, 16.4, 18.0, 19.8, 21.9),
    PuntoOMSLongitud(105.0, 14.0, 15.1, 16.5, 18.2, 19.8, 12.7, 13.8, 15.1, 16.5, 18.2, 20.0, 22.2),
    PuntoOMSLongitud(105.5, 14.1, 15.2, 16.7, 18.4, 20.0, 12.8, 14.0, 15.3, 16.7, 18.4, 20.2, 22.4),
    PuntoOMSLongitud(106.0, 14.3, 15.4, 16.9, 18.6, 20.2, 13.0, 14.1, 15.4, 16.9, 18.5, 20.5, 22.6),
    PuntoOMSLongitud(106.5, 14.4, 15.5, 17.1, 18.8, 20.4, 13.1, 14.3, 15.6, 17.1, 18.7, 20.7, 22.9),
    PuntoOMSLongitud(107.0, 14.5, 15.7, 17.2, 19.0, 20.6, 13.2, 14.4, 15.7, 17.2, 18.9, 20.9, 23.1),
    PuntoOMSLongitud(107.5, 14.7, 15.8, 17.4, 19.2, 20.9, 13.3, 14.5, 15.9, 17.4, 19.1, 21.1, 23.4),
    PuntoOMSLongitud(108.0, 14.8, 16.0, 17.6, 19.4, 21.1, 13.5, 14.7, 16.0, 17.6, 19.3, 21.3, 23.6),
    PuntoOMSLongitud(108.5, 15.0, 16.2, 17.8, 19.6, 21.3, 13.6, 14.8, 16.2, 17.8, 19.5, 21.6, 23.9),
    PuntoOMSLongitud(109.0, 15.1, 16.3, 18.0, 19.8, 21.5, 13.7, 15.0, 16.4, 18.0, 19.7, 21.8, 24.2),
    PuntoOMSLongitud(109.5, 15.3, 16.5, 18.1, 20.0, 21.8, 13.9, 15.1, 16.5, 18.1, 20.0, 22.0, 24.4),
    PuntoOMSLongitud(110.0, 15.4, 16.7, 18.3, 20.2, 22.0, 14.0, 15.3, 16.7, 18.3, 20.2, 22.3, 24.7)
)

val OMS_WFL_NINOS: List<PuntoOMSLongitud> = listOf(
    PuntoOMSLongitud(45.0, 2.1, 2.2, 2.4, 2.7, 2.9, 1.9, 2.0, 2.2, 2.4, 2.7, 3.0, 3.3),
    PuntoOMSLongitud(45.5, 2.1, 2.3, 2.5, 2.8, 3.0, 1.9, 2.1, 2.3, 2.5, 2.8, 3.1, 3.4),
    PuntoOMSLongitud(46.0, 2.2, 2.4, 2.6, 2.9, 3.1, 2.0, 2.2, 2.4, 2.6, 2.9, 3.1, 3.5),
    PuntoOMSLongitud(46.5, 2.3, 2.5, 2.7, 3.0, 3.2, 2.1, 2.3, 2.5, 2.7, 3.0, 3.2, 3.6),
    PuntoOMSLongitud(47.0, 2.4, 2.5, 2.8, 3.1, 3.3, 2.1, 2.3, 2.5, 2.8, 3.0, 3.3, 3.7),
    PuntoOMSLongitud(47.5, 2.4, 2.6, 2.9, 3.1, 3.4, 2.2, 2.4, 2.6, 2.9, 3.1, 3.4, 3.8),
    PuntoOMSLongitud(48.0, 2.5, 2.7, 2.9, 3.2, 3.5, 2.3, 2.5, 2.7, 2.9, 3.2, 3.6, 3.9),
    PuntoOMSLongitud(48.5, 2.6, 2.8, 3.0, 3.3, 3.6, 2.3, 2.6, 2.8, 3.0, 3.3, 3.7, 4.0),
    PuntoOMSLongitud(49.0, 2.7, 2.9, 3.1, 3.4, 3.7, 2.4, 2.6, 2.9, 3.1, 3.4, 3.8, 4.2),
    PuntoOMSLongitud(49.5, 2.7, 2.9, 3.2, 3.5, 3.8, 2.5, 2.7, 3.0, 3.2, 3.5, 3.9, 4.3),
    PuntoOMSLongitud(50.0, 2.8, 3.0, 3.3, 3.7, 4.0, 2.6, 2.8, 3.0, 3.3, 3.6, 4.0, 4.4),
    PuntoOMSLongitud(50.5, 2.9, 3.1, 3.4, 3.8, 4.1, 2.7, 2.9, 3.1, 3.4, 3.8, 4.1, 4.5),
    PuntoOMSLongitud(51.0, 3.0, 3.2, 3.5, 3.9, 4.2, 2.7, 3.0, 3.2, 3.5, 3.9, 4.2, 4.7),
    PuntoOMSLongitud(51.5, 3.1, 3.3, 3.6, 4.0, 4.3, 2.8, 3.1, 3.3, 3.6, 4.0, 4.4, 4.8),
    PuntoOMSLongitud(52.0, 3.2, 3.4, 3.8, 4.1, 4.5, 2.9, 3.2, 3.5, 3.8, 4.1, 4.5, 5.0),
    PuntoOMSLongitud(52.5, 3.3, 3.6, 3.9, 4.3, 4.6, 3.0, 3.3, 3.6, 3.9, 4.2, 4.6, 5.1),
    PuntoOMSLongitud(53.0, 3.4, 3.7, 4.0, 4.4, 4.7, 3.1, 3.4, 3.7, 4.0, 4.4, 4.8, 5.3),
    PuntoOMSLongitud(53.5, 3.5, 3.8, 4.1, 4.5, 4.9, 3.2, 3.5, 3.8, 4.1, 4.5, 4.9, 5.4),
    PuntoOMSLongitud(54.0, 3.6, 3.9, 4.3, 4.7, 5.0, 3.3, 3.6, 3.9, 4.3, 4.7, 5.1, 5.6),
    PuntoOMSLongitud(54.5, 3.8, 4.0, 4.4, 4.8, 5.2, 3.4, 3.7, 4.0, 4.4, 4.8, 5.3, 5.8),
    PuntoOMSLongitud(55.0, 3.9, 4.2, 4.5, 5.0, 5.4, 3.6, 3.8, 4.2, 4.5, 5.0, 5.4, 6.0),
    PuntoOMSLongitud(55.5, 4.0, 4.3, 4.7, 5.1, 5.5, 3.7, 4.0, 4.3, 4.7, 5.1, 5.6, 6.1),
    PuntoOMSLongitud(56.0, 4.1, 4.4, 4.8, 5.3, 5.7, 3.8, 4.1, 4.4, 4.8, 5.3, 5.8, 6.3),
    PuntoOMSLongitud(56.5, 4.3, 4.6, 5.0, 5.4, 5.9, 3.9, 4.2, 4.6, 5.0, 5.4, 5.9, 6.5),
    PuntoOMSLongitud(57.0, 4.4, 4.7, 5.1, 5.6, 6.0, 4.0, 4.3, 4.7, 5.1, 5.6, 6.1, 6.7),
    PuntoOMSLongitud(57.5, 4.5, 4.8, 5.3, 5.8, 6.2, 4.1, 4.5, 4.9, 5.3, 5.7, 6.3, 6.9),
    PuntoOMSLongitud(58.0, 4.6, 5.0, 5.4, 5.9, 6.4, 4.3, 4.6, 5.0, 5.4, 5.9, 6.4, 7.1),
    PuntoOMSLongitud(58.5, 4.8, 5.1, 5.6, 6.1, 6.5, 4.4, 4.7, 5.1, 5.6, 6.1, 6.6, 7.2),
    PuntoOMSLongitud(59.0, 4.9, 5.2, 5.7, 6.2, 6.7, 4.5, 4.8, 5.3, 5.7, 6.2, 6.8, 7.4),
    PuntoOMSLongitud(59.5, 5.0, 5.4, 5.9, 6.4, 6.9, 4.6, 5.0, 5.4, 5.9, 6.4, 7.0, 7.6),
    PuntoOMSLongitud(60.0, 5.1, 5.5, 6.0, 6.5, 7.0, 4.7, 5.1, 5.5, 6.0, 6.5, 7.1, 7.8),
    PuntoOMSLongitud(60.5, 5.3, 5.6, 6.1, 6.7, 7.2, 4.8, 5.2, 5.6, 6.1, 6.7, 7.3, 8.0),
    PuntoOMSLongitud(61.0, 5.4, 5.8, 6.3, 6.8, 7.4, 4.9, 5.3, 5.8, 6.3, 6.8, 7.4, 8.1),
    PuntoOMSLongitud(61.5, 5.5, 5.9, 6.4, 7.0, 7.5, 5.0, 5.4, 5.9, 6.4, 7.0, 7.6, 8.3),
    PuntoOMSLongitud(62.0, 5.6, 6.0, 6.5, 7.1, 7.7, 5.1, 5.6, 6.0, 6.5, 7.1, 7.7, 8.5),
    PuntoOMSLongitud(62.5, 5.7, 6.1, 6.7, 7.3, 7.8, 5.2, 5.7, 6.1, 6.7, 7.2, 7.9, 8.6),
    PuntoOMSLongitud(63.0, 5.8, 6.2, 6.8, 7.4, 8.0, 5.3, 5.8, 6.2, 6.8, 7.4, 8.0, 8.8),
    PuntoOMSLongitud(63.5, 5.9, 6.3, 6.9, 7.5, 8.1, 5.4, 5.9, 6.4, 6.9, 7.5, 8.2, 8.9),
    PuntoOMSLongitud(64.0, 6.0, 6.5, 7.0, 7.7, 8.2, 5.5, 6.0, 6.5, 7.0, 7.6, 8.3, 9.1),
    PuntoOMSLongitud(64.5, 6.1, 6.6, 7.1, 7.8, 8.4, 5.6, 6.1, 6.6, 7.1, 7.8, 8.5, 9.3),
    PuntoOMSLongitud(65.0, 6.3, 6.7, 7.3, 7.9, 8.5, 5.7, 6.2, 6.7, 7.3, 7.9, 8.6, 9.4),
    PuntoOMSLongitud(65.5, 6.4, 6.8, 7.4, 8.1, 8.7, 5.8, 6.3, 6.8, 7.4, 8.0, 8.7, 9.6),
    PuntoOMSLongitud(66.0, 6.5, 6.9, 7.5, 8.2, 8.8, 5.9, 6.4, 6.9, 7.5, 8.2, 8.9, 9.7),
    PuntoOMSLongitud(66.5, 6.6, 7.0, 7.6, 8.3, 8.9, 6.0, 6.5, 7.0, 7.6, 8.3, 9.0, 9.9),
    PuntoOMSLongitud(67.0, 6.7, 7.1, 7.7, 8.4, 9.1, 6.1, 6.6, 7.1, 7.7, 8.4, 9.2, 10.0),
    PuntoOMSLongitud(67.5, 6.8, 7.2, 7.9, 8.6, 9.2, 6.2, 6.7, 7.2, 7.9, 8.5, 9.3, 10.2),
    PuntoOMSLongitud(68.0, 6.9, 7.3, 8.0, 8.7, 9.3, 6.3, 6.8, 7.3, 8.0, 8.7, 9.4, 10.3),
    PuntoOMSLongitud(68.5, 7.0, 7.4, 8.1, 8.8, 9.5, 6.4, 6.9, 7.5, 8.1, 8.8, 9.6, 10.5),
    PuntoOMSLongitud(69.0, 7.1, 7.5, 8.2, 8.9, 9.6, 6.5, 7.0, 7.6, 8.2, 8.9, 9.7, 10.6),
    PuntoOMSLongitud(69.5, 7.1, 7.6, 8.3, 9.1, 9.7, 6.6, 7.1, 7.7, 8.3, 9.0, 9.8, 10.8),
    PuntoOMSLongitud(70.0, 7.2, 7.7, 8.4, 9.2, 9.9, 6.6, 7.2, 7.8, 8.4, 9.2, 10.0, 10.9),
    PuntoOMSLongitud(70.5, 7.3, 7.8, 8.5, 9.3, 10.0, 6.7, 7.3, 7.9, 8.5, 9.3, 10.1, 11.1),
    PuntoOMSLongitud(71.0, 7.4, 8.0, 8.6, 9.4, 10.1, 6.8, 7.4, 8.0, 8.6, 9.4, 10.2, 11.2),
    PuntoOMSLongitud(71.5, 7.5, 8.1, 8.8, 9.6, 10.3, 6.9, 7.5, 8.1, 8.8, 9.5, 10.4, 11.3),
    PuntoOMSLongitud(72.0, 7.6, 8.2, 8.9, 9.7, 10.4, 7.0, 7.6, 8.2, 8.9, 9.6, 10.5, 11.5),
    PuntoOMSLongitud(72.5, 7.7, 8.3, 9.0, 9.8, 10.5, 7.1, 7.6, 8.3, 9.0, 9.8, 10.6, 11.6),
    PuntoOMSLongitud(73.0, 7.8, 8.4, 9.1, 9.9, 10.7, 7.2, 7.7, 8.4, 9.1, 9.9, 10.8, 11.8),
    PuntoOMSLongitud(73.5, 7.9, 8.4, 9.2, 10.0, 10.8, 7.2, 7.8, 8.5, 9.2, 10.0, 10.9, 11.9),
    PuntoOMSLongitud(74.0, 8.0, 8.5, 9.3, 10.1, 10.9, 7.3, 7.9, 8.6, 9.3, 10.1, 11.0, 12.1),
    PuntoOMSLongitud(74.5, 8.1, 8.6, 9.4, 10.3, 11.0, 7.4, 8.0, 8.7, 9.4, 10.2, 11.2, 12.2),
    PuntoOMSLongitud(75.0, 8.2, 8.7, 9.5, 10.4, 11.2, 7.5, 8.1, 8.8, 9.5, 10.3, 11.3, 12.3),
    PuntoOMSLongitud(75.5, 8.2, 8.8, 9.6, 10.5, 11.3, 7.6, 8.2, 8.8, 9.6, 10.4, 11.4, 12.5),
    PuntoOMSLongitud(76.0, 8.3, 8.9, 9.7, 10.6, 11.4, 7.6, 8.3, 8.9, 9.7, 10.6, 11.5, 12.6),
    PuntoOMSLongitud(76.5, 8.4, 9.0, 9.8, 10.7, 11.5, 7.7, 8.3, 9.0, 9.8, 10.7, 11.6, 12.7),
    PuntoOMSLongitud(77.0, 8.5, 9.1, 9.9, 10.8, 11.6, 7.8, 8.4, 9.1, 9.9, 10.8, 11.7, 12.8),
    PuntoOMSLongitud(77.5, 8.6, 9.2, 10.0, 10.9, 11.7, 7.9, 8.5, 9.2, 10.0, 10.9, 11.9, 13.0),
    PuntoOMSLongitud(78.0, 8.7, 9.3, 10.1, 11.0, 11.8, 7.9, 8.6, 9.3, 10.1, 11.0, 12.0, 13.1),
    PuntoOMSLongitud(78.5, 8.7, 9.3, 10.2, 11.1, 12.0, 8.0, 8.7, 9.4, 10.2, 11.1, 12.1, 13.2),
    PuntoOMSLongitud(79.0, 8.8, 9.4, 10.3, 11.2, 12.1, 8.1, 8.7, 9.5, 10.3, 11.2, 12.2, 13.3),
    PuntoOMSLongitud(79.5, 8.9, 9.5, 10.4, 11.3, 12.2, 8.2, 8.8, 9.5, 10.4, 11.3, 12.3, 13.4),
    PuntoOMSLongitud(80.0, 9.0, 9.6, 10.4, 11.4, 12.3, 8.2, 8.9, 9.6, 10.4, 11.4, 12.4, 13.6),
    PuntoOMSLongitud(80.5, 9.1, 9.7, 10.5, 11.5, 12.4, 8.3, 9.0, 9.7, 10.5, 11.5, 12.5, 13.7),
    PuntoOMSLongitud(81.0, 9.1, 9.8, 10.6, 11.6, 12.5, 8.4, 9.1, 9.8, 10.6, 11.6, 12.6, 13.8),
    PuntoOMSLongitud(81.5, 9.2, 9.9, 10.7, 11.7, 12.6, 8.5, 9.1, 9.9, 10.7, 11.7, 12.7, 13.9),
    PuntoOMSLongitud(82.0, 9.3, 10.0, 10.8, 11.8, 12.7, 8.5, 9.2, 10.0, 10.8, 11.8, 12.8, 14.0),
    PuntoOMSLongitud(82.5, 9.4, 10.1, 10.9, 11.9, 12.8, 8.6, 9.3, 10.1, 10.9, 11.9, 13.0, 14.2),
    PuntoOMSLongitud(83.0, 9.5, 10.1, 11.0, 12.0, 13.0, 8.7, 9.4, 10.2, 11.0, 12.0, 13.1, 14.3),
    PuntoOMSLongitud(83.5, 9.6, 10.3, 11.2, 12.2, 13.1, 8.8, 9.5, 10.3, 11.2, 12.1, 13.2, 14.4),
    PuntoOMSLongitud(84.0, 9.7, 10.4, 11.3, 12.3, 13.2, 8.9, 9.6, 10.4, 11.3, 12.2, 13.3, 14.6),
    PuntoOMSLongitud(84.5, 9.8, 10.5, 11.4, 12.4, 13.3, 9.0, 9.7, 10.5, 11.4, 12.4, 13.5, 14.7),
    PuntoOMSLongitud(85.0, 9.9, 10.6, 11.5, 12.5, 13.5, 9.1, 9.8, 10.6, 11.5, 12.5, 13.6, 14.9),
    PuntoOMSLongitud(85.5, 10.0, 10.7, 11.6, 12.7, 13.6, 9.2, 9.9, 10.7, 11.6, 12.6, 13.7, 15.0),
    PuntoOMSLongitud(86.0, 10.1, 10.8, 11.7, 12.8, 13.7, 9.3, 10.0, 10.8, 11.7, 12.8, 13.9, 15.2),
    PuntoOMSLongitud(86.5, 10.2, 10.9, 11.9, 12.9, 13.9, 9.4, 10.1, 11.0, 11.9, 12.9, 14.0, 15.3),
    PuntoOMSLongitud(87.0, 10.3, 11.0, 12.0, 13.1, 14.0, 9.5, 10.2, 11.1, 12.0, 13.0, 14.2, 15.5),
    PuntoOMSLongitud(87.5, 10.4, 11.2, 12.1, 13.2, 14.2, 9.6, 10.4, 11.2, 12.1, 13.2, 14.3, 15.6),
    PuntoOMSLongitud(88.0, 10.6, 11.3, 12.2, 13.3, 14.3, 9.7, 10.5, 11.3, 12.2, 13.3, 14.5, 15.8),
    PuntoOMSLongitud(88.5, 10.7, 11.4, 12.4, 13.5, 14.4, 9.8, 10.6, 11.4, 12.4, 13.4, 14.6, 15.9),
    PuntoOMSLongitud(89.0, 10.8, 11.5, 12.5, 13.6, 14.6, 9.9, 10.7, 11.5, 12.5, 13.5, 14.7, 16.1),
    PuntoOMSLongitud(89.5, 10.9, 11.6, 12.6, 13.7, 14.7, 10.0, 10.8, 11.6, 12.6, 13.7, 14.9, 16.2),
    PuntoOMSLongitud(90.0, 11.0, 11.7, 12.7, 13.8, 14.9, 10.1, 10.9, 11.8, 12.7, 13.8, 15.0, 16.4),
    PuntoOMSLongitud(90.5, 11.1, 11.8, 12.8, 14.0, 15.0, 10.2, 11.0, 11.9, 12.8, 13.9, 15.1, 16.5),
    PuntoOMSLongitud(91.0, 11.2, 11.9, 13.0, 14.1, 15.1, 10.3, 11.1, 12.0, 13.0, 14.1, 15.3, 16.7),
    PuntoOMSLongitud(91.5, 11.3, 12.0, 13.1, 14.2, 15.3, 10.4, 11.2, 12.1, 13.1, 14.2, 15.4, 16.8),
    PuntoOMSLongitud(92.0, 11.4, 12.2, 13.2, 14.4, 15.4, 10.5, 11.3, 12.2, 13.2, 14.3, 15.6, 17.0),
    PuntoOMSLongitud(92.5, 11.5, 12.3, 13.3, 14.5, 15.5, 10.6, 11.4, 12.3, 13.3, 14.4, 15.7, 17.1),
    PuntoOMSLongitud(93.0, 11.6, 12.4, 13.4, 14.6, 15.7, 10.7, 11.5, 12.4, 13.4, 14.6, 15.8, 17.3),
    PuntoOMSLongitud(93.5, 11.7, 12.5, 13.5, 14.7, 15.8, 10.7, 11.6, 12.5, 13.5, 14.7, 16.0, 17.4),
    PuntoOMSLongitud(94.0, 11.8, 12.6, 13.7, 14.9, 16.0, 10.8, 11.7, 12.6, 13.7, 14.8, 16.1, 17.6),
    PuntoOMSLongitud(94.5, 11.9, 12.7, 13.8, 15.0, 16.1, 10.9, 11.8, 12.7, 13.8, 14.9, 16.3, 17.7),
    PuntoOMSLongitud(95.0, 12.0, 12.8, 13.9, 15.1, 16.2, 11.0, 11.9, 12.8, 13.9, 15.1, 16.4, 17.9),
    PuntoOMSLongitud(95.5, 12.1, 12.9, 14.0, 15.3, 16.4, 11.1, 12.0, 12.9, 14.0, 15.2, 16.5, 18.0),
    PuntoOMSLongitud(96.0, 12.2, 13.0, 14.1, 15.4, 16.5, 11.2, 12.1, 13.1, 14.1, 15.3, 16.7, 18.2),
    PuntoOMSLongitud(96.5, 12.3, 13.1, 14.3, 15.5, 16.7, 11.3, 12.2, 13.2, 14.3, 15.5, 16.8, 18.4),
    PuntoOMSLongitud(97.0, 12.4, 13.2, 14.4, 15.7, 16.8, 11.4, 12.3, 13.3, 14.4, 15.6, 17.0, 18.5),
    PuntoOMSLongitud(97.5, 12.5, 13.4, 14.5, 15.8, 17.0, 11.5, 12.4, 13.4, 14.5, 15.7, 17.1, 18.7),
    PuntoOMSLongitud(98.0, 12.6, 13.5, 14.6, 15.9, 17.1, 11.6, 12.5, 13.5, 14.6, 15.9, 17.3, 18.9),
    PuntoOMSLongitud(98.5, 12.7, 13.6, 14.8, 16.1, 17.3, 11.7, 12.6, 13.6, 14.8, 16.0, 17.5, 19.1),
    PuntoOMSLongitud(99.0, 12.8, 13.7, 14.9, 16.2, 17.4, 11.8, 12.7, 13.7, 14.9, 16.2, 17.6, 19.2),
    PuntoOMSLongitud(99.5, 12.9, 13.8, 15.0, 16.4, 17.6, 11.9, 12.8, 13.9, 15.0, 16.3, 17.8, 19.4),
    PuntoOMSLongitud(100.0, 13.0, 13.9, 15.2, 16.5, 17.8, 12.0, 12.9, 14.0, 15.2, 16.5, 18.0, 19.6),
    PuntoOMSLongitud(100.5, 13.2, 14.1, 15.3, 16.7, 17.9, 12.1, 13.0, 14.1, 15.3, 16.6, 18.1, 19.8),
    PuntoOMSLongitud(101.0, 13.3, 14.2, 15.4, 16.8, 18.1, 12.2, 13.2, 14.2, 15.4, 16.8, 18.3, 20.0),
    PuntoOMSLongitud(101.5, 13.4, 14.3, 15.6, 17.0, 18.3, 12.3, 13.3, 14.4, 15.6, 16.9, 18.5, 20.2),
    PuntoOMSLongitud(102.0, 13.5, 14.5, 15.7, 17.2, 18.5, 12.4, 13.4, 14.5, 15.7, 17.1, 18.7, 20.4),
    PuntoOMSLongitud(102.5, 13.6, 14.6, 15.9, 17.3, 18.6, 12.5, 13.5, 14.6, 15.9, 17.3, 18.8, 20.6),
    PuntoOMSLongitud(103.0, 13.8, 14.7, 16.0, 17.5, 18.8, 12.6, 13.6, 14.8, 16.0, 17.4, 19.0, 20.8),
    PuntoOMSLongitud(103.5, 13.9, 14.8, 16.2, 17.7, 19.0, 12.7, 13.7, 14.9, 16.2, 17.6, 19.2, 21.0),
    PuntoOMSLongitud(104.0, 14.0, 15.0, 16.3, 17.8, 19.2, 12.8, 13.9, 15.0, 16.3, 17.8, 19.4, 21.2),
    PuntoOMSLongitud(104.5, 14.1, 15.1, 16.5, 18.0, 19.4, 12.9, 14.0, 15.2, 16.5, 17.9, 19.6, 21.5),
    PuntoOMSLongitud(105.0, 14.2, 15.3, 16.6, 18.2, 19.6, 13.0, 14.1, 15.3, 16.6, 18.1, 19.8, 21.7),
    PuntoOMSLongitud(105.5, 14.4, 15.4, 16.8, 18.4, 19.8, 13.2, 14.2, 15.4, 16.8, 18.3, 20.0, 21.9),
    PuntoOMSLongitud(106.0, 14.5, 15.5, 16.9, 18.5, 20.0, 13.3, 14.4, 15.6, 16.9, 18.5, 20.2, 22.1),
    PuntoOMSLongitud(106.5, 14.6, 15.7, 17.1, 18.7, 20.2, 13.4, 14.5, 15.7, 17.1, 18.6, 20.4, 22.4),
    PuntoOMSLongitud(107.0, 14.8, 15.8, 17.3, 18.9, 20.4, 13.5, 14.6, 15.9, 17.3, 18.8, 20.6, 22.6),
    PuntoOMSLongitud(107.5, 14.9, 16.0, 17.4, 19.1, 20.6, 13.6, 14.7, 16.0, 17.4, 19.0, 20.8, 22.8),
    PuntoOMSLongitud(108.0, 15.0, 16.1, 17.6, 19.3, 20.8, 13.7, 14.9, 16.2, 17.6, 19.2, 21.0, 23.1),
    PuntoOMSLongitud(108.5, 15.2, 16.3, 17.8, 19.5, 21.0, 13.8, 15.0, 16.3, 17.8, 19.4, 21.2, 23.3),
    PuntoOMSLongitud(109.0, 15.3, 16.4, 17.9, 19.6, 21.2, 14.0, 15.1, 16.5, 17.9, 19.6, 21.4, 23.6),
    PuntoOMSLongitud(109.5, 15.4, 16.6, 18.1, 19.8, 21.4, 14.1, 15.3, 16.6, 18.1, 19.8, 21.7, 23.8),
    PuntoOMSLongitud(110.0, 15.6, 16.7, 18.3, 20.0, 21.6, 14.2, 15.4, 16.8, 18.3, 20.0, 21.9, 24.1)
)

// ═══════════════════════════════════════════════════════════════════════════
// FUNCIONES DE SELECCIÓN POR SEXO
// ═══════════════════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════════════════
// TABLAS OFICIALES LMS Y Z-SCORES — WHO Growth Reference 2007 (5–19 años / 61–144 meses)
// https://www.who.int/tools/growth-reference-data-for-5to19-years/indicators/bmi-for-age
// ═══════════════════════════════════════════════════════════════════════════

val OMS_IMC_2007_NINAS: List<PuntoOMSLms> = listOf(
    PuntoOMSLms(60, -0.8886, 15.2441, 0.0969, 11.77, 12.75, 13.89, 15.24, 16.87, 18.86, 21.34),
    PuntoOMSLms(61, -0.8886, 15.2441, 0.0969, 11.77, 12.75, 13.89, 15.24, 16.87, 18.86, 21.34),
    PuntoOMSLms(62, -0.9068, 15.2434, 0.0974, 11.76, 12.74, 13.88, 15.24, 16.88, 18.89, 21.40),
    PuntoOMSLms(63, -0.9248, 15.2433, 0.0978, 11.76, 12.73, 13.88, 15.24, 16.89, 18.91, 21.47),
    PuntoOMSLms(64, -0.9427, 15.2438, 0.0983, 11.75, 12.73, 13.88, 15.24, 16.90, 18.95, 21.54),
    PuntoOMSLms(65, -0.9605, 15.2448, 0.0988, 11.75, 12.72, 13.87, 15.24, 16.91, 18.98, 21.60),
    PuntoOMSLms(66, -0.9780, 15.2464, 0.0992, 11.74, 12.72, 13.87, 15.25, 16.92, 19.01, 21.67),
    PuntoOMSLms(67, -0.9954, 15.2487, 0.0997, 11.74, 12.71, 13.87, 15.25, 16.94, 19.04, 21.75),
    PuntoOMSLms(68, -1.0126, 15.2516, 0.1001, 11.73, 12.71, 13.86, 15.25, 16.95, 19.08, 21.82),
    PuntoOMSLms(69, -1.0296, 15.2551, 0.1006, 11.73, 12.71, 13.86, 15.26, 16.96, 19.11, 21.89),
    PuntoOMSLms(70, -1.0464, 15.2592, 0.1010, 11.73, 12.70, 13.86, 15.26, 16.98, 19.15, 21.97),
    PuntoOMSLms(71, -1.0630, 15.2641, 0.1015, 11.72, 12.70, 13.86, 15.26, 17.00, 19.18, 22.05),
    PuntoOMSLms(72, -1.0794, 15.2697, 0.1019, 11.72, 12.70, 13.86, 15.27, 17.01, 19.22, 22.13),
    PuntoOMSLms(73, -1.0956, 15.2760, 0.1024, 11.72, 12.70, 13.86, 15.28, 17.03, 19.26, 22.22),
    PuntoOMSLms(74, -1.1115, 15.2831, 0.1029, 11.72, 12.70, 13.87, 15.28, 17.05, 19.30, 22.30),
    PuntoOMSLms(75, -1.1272, 15.2911, 0.1033, 11.72, 12.70, 13.87, 15.29, 17.07, 19.35, 22.39),
    PuntoOMSLms(76, -1.1427, 15.2998, 0.1038, 11.72, 12.70, 13.87, 15.30, 17.09, 19.39, 22.48),
    PuntoOMSLms(77, -1.1579, 15.3095, 0.1042, 11.72, 12.70, 13.87, 15.31, 17.11, 19.44, 22.57),
    PuntoOMSLms(78, -1.1728, 15.3200, 0.1047, 11.72, 12.70, 13.88, 15.32, 17.13, 19.48, 22.67),
    PuntoOMSLms(79, -1.1875, 15.3314, 0.1052, 11.73, 12.71, 13.88, 15.33, 17.15, 19.53, 22.77),
    PuntoOMSLms(80, -1.2019, 15.3439, 0.1056, 11.73, 12.71, 13.89, 15.34, 17.18, 19.58, 22.86),
    PuntoOMSLms(81, -1.2160, 15.3572, 0.1061, 11.73, 12.72, 13.90, 15.36, 17.20, 19.63, 22.97),
    PuntoOMSLms(82, -1.2298, 15.3717, 0.1065, 11.74, 12.72, 13.91, 15.37, 17.23, 19.68, 23.07),
    PuntoOMSLms(83, -1.2433, 15.3871, 0.1070, 11.74, 12.73, 13.92, 15.39, 17.26, 19.73, 23.18),
    PuntoOMSLms(84, -1.2565, 15.4036, 0.1075, 11.75, 12.73, 13.93, 15.40, 17.29, 19.79, 23.29),
    PuntoOMSLms(85, -1.2693, 15.4211, 0.1079, 11.76, 12.74, 13.94, 15.42, 17.32, 19.84, 23.40),
    PuntoOMSLms(86, -1.2819, 15.4397, 0.1084, 11.77, 12.75, 13.95, 15.44, 17.35, 19.90, 23.51),
    PuntoOMSLms(87, -1.2941, 15.4593, 0.1088, 11.77, 12.76, 13.96, 15.46, 17.38, 19.96, 23.63),
    PuntoOMSLms(88, -1.3060, 15.4798, 0.1093, 11.78, 12.77, 13.98, 15.48, 17.42, 20.02, 23.75),
    PuntoOMSLms(89, -1.3175, 15.5014, 0.1097, 11.79, 12.78, 13.99, 15.50, 17.45, 20.09, 23.87),
    PuntoOMSLms(90, -1.3287, 15.5240, 0.1102, 11.80, 12.79, 14.01, 15.52, 17.49, 20.15, 23.99),
    PuntoOMSLms(91, -1.3395, 15.5476, 0.1106, 11.81, 12.81, 14.02, 15.55, 17.53, 20.21, 24.12),
    PuntoOMSLms(92, -1.3499, 15.5723, 0.1111, 11.83, 12.82, 14.04, 15.57, 17.56, 20.28, 24.25),
    PuntoOMSLms(93, -1.3600, 15.5979, 0.1116, 11.84, 12.84, 14.06, 15.60, 17.60, 20.35, 24.38),
    PuntoOMSLms(94, -1.3697, 15.6246, 0.1120, 11.85, 12.85, 14.08, 15.62, 17.64, 20.42, 24.51),
    PuntoOMSLms(95, -1.3790, 15.6523, 0.1125, 11.87, 12.87, 14.10, 15.65, 17.69, 20.49, 24.64),
    PuntoOMSLms(96, -1.3880, 15.6810, 0.1129, 11.88, 12.88, 14.12, 15.68, 17.73, 20.56, 24.78),
    PuntoOMSLms(97, -1.3966, 15.7107, 0.1134, 11.89, 12.90, 14.14, 15.71, 17.77, 20.63, 24.92),
    PuntoOMSLms(98, -1.4047, 15.7415, 0.1138, 11.91, 12.92, 14.16, 15.74, 17.82, 20.71, 25.06),
    PuntoOMSLms(99, -1.4125, 15.7732, 0.1142, 11.93, 12.94, 14.19, 15.77, 17.87, 20.78, 25.20),
    PuntoOMSLms(100, -1.4199, 15.8058, 0.1147, 11.94, 12.96, 14.21, 15.81, 17.91, 20.86, 25.34),
    PuntoOMSLms(101, -1.4270, 15.8394, 0.1151, 11.96, 12.98, 14.24, 15.84, 17.96, 20.94, 25.49),
    PuntoOMSLms(102, -1.4336, 15.8738, 0.1156, 11.98, 13.00, 14.26, 15.87, 18.01, 21.02, 25.64),
    PuntoOMSLms(103, -1.4398, 15.9090, 0.1160, 12.00, 13.02, 14.29, 15.91, 18.06, 21.10, 25.79),
    PuntoOMSLms(104, -1.4456, 15.9451, 0.1164, 12.02, 13.04, 14.32, 15.95, 18.11, 21.18, 25.93),
    PuntoOMSLms(105, -1.4511, 15.9818, 0.1169, 12.04, 13.07, 14.35, 15.98, 18.17, 21.26, 26.09),
    PuntoOMSLms(106, -1.4561, 16.0194, 0.1173, 12.06, 13.09, 14.38, 16.02, 18.22, 21.35, 26.24),
    PuntoOMSLms(107, -1.4607, 16.0575, 0.1177, 12.08, 13.12, 14.40, 16.06, 18.27, 21.43, 26.39),
    PuntoOMSLms(108, -1.4650, 16.0964, 0.1182, 12.10, 13.14, 14.43, 16.10, 18.33, 21.51, 26.54),
    PuntoOMSLms(109, -1.4688, 16.1358, 0.1186, 12.12, 13.16, 14.46, 16.14, 18.38, 21.60, 26.69),
    PuntoOMSLms(110, -1.4723, 16.1759, 0.1190, 12.14, 13.19, 14.50, 16.18, 18.44, 21.68, 26.84),
    PuntoOMSLms(111, -1.4753, 16.2166, 0.1194, 12.16, 13.22, 14.53, 16.22, 18.49, 21.77, 27.00),
    PuntoOMSLms(112, -1.4780, 16.2580, 0.1198, 12.19, 13.24, 14.56, 16.26, 18.55, 21.86, 27.15),
    PuntoOMSLms(113, -1.4803, 16.2999, 0.1203, 12.21, 13.27, 14.59, 16.30, 18.61, 21.94, 27.30),
    PuntoOMSLms(114, -1.4823, 16.3425, 0.1207, 12.23, 13.30, 14.62, 16.34, 18.67, 22.03, 27.46),
    PuntoOMSLms(115, -1.4838, 16.3858, 0.1211, 12.25, 13.32, 14.66, 16.39, 18.73, 22.12, 27.61),
    PuntoOMSLms(116, -1.4850, 16.4298, 0.1215, 12.28, 13.35, 14.69, 16.43, 18.79, 22.21, 27.77),
    PuntoOMSLms(117, -1.4859, 16.4746, 0.1219, 12.30, 13.38, 14.73, 16.48, 18.85, 22.30, 27.92),
    PuntoOMSLms(118, -1.4864, 16.5200, 0.1223, 12.33, 13.41, 14.76, 16.52, 18.91, 22.39, 28.07),
    PuntoOMSLms(119, -1.4866, 16.5663, 0.1227, 12.35, 13.44, 14.80, 16.57, 18.97, 22.48, 28.23),
    PuntoOMSLms(120, -1.4864, 16.6133, 0.1231, 12.38, 13.47, 14.84, 16.61, 19.03, 22.57, 28.38),
    PuntoOMSLms(121, -1.4859, 16.6612, 0.1235, 12.40, 13.50, 14.88, 16.66, 19.10, 22.66, 28.53),
    PuntoOMSLms(122, -1.4851, 16.7100, 0.1238, 12.43, 13.53, 14.91, 16.71, 19.16, 22.75, 28.68),
    PuntoOMSLms(123, -1.4839, 16.7595, 0.1242, 12.46, 13.56, 14.95, 16.76, 19.23, 22.85, 28.83),
    PuntoOMSLms(124, -1.4825, 16.8100, 0.1246, 12.48, 13.60, 14.99, 16.81, 19.29, 22.94, 28.99),
    PuntoOMSLms(125, -1.4807, 16.8614, 0.1250, 12.51, 13.63, 15.04, 16.86, 19.36, 23.04, 29.14),
    PuntoOMSLms(126, -1.4787, 16.9136, 0.1253, 12.54, 13.67, 15.08, 16.91, 19.43, 23.13, 29.29),
    PuntoOMSLms(127, -1.4763, 16.9667, 0.1257, 12.57, 13.70, 15.12, 16.97, 19.50, 23.23, 29.44),
    PuntoOMSLms(128, -1.4737, 17.0208, 0.1261, 12.60, 13.74, 15.16, 17.02, 19.57, 23.33, 29.59),
    PuntoOMSLms(129, -1.4708, 17.0757, 0.1264, 12.63, 13.77, 15.21, 17.08, 19.64, 23.43, 29.74),
    PuntoOMSLms(130, -1.4677, 17.1316, 0.1268, 12.66, 13.81, 15.25, 17.13, 19.71, 23.52, 29.89),
    PuntoOMSLms(131, -1.4642, 17.1883, 0.1271, 12.70, 13.85, 15.30, 17.19, 19.79, 23.62, 30.04),
    PuntoOMSLms(132, -1.4606, 17.2459, 0.1275, 12.73, 13.88, 15.34, 17.25, 19.86, 23.73, 30.19),
    PuntoOMSLms(133, -1.4567, 17.3044, 0.1278, 12.76, 13.93, 15.39, 17.30, 19.93, 23.82, 30.34),
    PuntoOMSLms(134, -1.4526, 17.3637, 0.1282, 12.79, 13.96, 15.44, 17.36, 20.01, 23.93, 30.48),
    PuntoOMSLms(135, -1.4482, 17.4238, 0.1285, 12.83, 14.00, 15.49, 17.42, 20.09, 24.03, 30.63),
    PuntoOMSLms(136, -1.4436, 17.4847, 0.1288, 12.86, 14.04, 15.54, 17.48, 20.16, 24.13, 30.78),
    PuntoOMSLms(137, -1.4389, 17.5464, 0.1291, 12.90, 14.09, 15.59, 17.55, 20.24, 24.23, 30.92),
    PuntoOMSLms(138, -1.4339, 17.6088, 0.1295, 12.93, 14.13, 15.64, 17.61, 20.32, 24.34, 31.06),
    PuntoOMSLms(139, -1.4288, 17.6719, 0.1298, 12.97, 14.17, 15.69, 17.67, 20.40, 24.44, 31.21),
    PuntoOMSLms(140, -1.4235, 17.7357, 0.1301, 13.00, 14.21, 15.74, 17.74, 20.48, 24.55, 31.35),
    PuntoOMSLms(141, -1.4180, 17.8001, 0.1304, 13.04, 14.26, 15.79, 17.80, 20.56, 24.65, 31.49),
    PuntoOMSLms(142, -1.4123, 17.8651, 0.1307, 13.08, 14.30, 15.85, 17.86, 20.64, 24.76, 31.63),
    PuntoOMSLms(143, -1.4065, 17.9306, 0.1310, 13.11, 14.35, 15.90, 17.93, 20.72, 24.86, 31.77),
    PuntoOMSLms(144, -1.4006, 17.9966, 0.1313, 13.15, 14.39, 15.95, 18.00, 20.81, 24.97, 31.91)
)

val OMS_IMC_2007_NINOS: List<PuntoOMSLms> = listOf(
    PuntoOMSLms(60, -0.7387, 15.2641, 0.0839, 12.12, 13.03, 14.07, 15.26, 16.64, 18.26, 20.17),
    PuntoOMSLms(61, -0.7387, 15.2641, 0.0839, 12.12, 13.03, 14.07, 15.26, 16.64, 18.26, 20.17),
    PuntoOMSLms(62, -0.7621, 15.2616, 0.0841, 12.12, 13.03, 14.07, 15.26, 16.65, 18.27, 20.20),
    PuntoOMSLms(63, -0.7856, 15.2604, 0.0844, 12.11, 13.02, 14.06, 15.26, 16.65, 18.29, 20.24),
    PuntoOMSLms(64, -0.8089, 15.2605, 0.0846, 12.11, 13.02, 14.06, 15.26, 16.66, 18.31, 20.28),
    PuntoOMSLms(65, -0.8322, 15.2619, 0.0849, 12.11, 13.02, 14.06, 15.26, 16.67, 18.33, 20.32),
    PuntoOMSLms(66, -0.8554, 15.2645, 0.0852, 12.12, 13.02, 14.06, 15.26, 16.68, 18.35, 20.36),
    PuntoOMSLms(67, -0.8785, 15.2684, 0.0854, 12.12, 13.02, 14.06, 15.27, 16.69, 18.37, 20.41),
    PuntoOMSLms(68, -0.9015, 15.2737, 0.0857, 12.12, 13.02, 14.06, 15.27, 16.70, 18.40, 20.46),
    PuntoOMSLms(69, -0.9243, 15.2801, 0.0860, 12.12, 13.03, 14.07, 15.28, 16.71, 18.43, 20.52),
    PuntoOMSLms(70, -0.9471, 15.2877, 0.0862, 12.13, 13.03, 14.07, 15.29, 16.73, 18.46, 20.57),
    PuntoOMSLms(71, -0.9697, 15.2965, 0.0865, 12.13, 13.04, 14.08, 15.30, 16.74, 18.49, 20.63),
    PuntoOMSLms(72, -0.9921, 15.3062, 0.0868, 12.14, 13.04, 14.08, 15.31, 16.76, 18.52, 20.69),
    PuntoOMSLms(73, -1.0144, 15.3169, 0.0871, 12.15, 13.05, 14.09, 15.32, 16.78, 18.55, 20.75),
    PuntoOMSLms(74, -1.0365, 15.3285, 0.0874, 12.15, 13.05, 14.10, 15.33, 16.80, 18.59, 20.82),
    PuntoOMSLms(75, -1.0584, 15.3408, 0.0877, 12.16, 13.06, 14.11, 15.34, 16.82, 18.63, 20.88),
    PuntoOMSLms(76, -1.0801, 15.3540, 0.0880, 12.17, 13.07, 14.12, 15.35, 16.84, 18.66, 20.95),
    PuntoOMSLms(77, -1.1017, 15.3679, 0.0883, 12.18, 13.08, 14.13, 15.37, 16.86, 18.70, 21.02),
    PuntoOMSLms(78, -1.1230, 15.3825, 0.0887, 12.19, 13.09, 14.14, 15.38, 16.89, 18.75, 21.10),
    PuntoOMSLms(79, -1.1441, 15.3978, 0.0890, 12.20, 13.10, 14.15, 15.40, 16.91, 18.79, 21.17),
    PuntoOMSLms(80, -1.1649, 15.4137, 0.0893, 12.21, 13.11, 14.16, 15.41, 16.94, 18.83, 21.25),
    PuntoOMSLms(81, -1.1856, 15.4302, 0.0896, 12.22, 13.12, 14.17, 15.43, 16.96, 18.88, 21.33),
    PuntoOMSLms(82, -1.2060, 15.4473, 0.0900, 12.23, 13.13, 14.18, 15.45, 16.99, 18.92, 21.41),
    PuntoOMSLms(83, -1.2261, 15.4650, 0.0903, 12.24, 13.14, 14.20, 15.46, 17.02, 18.97, 21.50),
    PuntoOMSLms(84, -1.2460, 15.4832, 0.0907, 12.25, 13.15, 14.21, 15.48, 17.05, 19.02, 21.58),
    PuntoOMSLms(85, -1.2656, 15.5019, 0.0910, 12.26, 13.16, 14.22, 15.50, 17.08, 19.07, 21.67),
    PuntoOMSLms(86, -1.2849, 15.5210, 0.0914, 12.27, 13.17, 14.24, 15.52, 17.11, 19.12, 21.76),
    PuntoOMSLms(87, -1.3040, 15.5407, 0.0918, 12.28, 13.18, 14.25, 15.54, 17.14, 19.17, 21.86),
    PuntoOMSLms(88, -1.3228, 15.5608, 0.0921, 12.29, 13.20, 14.27, 15.56, 17.17, 19.22, 21.95),
    PuntoOMSLms(89, -1.3414, 15.5814, 0.0925, 12.31, 13.21, 14.28, 15.58, 17.20, 19.27, 22.05),
    PuntoOMSLms(90, -1.3596, 15.6023, 0.0929, 12.32, 13.22, 14.29, 15.60, 17.23, 19.33, 22.15),
    PuntoOMSLms(91, -1.3776, 15.6237, 0.0933, 12.33, 13.23, 14.31, 15.62, 17.26, 19.38, 22.25),
    PuntoOMSLms(92, -1.3953, 15.6455, 0.0937, 12.34, 13.25, 14.33, 15.65, 17.30, 19.44, 22.35),
    PuntoOMSLms(93, -1.4126, 15.6677, 0.0941, 12.36, 13.26, 14.34, 15.67, 17.33, 19.50, 22.46),
    PuntoOMSLms(94, -1.4297, 15.6903, 0.0945, 12.37, 13.27, 14.36, 15.69, 17.37, 19.55, 22.56),
    PuntoOMSLms(95, -1.4464, 15.7133, 0.0949, 12.38, 13.29, 14.38, 15.71, 17.40, 19.61, 22.67),
    PuntoOMSLms(96, -1.4629, 15.7368, 0.0953, 12.39, 13.30, 14.39, 15.74, 17.44, 19.68, 22.79),
    PuntoOMSLms(97, -1.4790, 15.7606, 0.0957, 12.41, 13.32, 14.41, 15.76, 17.47, 19.74, 22.90),
    PuntoOMSLms(98, -1.4947, 15.7848, 0.0961, 12.42, 13.33, 14.43, 15.79, 17.51, 19.80, 23.02),
    PuntoOMSLms(99, -1.5101, 15.8094, 0.0965, 12.43, 13.35, 14.45, 15.81, 17.55, 19.86, 23.13),
    PuntoOMSLms(100, -1.5252, 15.8344, 0.0969, 12.45, 13.36, 14.47, 15.83, 17.59, 19.93, 23.25),
    PuntoOMSLms(101, -1.5399, 15.8597, 0.0974, 12.46, 13.38, 14.48, 15.86, 17.62, 19.99, 23.38),
    PuntoOMSLms(102, -1.5542, 15.8855, 0.0978, 12.47, 13.39, 14.50, 15.89, 17.66, 20.06, 23.50),
    PuntoOMSLms(103, -1.5681, 15.9116, 0.0982, 12.49, 13.41, 14.52, 15.91, 17.70, 20.12, 23.63),
    PuntoOMSLms(104, -1.5817, 15.9381, 0.0986, 12.50, 13.42, 14.54, 15.94, 17.74, 20.19, 23.75),
    PuntoOMSLms(105, -1.5948, 15.9651, 0.0991, 12.52, 13.44, 14.56, 15.96, 17.78, 20.26, 23.89),
    PuntoOMSLms(106, -1.6076, 15.9925, 0.0995, 12.53, 13.46, 14.58, 15.99, 17.82, 20.33, 24.02),
    PuntoOMSLms(107, -1.6199, 16.0205, 0.0999, 12.55, 13.47, 14.60, 16.02, 17.87, 20.40, 24.15),
    PuntoOMSLms(108, -1.6318, 16.0490, 0.1004, 12.56, 13.49, 14.62, 16.05, 17.91, 20.47, 24.29),
    PuntoOMSLms(109, -1.6433, 16.0781, 0.1008, 12.58, 13.51, 14.65, 16.08, 17.95, 20.54, 24.43),
    PuntoOMSLms(110, -1.6544, 16.1078, 0.1013, 12.59, 13.53, 14.67, 16.11, 18.00, 20.61, 24.57),
    PuntoOMSLms(111, -1.6651, 16.1381, 0.1017, 12.61, 13.54, 14.69, 16.14, 18.04, 20.69, 24.71),
    PuntoOMSLms(112, -1.6753, 16.1692, 0.1021, 12.63, 13.56, 14.71, 16.17, 18.09, 20.76, 24.85),
    PuntoOMSLms(113, -1.6851, 16.2009, 0.1026, 12.64, 13.58, 14.74, 16.20, 18.13, 20.84, 25.00),
    PuntoOMSLms(114, -1.6944, 16.2333, 0.1030, 12.66, 13.60, 14.76, 16.23, 18.18, 20.92, 25.15),
    PuntoOMSLms(115, -1.7032, 16.2665, 0.1035, 12.68, 13.62, 14.79, 16.27, 18.23, 20.99, 25.30),
    PuntoOMSLms(116, -1.7116, 16.3004, 0.1039, 12.70, 13.64, 14.81, 16.30, 18.28, 21.07, 25.45),
    PuntoOMSLms(117, -1.7196, 16.3351, 0.1043, 12.72, 13.67, 14.84, 16.34, 18.33, 21.15, 25.61),
    PuntoOMSLms(118, -1.7271, 16.3704, 0.1048, 12.73, 13.69, 14.87, 16.37, 18.38, 21.23, 25.76),
    PuntoOMSLms(119, -1.7341, 16.4065, 0.1052, 12.76, 13.71, 14.89, 16.41, 18.43, 21.32, 25.91),
    PuntoOMSLms(120, -1.7407, 16.4433, 0.1057, 12.78, 13.73, 14.92, 16.44, 18.48, 21.40, 26.07),
    PuntoOMSLms(121, -1.7468, 16.4807, 0.1061, 12.80, 13.76, 14.95, 16.48, 18.53, 21.48, 26.23),
    PuntoOMSLms(122, -1.7525, 16.5189, 0.1065, 12.82, 13.78, 14.98, 16.52, 18.59, 21.57, 26.39),
    PuntoOMSLms(123, -1.7578, 16.5578, 0.1070, 12.84, 13.81, 15.01, 16.56, 18.64, 21.65, 26.55),
    PuntoOMSLms(124, -1.7626, 16.5974, 0.1074, 12.86, 13.83, 15.04, 16.60, 18.70, 21.74, 26.71),
    PuntoOMSLms(125, -1.7670, 16.6376, 0.1078, 12.88, 13.86, 15.07, 16.64, 18.75, 21.83, 26.88),
    PuntoOMSLms(126, -1.7710, 16.6786, 0.1082, 12.90, 13.89, 15.11, 16.68, 18.81, 21.91, 27.04),
    PuntoOMSLms(127, -1.7745, 16.7203, 0.1086, 12.93, 13.91, 15.14, 16.72, 18.86, 22.00, 27.20),
    PuntoOMSLms(128, -1.7777, 16.7628, 0.1091, 12.95, 13.94, 15.17, 16.76, 18.92, 22.09, 27.37),
    PuntoOMSLms(129, -1.7804, 16.8059, 0.1095, 12.98, 13.97, 15.21, 16.81, 18.98, 22.18, 27.53),
    PuntoOMSLms(130, -1.7828, 16.8497, 0.1099, 13.00, 14.00, 15.24, 16.85, 19.04, 22.27, 27.70),
    PuntoOMSLms(131, -1.7847, 16.8941, 0.1103, 13.03, 14.03, 15.28, 16.89, 19.10, 22.36, 27.86),
    PuntoOMSLms(132, -1.7862, 16.9392, 0.1107, 13.05, 14.06, 15.31, 16.94, 19.16, 22.45, 28.03),
    PuntoOMSLms(133, -1.7873, 16.9850, 0.1111, 13.08, 14.09, 15.35, 16.98, 19.22, 22.54, 28.19),
    PuntoOMSLms(134, -1.7881, 17.0314, 0.1115, 13.10, 14.12, 15.38, 17.03, 19.29, 22.64, 28.36),
    PuntoOMSLms(135, -1.7884, 17.0784, 0.1119, 13.13, 14.15, 15.42, 17.08, 19.35, 22.73, 28.52),
    PuntoOMSLms(136, -1.7884, 17.1262, 0.1123, 13.16, 14.18, 15.46, 17.13, 19.41, 22.82, 28.68),
    PuntoOMSLms(137, -1.7880, 17.1746, 0.1127, 13.19, 14.21, 15.50, 17.18, 19.48, 22.91, 28.85),
    PuntoOMSLms(138, -1.7873, 17.2236, 0.1130, 13.21, 14.24, 15.54, 17.22, 19.54, 23.01, 29.01),
    PuntoOMSLms(139, -1.7861, 17.2734, 0.1134, 13.24, 14.28, 15.58, 17.27, 19.61, 23.10, 29.17),
    PuntoOMSLms(140, -1.7846, 17.3240, 0.1138, 13.27, 14.31, 15.62, 17.32, 19.67, 23.20, 29.33),
    PuntoOMSLms(141, -1.7828, 17.3752, 0.1142, 13.30, 14.35, 15.66, 17.38, 19.74, 23.29, 29.49),
    PuntoOMSLms(142, -1.7806, 17.4272, 0.1145, 13.33, 14.38, 15.70, 17.43, 19.81, 23.39, 29.64),
    PuntoOMSLms(143, -1.7780, 17.4799, 0.1149, 13.36, 14.42, 15.74, 17.48, 19.88, 23.48, 29.80),
    PuntoOMSLms(144, -1.7751, 17.5334, 0.1152, 13.39, 14.45, 15.79, 17.53, 19.95, 23.58, 29.96)
)

fun calcularZScoreLms(imc: Double, l: Double, m: Double, s: Double): Double {
    if (imc <= 0.0 || m <= 0.0 || s <= 0.0) return 0.0
    return if (abs(l) > 1e-6) {
        ((imc / m).pow(l) - 1.0) / (l * s)
    } else {
        ln(imc / m) / s
    }
}

fun interpolarPuntoOMSLms(tabla: List<PuntoOMSLms>, meses: Int): PuntoOMSLms {
    if (meses <= tabla.first().meses) return tabla.first()
    if (meses >= tabla.last().meses)  return tabla.last()

    val idx = tabla.indexOfFirst { it.meses >= meses }
    val b   = tabla[idx]
    val a   = tabla[idx - 1]
    val t   = (meses - a.meses).toDouble() / (b.meses - a.meses)

    fun lerp(va: Double, vb: Double) = va + (vb - va) * t
    return PuntoOMSLms(
        meses  = meses,
        l      = lerp(a.l, b.l),
        m      = lerp(a.m, b.m),
        s      = lerp(a.s, b.s),
        sd3neg = lerp(a.sd3neg, b.sd3neg),
        sd2neg = lerp(a.sd2neg, b.sd2neg),
        sd1neg = lerp(a.sd1neg, b.sd1neg),
        sd0    = lerp(a.sd0, b.sd0),
        sd1pos = lerp(a.sd1pos, b.sd1pos),
        sd2pos = lerp(a.sd2pos, b.sd2pos),
        sd3pos = lerp(a.sd3pos, b.sd3pos)
    )
}

fun omsPesoPorSexo(sexo: Sexo?)  = if (sexo == Sexo.NINA) OMS_PESO_NINAS  else OMS_PESO_NINOS
fun omsTallaPorSexo(sexo: Sexo?) = if (sexo == Sexo.NINA) OMS_TALLA_NINAS else OMS_TALLA_NINOS
fun omsWflPorSexo(sexo: Sexo?): List<PuntoOMSLongitud> = if (sexo == Sexo.NINA) OMS_WFL_NINAS else OMS_WFL_NINOS

// ═══════════════════════════════════════════════════════════════════════════
// INTERPOLACIÓN LINEAL ENTRE PUNTOS OMS
// Permite obtener el valor referencia para cualquier mes o longitud intermedia.
// ═══════════════════════════════════════════════════════════════════════════

fun interpolarPuntoOMS(tabla: List<PuntoOMS>, meses: Int): PuntoOMS {
    if (meses <= tabla.first().meses) return tabla.first()
    if (meses >= tabla.last().meses)  return tabla.last()

    val idx = tabla.indexOfFirst { it.meses >= meses }
    val b   = tabla[idx]
    val a   = tabla[idx - 1]
    val t   = (meses - a.meses).toDouble() / (b.meses - a.meses)

    fun lerp(va: Double, vb: Double) = va + (vb - va) * t
    return PuntoOMS(
        meses = meses,
        p3    = lerp(a.p3,  b.p3),
        p15   = lerp(a.p15, b.p15),
        p50   = lerp(a.p50, b.p50),
        p85   = lerp(a.p85, b.p85),
        p97   = lerp(a.p97, b.p97)
    )
}

fun interpolarPuntoOMSLongitud(tabla: List<PuntoOMSLongitud>, longitudCm: Double): PuntoOMSLongitud {
    if (longitudCm <= tabla.first().longitudCm) return tabla.first()
    if (longitudCm >= tabla.last().longitudCm)  return tabla.last()

    val idx = tabla.indexOfFirst { it.longitudCm >= longitudCm }
    val b   = tabla[idx]
    val a   = tabla[idx - 1]
    val t   = (longitudCm - a.longitudCm) / (b.longitudCm - a.longitudCm)

    fun lerp(va: Double, vb: Double) = va + (vb - va) * t
    return PuntoOMSLongitud(
        longitudCm = longitudCm,
        p3         = lerp(a.p3, b.p3),
        p15        = lerp(a.p15, b.p15),
        p50        = lerp(a.p50, b.p50),
        p85        = lerp(a.p85, b.p85),
        p97        = lerp(a.p97, b.p97),
        sd3neg     = lerp(a.sd3neg, b.sd3neg),
        sd2neg     = lerp(a.sd2neg, b.sd2neg),
        sd1neg     = lerp(a.sd1neg, b.sd1neg),
        sd0        = lerp(a.sd0, b.sd0),
        sd1pos     = lerp(a.sd1pos, b.sd1pos),
        sd2pos     = lerp(a.sd2pos, b.sd2pos),
        sd3pos     = lerp(a.sd3pos, b.sd3pos)
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// CLASIFICACIÓN NUTRICIONAL Y ANTROPOMÉTRICA (OMS 2006 / 2007)
//
// METODOLOGÍA OFICIAL OMS:
// 1. Menores de 2 años (< 24 meses) con peso y longitud:
//    → Peso-para-la-longitud (WFL) OMS 2006 (0–2 años / 45–110 cm).
//      Evalúa adecuadamente la proporción corporal (emaciación / sobrepeso / obesidad).
// 2. Niños de 2 a 5 años (24–60 meses):
//    → IMC-para-la-edad OMS 2006 (cortes P3, P15, P85, P97 / -2DE, -1DE, +1DE, +2DE).
// 3. Niños de 5 a 12 años (61–144 meses):
//    → IMC-para-la-edad WHO Growth Reference 2007:
//      - Obesidad: > +2 DE (> P97)
//      - Sobrepeso: > +1 DE a +2 DE (P85 a P97)
//      - Normal: -2 DE a +1 DE (P15 a P85)
//      - Delgadez (Bajo peso): -3 DE a -2 DE (P3 a P15)
//      - Delgadez severa (Bajo peso severo): < -3 DE (< P3)
// 4. Sin longitud/talla registrada (solo peso disponible):
//    → Peso-para-la-edad OMS (WFA) como evaluación ponderal orientativa.
// ═══════════════════════════════════════════════════════════════════════════

fun interpretarIMC(
    imc: Double,
    meses: Int,
    sexo: Sexo? = null,
    pesoKg: Double = 0.0,
    tallaCm: Double = 0.0
): InterpretacionIMC {
    val sinSexo   = sexo == null
    val sexoLabel = when (sexo) {
        Sexo.NINO -> " (niño)"
        Sexo.NINA -> " (niña)"
        null      -> " (orientativo — registra el sexo para mayor precisión)"
    }

    // ── 1. MENORES DE 2 AÑOS (< 24 meses) CON PESO Y LONGITUD ──────────────────
    // Indicador oficial OMS 2006: Peso-para-la-longitud (WFL)
    if (meses < 24 && pesoKg > 0.0 && tallaCm >= 45.0) {
        val indLabel = "Clasificación basada en peso-para-longitud OMS"
        val tablaWfl = omsWflPorSexo(sexo)
        val refWfl   = interpolarPuntoOMSLongitud(tablaWfl, tallaCm.coerceIn(45.0, 110.0))
        val p50      = ((refWfl.p50 * 10.0).roundToInt() / 10.0)
        val sd2neg   = ((refWfl.sd2neg * 10.0).roundToInt() / 10.0)
        val sd3neg   = ((refWfl.sd3neg * 10.0).roundToInt() / 10.0)
        val sd1pos   = ((refWfl.sd1pos * 10.0).roundToInt() / 10.0)
        val sd2pos   = ((refWfl.sd2pos * 10.0).roundToInt() / 10.0)
        val sd3pos   = ((refWfl.sd3pos * 10.0).roundToInt() / 10.0)

        return when {
            pesoKg < refWfl.sd3neg -> InterpretacionIMC(
                categoria   = "Bajo peso severo",
                descripcion = "Peso actual ($pesoKg kg) muy bajo para la longitud ($tallaCm cm) [< -3 DE OMS: $sd3neg kg]$sexoLabel. $indLabel. Consulta pediátrica prioritaria.",
                color       = 0xFFE53935,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
            pesoKg < refWfl.sd2neg -> InterpretacionIMC(
                categoria   = "Bajo peso",
                descripcion = "Peso actual ($pesoKg kg) bajo para la longitud ($tallaCm cm) [< -2 DE OMS: $sd2neg kg]$sexoLabel. $indLabel. Monitoreo pediátrico.",
                color       = 0xFFFF7043,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
            pesoKg > refWfl.sd3pos -> InterpretacionIMC(
                categoria   = "Obesidad",
                descripcion = "Peso actual ($pesoKg kg) muy elevado para la longitud ($tallaCm cm) [> +3 DE OMS: $sd3pos kg]$sexoLabel. $indLabel. Valoración médica requerida.",
                color       = 0xFFE53935,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
            pesoKg > refWfl.sd2pos -> InterpretacionIMC(
                categoria   = "Sobrepeso",
                descripcion = "Peso actual ($pesoKg kg) elevado para la longitud ($tallaCm cm) [> +2 DE OMS: $sd2pos kg]$sexoLabel. $indLabel. Valoración nutricional recomendada.",
                color       = 0xFFE53935,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
            pesoKg > refWfl.sd1pos -> InterpretacionIMC(
                categoria   = "Riesgo sobrepeso",
                descripcion = "Peso actual ($pesoKg kg) en rango de riesgo para la longitud ($tallaCm cm) [> +1 DE OMS: $sd1pos kg]$sexoLabel. $indLabel. Monitorear curva.",
                color       = 0xFFFFB300,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
            else -> InterpretacionIMC(
                categoria   = "Normal",
                descripcion = "Peso actual ($pesoKg kg) adecuado para la longitud ($tallaCm cm) [Mediana OMS: $p50 kg]$sexoLabel. $indLabel.",
                color       = 0xFF43A047,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
        }
    }

    // ── 2. NIÑOS DE 5 A 12 AÑOS (61–144 meses) / WHO Reference 2007 (Z-Scores Oficiales LMS) ──
    if (meses in 61..144 && imc > 0.0) {
        val indLabel = "Clasificación basada en IMC-para-edad OMS 2007 (5–12 años)"
        val tablaLms = if (sexo == Sexo.NINA) OMS_IMC_2007_NINAS else OMS_IMC_2007_NINOS
        val refLms   = interpolarPuntoOMSLms(tablaLms, meses.coerceIn(60, 144))
        val zScore   = calcularZScoreLms(imc, refLms.l, refLms.m, refLms.s)
        val zRound   = ((zScore * 100.0).roundToInt() / 100.0)
        val zSign    = if (zRound > 0) "+$zRound" else "$zRound"
        val imcRound = ((imc * 10.0).roundToInt() / 10.0)

        // Puntos de corte oficiales WHO Growth Reference 2007 (5–19 años):
        // Z < -3.0          → Delgadez severa
        // -3.0 <= Z < -2.0  → Delgadez (Bajo peso)
        // -2.0 <= Z <= +1.0 → Normal
        // +1.0 < Z <= +2.0  → Sobrepeso
        // Z > +2.0          → Obesidad
        return when {
            zScore < -3.0 -> InterpretacionIMC(
                categoria   = "Delgadez severa",
                descripcion = "IMC ($imcRound) con Z-score $zSign (< -3 DE OMS 2007 / Delgadez severa)$sexoLabel. $indLabel. Consulta médica prioritaria.",
                color       = 0xFFE53935,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
            zScore < -2.0 -> InterpretacionIMC(
                categoria   = "Delgadez",
                descripcion = "IMC ($imcRound) con Z-score $zSign (-3 a -2 DE OMS 2007 / Delgadez)$sexoLabel. $indLabel. Monitoreo pediátrico.",
                color       = 0xFFFF7043,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
            zScore > 2.0 -> InterpretacionIMC(
                categoria   = "Obesidad",
                descripcion = "IMC ($imcRound) con Z-score $zSign (> +2 DE OMS 2007 / Obesidad escolar)$sexoLabel. $indLabel. Valoración nutricional recomendada.",
                color       = 0xFFE53935,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
            zScore > 1.0 -> InterpretacionIMC(
                categoria   = "Sobrepeso",
                descripcion = "IMC ($imcRound) con Z-score $zSign (+1 a +2 DE OMS 2007 / Sobrepeso escolar)$sexoLabel. $indLabel. Monitorear curva y hábitos.",
                color       = 0xFFFFB300,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
            else -> InterpretacionIMC(
                categoria   = "Normal",
                descripcion = "IMC ($imcRound) con Z-score $zSign (-2 a +1 DE OMS 2007 / Rango saludable)$sexoLabel. $indLabel.",
                color       = 0xFF43A047,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
        }
    }

    // ── 3. NIÑOS DE 0 A 5 AÑOS (0–60 meses) CON IMC CALCULADO ─────────────────
    if (imc > 0.0) {
        val indLabel = if (meses < 24) "Clasificación basada en IMC-para-edad OMS (< 2 años)" else "Clasificación basada en IMC-para-edad OMS (2–5 años)"
        val tabla    = if (sexo == Sexo.NINA) OMS_IMC_NINAS else OMS_IMC_NINOS
        val ref      = interpolarPuntoOMS(tabla, meses.coerceIn(0, 60))
        val imcRound = ((imc * 10.0).roundToInt() / 10.0)

        return when {
            imc < ref.p3 -> InterpretacionIMC(
                categoria   = "Bajo peso severo",
                descripcion = "IMC ($imcRound) inferior a P3 OMS (< -2 DE)$sexoLabel. $indLabel. Consulta pediátrica requerida.",
                color       = 0xFFE53935,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
            imc < ref.p15 -> InterpretacionIMC(
                categoria   = "Bajo peso",
                descripcion = "IMC ($imcRound) en rango P3–P15 OMS (-2 a -1 DE)$sexoLabel. $indLabel. Monitorear con el pediatra.",
                color       = 0xFFFF7043,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
            imc > ref.p97 -> InterpretacionIMC(
                categoria   = "Sobrepeso",
                descripcion = "IMC ($imcRound) superior a P97 OMS (> +2 DE)$sexoLabel. $indLabel. Valoración pediátrica recomendada.",
                color       = 0xFFE53935,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
            imc > ref.p85 -> InterpretacionIMC(
                categoria   = "Riesgo sobrepeso",
                descripcion = "IMC ($imcRound) en rango P85–P97 OMS (+1 a +2 DE)$sexoLabel. $indLabel. Monitorear curva.",
                color       = 0xFFFFB300,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
            else -> InterpretacionIMC(
                categoria   = "Normal",
                descripcion = "IMC ($imcRound) en rango saludable P15–P85 OMS$sexoLabel. $indLabel.",
                color       = 0xFF43A047,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
        }
    }

    // ── 4. EVALUACIÓN PONDERAL CUANDO SOLO SE TIENE PESO (SIN TALLA) ──────────
    if (pesoKg > 0.0) {
        val indLabel  = "Evaluación ponderal basada en Peso-para-edad OMS"
        val tablaPeso = omsPesoPorSexo(sexo)
        val refPeso   = interpolarPuntoOMS(tablaPeso, meses.coerceIn(0, 120))
        val p3        = ((refPeso.p3 * 10.0).roundToInt() / 10.0)
        val p15       = ((refPeso.p15 * 10.0).roundToInt() / 10.0)
        val p85       = ((refPeso.p85 * 10.0).roundToInt() / 10.0)
        val p97       = ((refPeso.p97 * 10.0).roundToInt() / 10.0)

        return when {
            pesoKg < refPeso.p3 -> InterpretacionIMC(
                categoria   = "Bajo peso severo",
                descripcion = "Peso actual ($pesoKg kg) bajo P3 OMS ($p3 kg) a los $meses m$sexoLabel. $indLabel. (Registra la talla para clasificar estado nutricional completo).",
                color       = 0xFFE53935,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
            pesoKg < refPeso.p15 -> InterpretacionIMC(
                categoria   = "Bajo peso",
                descripcion = "Peso actual ($pesoKg kg) en rango P3–P15 OMS ($p15 kg) a los $meses m$sexoLabel. $indLabel. (Registra la talla para clasificar estado nutricional completo).",
                color       = 0xFFFF7043,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
            pesoKg > refPeso.p97 -> InterpretacionIMC(
                categoria   = "Riesgo sobrepeso",
                descripcion = "Peso actual ($pesoKg kg) superior a P97 OMS ($p97 kg) a los $meses m$sexoLabel. $indLabel. (Requiere correlación con la talla).",
                color       = 0xFFFFB300,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
            pesoKg > refPeso.p85 -> InterpretacionIMC(
                categoria   = "Riesgo sobrepeso",
                descripcion = "Peso actual ($pesoKg kg) en rango P85–P97 OMS ($p85–$p97 kg) a los $meses m$sexoLabel. $indLabel. (Requiere correlación con la talla).",
                color       = 0xFFFFB300,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
            else -> InterpretacionIMC(
                categoria   = "Normal",
                descripcion = "Peso actual ($pesoKg kg) en rango saludable P15–P85 OMS a los $meses m$sexoLabel. $indLabel.",
                color       = 0xFF43A047,
                esSinSexo   = sinSexo,
                indicador   = indLabel
            )
        }
    }

    return InterpretacionIMC("Normal", "Datos insuficientes para evaluación antropométrica.", 0xFF43A047, sinSexo, "Datos insuficientes")
}

package com.example.nutriia.crecimiento

import com.example.nutriia.shared.Timestamp
import kotlinx.serialization.Serializable

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

data class InterpretacionIMC(
    val categoria:   String,
    val descripcion: String,
    val color:       Long,
    /** true cuando se calculó sin sexo registrado — la UI lo muestra con aviso */
    val esSinSexo: Boolean = false
)

// ═══════════════════════════════════════════════════════════════════════════
// METADATOS DE FUENTES
// ═══════════════════════════════════════════════════════════════════════════

object FuentesCrecimiento {
    // WHO Child Growth Standards 2006 (0–5 años)
    const val WHO_GROWTH_URL    = "https://www.who.int/tools/child-growth-standards"
    const val WHO_WEIGHT_URL    = "https://www.who.int/tools/child-growth-standards/standards/weight-for-age"
    const val WHO_HEIGHT_URL    = "https://www.who.int/tools/child-growth-standards/standards/length-height-for-age"
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
    // ── 0–60 m: WHO Child Growth Standards 2006 ──────────────────────────
    PuntoOMS(0,   2.5,  2.9,  3.3,  3.9,  4.4),
    PuntoOMS(3,   5.0,  5.7,  6.4,  7.2,  7.9),
    PuntoOMS(6,   6.4,  7.1,  7.9,  8.8,  9.7),
    PuntoOMS(9,   7.3,  8.2,  9.2, 10.2, 11.0),
    PuntoOMS(12,  7.8,  8.8,  9.9, 11.0, 11.9),
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
    // ── 61–120 m: WHO Growth Reference 2007 ─────────────────────────────
    // Fuente: wfa-boys--5-10years-per.pdf (cdn.who.int)
    // Selección: mes 66, 72, 78, 84, 90, 96, 102, 108, 114, 120
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
// 0–60 m  → WHO Child Growth Standards 2006
// 61–120 m → WHO Growth Reference 2007 (wfa-girls-5-10years-per.pdf)
//   Fuente PDF: cdn.who.int/.../wfa-girls-5-10years-per.pdf
// ═══════════════════════════════════════════════════════════════════════════

val OMS_PESO_NINAS = listOf(
    // ── 0–60 m: WHO Child Growth Standards 2006 ──────────────────────────
    PuntoOMS(0,   2.4,  2.8,  3.2,  3.7,  4.2),
    PuntoOMS(3,   4.5,  5.2,  5.8,  6.6,  7.3),
    PuntoOMS(6,   5.7,  6.5,  7.3,  8.2,  9.3),
    PuntoOMS(9,   6.5,  7.3,  8.2,  9.3, 10.4),
    PuntoOMS(12,  7.0,  7.9,  8.9, 10.1, 11.3),
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
    // Fuente: wfa-girls-5-10years-per.pdf (cdn.who.int)
    // Columnas: P3 | P15 | P50 | P85 | P97
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
//   Fuente PDF: cdn.who.int/.../hfa-boys-5-19years-per.pdf
//   0–24 m = longitud (acostado); > 24 m = talla (de pie).
//   OMS aplica corrección +0.7 cm al pasar de longitud a talla.
// ═══════════════════════════════════════════════════════════════════════════

val OMS_TALLA_NINOS = listOf(
    // ── 0–60 m: WHO Child Growth Standards 2006 ──────────────────────────
    PuntoOMS(0,   46.3,  48.0,  49.9,  51.8,  53.4),
    PuntoOMS(3,   57.3,  59.4,  61.4,  63.5,  65.3),
    PuntoOMS(6,   63.3,  65.5,  67.6,  69.8,  71.6),
    PuntoOMS(9,   68.0,  70.1,  72.3,  74.5,  76.5),
    PuntoOMS(12,  71.7,  73.9,  75.7,  78.6,  80.5),
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
    // Fuente: hfa-boys-5-19years-per.pdf (cdn.who.int)
    // Columnas: P3 | P15 | P50 | P85 | P97  (cada 6 meses)
    PuntoOMS(66,  104.0, 107.5, 112.9, 116.1, 119.5), // 5:6
    PuntoOMS(72,  106.7, 110.3, 116.0, 119.3, 122.8), // 6:0
    PuntoOMS(78,  109.3, 113.0, 118.9, 122.2, 125.7), // 6:6
    PuntoOMS(84,  111.8, 115.7, 121.7, 125.2, 128.7), // 7:0
    PuntoOMS(90,  114.3, 118.3, 124.5, 128.2, 131.7), // 7:6
    PuntoOMS(96,  116.6, 120.8, 127.3, 131.1, 134.8), // 8:0
    PuntoOMS(102, 119.0, 123.3, 129.9, 133.9, 137.5), // 8:6
    PuntoOMS(108, 121.3, 125.7, 132.6, 136.6, 140.4), // 9:0
    PuntoOMS(114, 123.5, 128.1, 135.2, 139.4, 143.2), // 9:6
    PuntoOMS(120, 125.8, 130.5, 137.8, 142.1, 146.0), // 10:0
    PuntoOMS(126, 128.1, 132.9, 140.4, 144.8, 148.7), // 10:6
    PuntoOMS(132, 130.5, 135.4, 143.1, 147.7, 151.8), // 11:0
    PuntoOMS(138, 133.0, 138.0, 146.0, 150.6, 155.0), // 11:6
    PuntoOMS(144, 135.8, 140.7, 149.1, 153.9, 158.4)  // 12:0
)

// ═══════════════════════════════════════════════════════════════════════════
// CURVAS OMS TALLA POR EDAD — NIÑAS (girls)
//
// 0–60 m  → WHO Child Growth Standards 2006
// 61–144 m → WHO Growth Reference 2007 (hfa-girls-5-19years-per.pdf)
//   Fuente PDF: cdn.who.int/.../hfa-girls-5-19years-per.pdf
// ═══════════════════════════════════════════════════════════════════════════

val OMS_TALLA_NINAS = listOf(
    // ── 0–60 m: WHO Child Growth Standards 2006 ──────────────────────────
    PuntoOMS(0,   45.6,  47.3,  49.1,  51.0,  52.7),
    PuntoOMS(3,   55.6,  57.7,  59.8,  61.9,  63.8),
    PuntoOMS(6,   61.2,  63.5,  65.7,  68.0,  69.9),
    PuntoOMS(9,   66.0,  68.2,  70.4,  72.8,  74.9),
    PuntoOMS(12,  69.2,  71.6,  74.0,  76.4,  78.6),
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
    // Fuente: hfa-girls-5-19years-per.pdf (cdn.who.int)
    // Columnas: P3 | P15 | P50 | P85 | P97  (cada 6 meses)
    PuntoOMS(66,  102.9, 106.6, 112.2, 116.3, 119.6), // 5:6
    PuntoOMS(72,  105.5, 109.3, 115.1, 119.2, 122.7), // 6:0
    PuntoOMS(78,  108.0, 112.0, 118.0, 122.0, 125.6), // 6:6
    PuntoOMS(84,  110.5, 114.6, 120.8, 124.9, 128.5), // 7:0
    PuntoOMS(90,  113.1, 117.3, 123.7, 127.8, 131.5), // 7:6
    PuntoOMS(96,  115.7, 120.1, 126.6, 130.8, 134.5), // 8:0
    PuntoOMS(102, 118.3, 122.8, 129.5, 133.7, 137.5), // 8:6
    PuntoOMS(108, 121.0, 125.7, 132.5, 136.8, 140.7), // 9:0
    PuntoOMS(114, 123.8, 128.6, 135.5, 140.0, 143.9), // 9:6
    PuntoOMS(120, 126.6, 131.5, 138.6, 143.2, 147.2), // 10:0
    PuntoOMS(126, 129.5, 134.5, 141.8, 146.4, 150.4), // 10:6
    PuntoOMS(132, 132.5, 137.6, 145.0, 149.6, 153.6), // 11:0
    PuntoOMS(138, 135.5, 140.7, 148.2, 152.7, 156.7), // 11:6
    PuntoOMS(144, 138.4, 143.6, 151.2, 155.8, 159.8)  // 12:0
)

// ═══════════════════════════════════════════════════════════════════════════
// TABLAS IMC-PARA-EDAD OMS — NIÑOS (boys)
//
// 0–60 m  → WHO Child Growth Standards 2006 (bmi-for-age-boys-percentiles)
// 61–144 m → WHO Growth Reference 2007 (bmifa-boys-5-19years-per.pdf)
//   Fuente PDF: cdn.who.int/.../bmifa-boys-5-19years-per.pdf
//   Columnas: P3 | P15 | P50 | P85 | P97  (cada 6 meses)
// ═══════════════════════════════════════════════════════════════════════════

private val OMS_IMC_NINOS = listOf(
    // ── 0–60 m: WHO Child Growth Standards 2006 ──────────────────────────
    PuntoOMS(0,  10.2, 11.1, 13.4, 15.3, 16.7),
    PuntoOMS(6,  13.5, 14.5, 16.1, 17.7, 19.0),
    PuntoOMS(12, 13.4, 14.4, 15.9, 17.4, 18.7),
    PuntoOMS(18, 13.4, 14.3, 15.7, 17.1, 18.3),
    PuntoOMS(24, 13.4, 14.2, 15.5, 16.9, 18.1),
    PuntoOMS(30, 13.2, 14.0, 15.3, 16.7, 18.0),
    PuntoOMS(36, 13.1, 13.8, 15.1, 16.6, 17.9),
    PuntoOMS(42, 13.0, 13.7, 15.0, 16.6, 18.0),
    PuntoOMS(48, 12.9, 13.6, 14.9, 16.6, 18.1),
    PuntoOMS(54, 12.8, 13.5, 14.9, 16.6, 18.2),
    PuntoOMS(60, 12.7, 13.4, 14.8, 16.6, 18.3),
    // ── 61–144 m: WHO Growth Reference 2007 ─────────────────────────────
    // Fuente: bmifa-boys-5-19years-per.pdf (cdn.who.int)
    PuntoOMS(66,  13.1, 13.8, 15.3, 17.0, 18.5), // 5:6
    PuntoOMS(72,  13.2, 14.0, 15.3, 16.9, 18.5), // 6:0
    PuntoOMS(78,  13.2, 14.1, 15.4, 16.9, 18.5), // 6:6
    PuntoOMS(84,  13.3, 14.2, 15.5, 17.1, 18.8), // 7:0
    PuntoOMS(90,  13.3, 14.2, 15.6, 17.3, 19.0), // 7:6
    PuntoOMS(96,  13.4, 14.4, 15.7, 17.5, 19.4), // 8:0
    PuntoOMS(102, 13.5, 14.5, 15.9, 17.7, 19.7), // 8:6
    PuntoOMS(108, 13.6, 14.6, 16.0, 18.0, 20.1), // 9:0
    PuntoOMS(114, 13.7, 14.8, 16.2, 18.3, 20.5), // 9:6
    PuntoOMS(120, 13.9, 14.9, 16.4, 18.6, 21.0), // 10:0
    PuntoOMS(126, 14.0, 15.1, 16.7, 18.9, 21.5), // 10:6
    PuntoOMS(132, 14.2, 15.3, 16.9, 19.3, 22.0), // 11:0
    PuntoOMS(138, 14.4, 15.5, 17.2, 19.6, 22.5), // 11:6
    PuntoOMS(144, 14.6, 15.7, 17.5, 20.1, 23.1)  // 12:0
)

// ═══════════════════════════════════════════════════════════════════════════
// TABLAS IMC-PARA-EDAD OMS — NIÑAS (girls)
//
// 0–60 m  → WHO Child Growth Standards 2006
// 61–144 m → WHO Growth Reference 2007 (bmifa-girls-5-19years-per.pdf)
//   Fuente PDF: cdn.who.int/.../bmifa-girls-5-19years-per.pdf
// ═══════════════════════════════════════════════════════════════════════════

private val OMS_IMC_NINAS = listOf(
    // ── 0–60 m: WHO Child Growth Standards 2006 ──────────────────────────
    PuntoOMS(0,  10.1, 11.0, 13.2, 15.2, 16.8),
    PuntoOMS(6,  12.7, 13.6, 15.2, 16.9, 18.5),
    PuntoOMS(12, 12.7, 13.6, 15.2, 16.9, 18.4),
    PuntoOMS(18, 12.8, 13.6, 15.1, 16.6, 18.1),
    PuntoOMS(24, 12.9, 13.7, 15.0, 16.4, 17.8),
    PuntoOMS(30, 12.9, 13.6, 14.9, 16.3, 17.6),
    PuntoOMS(36, 12.9, 13.6, 14.8, 16.2, 17.6),
    PuntoOMS(42, 12.8, 13.5, 14.8, 16.2, 17.6),
    PuntoOMS(48, 12.7, 13.4, 14.7, 16.2, 17.7),
    PuntoOMS(54, 12.7, 13.3, 14.7, 16.2, 17.8),
    PuntoOMS(60, 12.6, 13.3, 14.7, 16.2, 17.9),
    // ── 61–144 m: WHO Growth Reference 2007 ─────────────────────────────
    // Fuente: bmifa-girls-5-19years-per.pdf (cdn.who.int)
    PuntoOMS(66,  12.8, 13.8, 15.2, 17.0, 18.7), // 5:6
    PuntoOMS(72,  12.8, 13.8, 15.3, 17.1, 18.9), // 6:0
    PuntoOMS(78,  12.8, 13.9, 15.3, 17.2, 19.2), // 6:6
    PuntoOMS(84,  12.9, 13.9, 15.4, 17.4, 19.4), // 7:0
    PuntoOMS(90,  12.9, 14.0, 15.5, 17.6, 19.8), // 7:6
    PuntoOMS(96,  13.0, 14.1, 15.7, 17.8, 20.2), // 8:0
    PuntoOMS(102, 13.1, 14.2, 15.9, 18.1, 20.6), // 8:6
    PuntoOMS(108, 13.3, 14.4, 16.1, 18.4, 21.1), // 9:0
    PuntoOMS(114, 13.4, 14.6, 16.4, 18.8, 21.6), // 9:6
    PuntoOMS(120, 13.6, 14.8, 16.6, 19.1, 22.1), // 10:0
    PuntoOMS(126, 13.8, 15.0, 16.9, 19.5, 22.6), // 10:6
    PuntoOMS(132, 14.0, 15.3, 17.2, 20.0, 23.2), // 11:0
    PuntoOMS(138, 14.3, 15.6, 17.6, 20.4, 23.8), // 11:6
    PuntoOMS(144, 14.6, 15.9, 18.0, 20.9, 24.4)  // 12:0
)

// ═══════════════════════════════════════════════════════════════════════════
// FUNCIONES DE SELECCIÓN POR SEXO
// ═══════════════════════════════════════════════════════════════════════════

fun omsPesoPorSexo(sexo: Sexo?)  = if (sexo == Sexo.NINA) OMS_PESO_NINAS  else OMS_PESO_NINOS
fun omsTallaPorSexo(sexo: Sexo?) = if (sexo == Sexo.NINA) OMS_TALLA_NINAS else OMS_TALLA_NINOS

// ═══════════════════════════════════════════════════════════════════════════
// INTERPOLACIÓN LINEAL ENTRE PUNTOS OMS
// Permite obtener el valor referencia para cualquier mes intermedio.
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

// ═══════════════════════════════════════════════════════════════════════════
// INTERPRETACIÓN IMC
//
// Metodología (WHO 2006/2007):
//   < 24 m  → OMS recomienda peso-para-longitud (WFL), no IMC.
//              Se muestra orientativo con aviso explícito.
//   24–60 m → IMC-para-edad según tablas OMS 2006 con interpolación.
//              Cortes: P3 / P15 / P85 / P97.
//   61–144 m → IMC-para-edad según WHO Reference 2007 con interpolación.
//              Cortes: P3 / P15 / P85 / P97.
//
// Sin sexo registrado: usa tabla de niños como aproximación y marca
// esSinSexo=true para que la UI muestre aviso al usuario.
//
// Fuentes:
//   • WHO Child Growth Standards 2006
//     https://www.who.int/tools/child-growth-standards
//   • WHO Growth Reference 2007 — de Onis et al., Bull WHO 2007;85(9):660-667
//     https://www.who.int/tools/growth-reference-data-for-5to19-years
// ═══════════════════════════════════════════════════════════════════════════

fun interpretarIMC(imc: Double, meses: Int, sexo: Sexo? = null): InterpretacionIMC {
    val sinSexo   = sexo == null
    val sexoLabel = when (sexo) {
        Sexo.NINO -> " (niño, P OMS)"
        Sexo.NINA -> " (niña, P OMS)"
        null      -> " (orientativo — registra el sexo para mayor precisión)"
    }

    // ── < 24 meses: OMS indica WFL, no IMC ───────────────────────────────
    if (meses < 24) {
        val desc = "La OMS recomienda el índice peso-para-longitud en < 2 años.$sexoLabel Consulta al pediatra."
        return when {
            imc < 14.0 -> InterpretacionIMC("Bajo peso",        desc, 0xFFE53935, sinSexo)
            imc < 18.0 -> InterpretacionIMC("Normal",           desc, 0xFF43A047, sinSexo)
            imc < 20.0 -> InterpretacionIMC("Riesgo sobrepeso", desc, 0xFFFFB300, sinSexo)
            else       -> InterpretacionIMC("Sobrepeso",        desc, 0xFFE53935, sinSexo)
        }
    }

    // ── ≥ 24 meses: tablas OMS con interpolación (límite 144 m) ──────────
    val tabla = if (sexo == Sexo.NINA) OMS_IMC_NINAS else OMS_IMC_NINOS
    val ref   = interpolarPuntoOMS(tabla, meses.coerceIn(0, 144))

    return when {
        imc < ref.p3  -> InterpretacionIMC(
            "Bajo peso severo",
            "Por debajo del percentil 3 OMS$sexoLabel. Consulta urgente al pediatra.",
            0xFFE53935, sinSexo
        )
        imc < ref.p15 -> InterpretacionIMC(
            "Bajo peso",
            "Entre P3–P15 OMS$sexoLabel. Monitorear con el pediatra.",
            0xFFFF7043, sinSexo
        )
        imc < ref.p85 -> InterpretacionIMC(
            "Normal",
            "Entre P15–P85 OMS$sexoLabel. Rango saludable.",
            0xFF43A047, sinSexo
        )
        imc < ref.p97 -> InterpretacionIMC(
            "Riesgo sobrepeso",
            "Entre P85–P97 OMS$sexoLabel. Monitorear con el pediatra.",
            0xFFFFB300, sinSexo
        )
        else -> InterpretacionIMC(
            "Sobrepeso",
            "Por encima del P97 OMS$sexoLabel. Valoración pediátrica recomendada.",
            0xFFE53935, sinSexo
        )
    }
}
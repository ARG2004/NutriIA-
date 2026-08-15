package com.example.nutriia.embarazo

import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.RegionMexico
import com.example.nutriia.sueldo.Alergeno   // reutilizar el enum existente, no duplicar

enum class TrimestreEmbarazo(val label: String, val semanaMin: Int, val semanaMax: Int) {
    PRIMERO(  "Primer trimestre",  1, 13),
    SEGUNDO(  "Segundo trimestre", 14, 27),
    TERCERO(  "Tercer trimestre",  28, 42)
}

data class MacroObjetivoEmbarazo(
    val caloriasExtra: Int,      // kcal adicionales sobre la dieta basal, por trimestre
    val proteinasG:    Double,
    val hierroMg:      Double,
    val calcioMg:      Double,
    val acidoFolicoUg: Double,
    val omega3G:        Double,
    val aguaLitros:    Double
)

data class RecetaEmbarazo(
    val nombre:              String,
    val ingredientes:        List<String>,
    val preparacion:         String,
    val kcal:                Int,
    val tipoComida:          com.example.nutriia.sueldo.TipoComida,  // reutilizar enum existente
    val trimestreMinimo:     TrimestreEmbarazo = TrimestreEmbarazo.PRIMERO,
    val nivelMinimo:         NivelIngreso = NivelIngreso.BASICO,
    val fuente:              String,
    val regiones:            List<RegionMexico> = listOf(RegionMexico.GENERAL),
    val alergenos:           List<Alergeno> = emptyList(),
    val condicionesExcluidas: List<String> = emptyList(), // ej. "diabetes gestacional", "hipertension"
    val toleranciaNauseas:   Boolean = true
) {
    fun contieneIngrediente(nombre: String): Boolean {
        val normQuery = nombre.lowercase().trim()
        val nameNorm = this.nombre.lowercase()
        
        if (nameNorm.contains(normQuery) || normQuery.contains(nameNorm)) return true
        
        return ingredientes.any { ing ->
            val ingNorm = ing.lowercase()
            if (ingNorm.contains(normQuery) || normQuery.contains(ingNorm)) return true
            
            val queryWords = normQuery.split(Regex("[\\s,]+")).filter { it.length > 3 }
            val ingWords = ingNorm.split(Regex("[\\s,]+")).filter { it.length > 3 }
            
            queryWords.any { qw ->
                ingWords.any { iw ->
                    val qwClean = qw.removeSuffix("s").removeSuffix("es")
                    val iwClean = iw.removeSuffix("s").removeSuffix("es")
                    iwClean.contains(qwClean) || qwClean.contains(iwClean)
                }
            }
        }
    }

    fun esSeguraParaPerfil(alergenosPerfil: List<Alergeno>, condicionesPerfil: List<String>): Boolean {
        val sinAlergenos = alergenos.none { it in alergenosPerfil }
        val condicionesNormalizadas = condicionesPerfil.map { it.lowercase() }
        val sinConflictoCondicion = condicionesExcluidas.none { excl ->
            condicionesNormalizadas.any { it.contains(excl.lowercase()) }
        }
        val aptoNauseas = if (condicionesNormalizadas.any { it.contains("náuseas") || it.contains("vómitos") || it.contains("nausea") }) {
            toleranciaNauseas
        } else {
            true
        }
        return sinAlergenos && sinConflictoCondicion && aptoNauseas
    }
}

data class ComidasDiariasEmbarazo(
    val desayuno:  String,
    val colacion1: String,
    val comida:    String,
    val colacion2: String,
    val cena:      String
)

data class PlanDietaEmbarazoSemanal(
    val diaSemana:    String,
    val comidas:      ComidasDiariasEmbarazo,
    val macros:       MacroObjetivoEmbarazo,
    val trimestre:    TrimestreEmbarazo
)

data class AlimentoRiesgoEmbarazo(
    val nombre: String,
    val motivo: String
)

data class ResumenNutricionalEmbarazo(
    val trimestreLabel:      String,
    val macroObjetivo:       MacroObjetivoEmbarazo,
    val alimentosClave:      List<String>,
    val alertas:             List<String>,
    val alimentosAEvitar:    List<AlimentoRiesgoEmbarazo>,
    val alertasCondicion:    List<String> = emptyList()
)

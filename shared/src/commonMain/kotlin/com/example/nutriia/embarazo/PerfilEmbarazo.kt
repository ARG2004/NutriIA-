package com.example.nutriia.embarazo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.RegionMexico
import com.example.nutriia.sueldo.Alergeno
import com.example.nutriia.ui.theme.parsearAlergenos   // reutilizar el existente, NO duplicar

@Parcelize
data class PerfilEmbarazo(
    val semanas:                Int          = 1,
    val condiciones:            List<String> = emptyList(),
    val preferencias:           List<String> = emptyList(),
    val fechaUltimaMenstruacion: String      = "",
    val nivelIngreso:           NivelIngreso = NivelIngreso.BASICO,
    val region:                 RegionMexico = RegionMexico.CENTRO,
    val allergiesDetail:        String       = "",
    val edad:                   Int          = 0,
    val tallaM:                 Double       = 0.0,
    val pesoPregestacionalKg:   Double       = 0.0,
    val esGemelar:              Boolean      = false,
    val otrasCondicionesTexto:  String       = ""
) : Parcelable {

    val alergenosParsados: List<Alergeno> get() =
        if (allergiesDetail.isNotBlank()) parsearAlergenos(allergiesDetail) else emptyList()

    val imcPregestacional: Double get() =
        if (tallaM > 0) pesoPregestacionalKg / (tallaM * tallaM) else 0.0

    fun toMap(): Map<String, Any?> = mapOf(
        "semanas"                 to semanas,
        "condiciones"             to condiciones,
        "preferencias"            to preferencias,
        "fechaUltimaMenstruacion" to fechaUltimaMenstruacion,
        "nivelIngreso"            to nivelIngreso.index,
        "region"                  to region.name,
        "allergiesDetail"         to allergiesDetail,
        "edad"                    to edad,
        "tallaM"                  to tallaM,
        "pesoPregestacionalKg"    to pesoPregestacionalKg,
        "esGemelar"               to esGemelar,
        "otrasCondicionesTexto"   to otrasCondicionesTexto
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): PerfilEmbarazo = PerfilEmbarazo(
            semanas                 = (map["semanas"] as? Long)?.toInt() ?: (map["semanas"] as? Int) ?: 1,
            condiciones             = (map["condiciones"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            preferencias            = (map["preferencias"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            fechaUltimaMenstruacion = map["fechaUltimaMenstruacion"] as? String ?: "",
            nivelIngreso            = ((map["nivelIngreso"] as? Long)?.toInt() ?: (map["nivelIngreso"] as? Int))
                                        ?.let { NivelIngreso.fromIndex(it) } ?: NivelIngreso.BASICO,
            region                  = (map["region"] as? String)?.let { name ->
                                        RegionMexico.entries.firstOrNull { it.name == name }
                                        } ?: RegionMexico.CENTRO,
            allergiesDetail         = map["allergiesDetail"] as? String ?: "",
            edad                    = (map["edad"] as? Long)?.toInt() ?: (map["edad"] as? Int) ?: 0,
            tallaM                  = (map["tallaM"] as? Double) ?: (map["tallaM"] as? Long)?.toDouble() ?: (map["tallaM"] as? Float)?.toDouble() ?: 0.0,
            pesoPregestacionalKg    = (map["pesoPregestacionalKg"] as? Double) ?: (map["pesoPregestacionalKg"] as? Long)?.toDouble() ?: (map["pesoPregestacionalKg"] as? Float)?.toDouble() ?: 0.0,
            esGemelar               = map["esGemelar"] as? Boolean ?: false,
            otrasCondicionesTexto   = map["otrasCondicionesTexto"] as? String ?: ""
        )
    }
}

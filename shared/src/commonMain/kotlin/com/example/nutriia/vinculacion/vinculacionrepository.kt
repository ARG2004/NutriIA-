package com.example.nutriia.vinculacion

import com.example.nutriia.platform.currentTimeMillis
import com.example.nutriia.platform.generateUUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class VinculacionRepository {

    fun observarHijo(padreUid: String, childId: String): Flow<Map<String, Any?>?> = kotlinx.coroutines.flow.flowOf(null)

    private val vinculacionesState = MutableStateFlow<Map<String, Vinculacion>>(emptyMap())
    private val nutriologosState   = MutableStateFlow<Map<String, NutriologoPublico>>(emptyMap())
    private val planesState        = MutableStateFlow<Map<String, PlanAlimentario>>(emptyMap())

    fun observarVinculacionesDelPadre(padreUid: String = ""): Flow<List<Vinculacion>> {
        return vinculacionesState.map { map ->
            if (padreUid.isEmpty()) map.values.toList() else map.values.filter { it.padreUid == padreUid }
        }
    }

    fun observarVinculacionesDelNutriologo(nutriologoUid: String = ""): Flow<List<Vinculacion>> {
        return vinculacionesState.map { map ->
            if (nutriologoUid.isEmpty()) map.values.toList() else map.values.filter { it.nutriologoUid == nutriologoUid }
        }
    }

    suspend fun publicarPerfilNutriologo(
        nombre:       String,
        especialidad: String,
        cedula:       String,
        email:        String
    ): Result<NutriologoPublico> {
        val uid = "nutri_${nombre.hashCode()}"
        val codigo = "NUTRI-${generateUUID().take(6).uppercase()}"
        val perfil = NutriologoPublico(
            uid          = uid,
            nombre       = nombre,
            especialidad = especialidad,
            cedula       = cedula,
            codigo       = codigo,
            email        = email.trim().lowercase()
        )
        nutriologosState.value = nutriologosState.value + (uid to perfil)
        return Result.success(perfil)
    }

    suspend fun obtenerMiPerfilPublico(): Result<NutriologoPublico?> {
        return Result.success(nutriologosState.value.values.firstOrNull())
    }

    suspend fun responderSolicitud(vinculacionId: String, aceptar: Boolean): Result<Unit> {
        val current = vinculacionesState.value[vinculacionId] ?: return Result.failure(Exception("No encontrada"))
        val updated = current.copy(
            estado = if (aceptar) EstadoVinculacion.ACTIVO else EstadoVinculacion.RECHAZADO
        )
        vinculacionesState.value = vinculacionesState.value + (vinculacionId to updated)
        return Result.success(Unit)
    }

    suspend fun buscarNutriologoPorCodigo(codigo: String): Result<NutriologoPublico?> {
        val q = codigo.trim().uppercase()
        if (q.isEmpty()) return Result.success(null)
        val found = nutriologosState.value.values.firstOrNull { it.codigo.uppercase() == q }
        return Result.success(found)
    }

    suspend fun buscarNutriologoPorEmail(email: String): Result<NutriologoPublico?> {
        val e = email.trim().lowercase()
        if (e.isEmpty()) return Result.success(null)
        val found = nutriologosState.value.values.firstOrNull { it.email.lowercase() == e }
        return Result.success(found)
    }

    suspend fun listarNutriologos(limite: Long = 50): Result<List<NutriologoPublico>> {
        return Result.success(nutriologosState.value.values.take(limite.toInt()))
    }

    suspend fun buscarNutriologosEnDirectorio(query: String): Result<List<NutriologoPublico>> {
        val q = query.trim()
        if (q.isBlank()) return listarNutriologos()
        val filtrado = nutriologosState.value.values.filter {
            it.nombre.contains(q, ignoreCase = true) || it.especialidad.contains(q, ignoreCase = true)
        }
        return Result.success(filtrado)
    }

    suspend fun solicitarVinculacion(
        nutriologo:  NutriologoPublico,
        padreNombre: String,
        childId:     String,
        childNombre: String
    ): Result<Vinculacion> {
        val padreUid = "padre_default"
        val docId = Vinculacion.docId(nutriologo.uid, padreUid)
        val vinc = Vinculacion(
            id               = docId,
            nutriologoUid    = nutriologo.uid,
            nutriologoNombre = nutriologo.nombre,
            padreUid         = padreUid,
            padreNombre      = padreNombre,
            childId          = childId,
            childNombre      = childNombre,
            estado           = EstadoVinculacion.PENDIENTE,
            creadoEn         = currentTimeMillis()
        )
        vinculacionesState.value = vinculacionesState.value + (docId to vinc)
        return Result.success(vinc)
    }

    suspend fun revocarVinculacion(vinculacionId: String): Result<Unit> {
        val current = vinculacionesState.value[vinculacionId] ?: return Result.failure(Exception("No encontrada"))
        val updated = current.copy(estado = EstadoVinculacion.REVOCADO)
        vinculacionesState.value = vinculacionesState.value + (vinculacionId to updated)
        return Result.success(Unit)
    }

    suspend fun guardarPlan(plan: PlanAlimentario): Result<Unit> {
        val id = if (plan.id.isEmpty()) generateUUID() else plan.id
        val newPlan = plan.copy(id = id)
        planesState.value = planesState.value + (plan.childId to newPlan)
        return Result.success(Unit)
    }

    fun observarPlanActivo(childId: String): Flow<PlanAlimentario?> {
        return planesState.map { it[childId] }
    }
}

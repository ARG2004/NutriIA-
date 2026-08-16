package com.example.nutriia.ginecologo

import com.example.nutriia.platform.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class GinecologoRepository {

    private val ginecologosState = MutableStateFlow<Map<String, GinecologoPublico>>(emptyMap())
    private val vinculacionesState = MutableStateFlow<Map<String, VinculacionEmbarazo>>(emptyMap())
    private val citasState = MutableStateFlow<Map<String, List<CitaEmbarazo>>>(emptyMap())

    suspend fun publicarPerfilGinecologo(
        nombre:       String,
        especialidad: String,
        cedula:       String,
        email:        String
    ): Result<GinecologoPublico> {
        val uid = "user_${nombre.hashCode()}"
        val codigo = generarCodigo(nombre)
        val perfil = GinecologoPublico(
            uid          = uid,
            nombre       = nombre,
            especialidad = especialidad,
            cedula       = cedula,
            codigo       = codigo,
            email        = email.trim().lowercase()
        )
        ginecologosState.value = ginecologosState.value + (uid to perfil)
        return Result.success(perfil)
    }

    suspend fun buscarGinecologoPorCodigo(codigo: String): Result<GinecologoPublico?> {
        val q = codigo.trim().uppercase()
        if (q.isEmpty()) return Result.success(null)
        val found = ginecologosState.value.values.firstOrNull { it.codigo.uppercase() == q }
        return Result.success(found)
    }

    suspend fun buscarGinecologoPorEmail(email: String): Result<GinecologoPublico?> {
        val e = email.trim().lowercase()
        if (e.isEmpty()) return Result.success(null)
        val found = ginecologosState.value.values.firstOrNull { it.email.lowercase() == e }
        return Result.success(found)
    }

    suspend fun listarGinecologos(limite: Long = 50): Result<List<GinecologoPublico>> {
        return Result.success(ginecologosState.value.values.take(limite.toInt()))
    }

    suspend fun buscarGinecologosEnDirectorio(query: String): Result<List<GinecologoPublico>> {
        val q = query.trim()
        if (q.isBlank()) return listarGinecologos()
        val filtrado = ginecologosState.value.values.filter {
            it.nombre.contains(q, ignoreCase = true) || it.especialidad.contains(q, ignoreCase = true)
        }
        return Result.success(filtrado)
    }

    suspend fun solicitarVinculacion(
        ginecologo: GinecologoPublico,
        mamaNombre: String
    ): Result<VinculacionEmbarazo> {
        val mamaUid = "mama_default"
        val docId = VinculacionEmbarazo.docId(ginecologo.uid, mamaUid)
        val vinculacion = VinculacionEmbarazo(
            id               = docId,
            ginecologoUid    = ginecologo.uid,
            ginecologoNombre = ginecologo.nombre,
            mamaUid          = mamaUid,
            mamaNombre       = mamaNombre,
            estado           = EstadoVinculacionEmbarazo.PENDIENTE,
            creadoEn         = com.example.nutriia.platform.currentTimeMillis()
        )
        vinculacionesState.value = vinculacionesState.value + (docId to vinculacion)
        return Result.success(vinculacion)
    }

    suspend fun responderSolicitud(vinculacionId: String, aceptar: Boolean): Result<Unit> {
        val current = vinculacionesState.value[vinculacionId] ?: return Result.failure(Exception("No encontrada"))
        val updated = current.copy(
            estado = if (aceptar) EstadoVinculacionEmbarazo.ACTIVO else EstadoVinculacionEmbarazo.RECHAZADO
        )
        vinculacionesState.value = vinculacionesState.value + (vinculacionId to updated)
        return Result.success(Unit)
    }

    suspend fun revocarVinculacion(vinculacionId: String): Result<Unit> {
        val current = vinculacionesState.value[vinculacionId] ?: return Result.failure(Exception("No encontrada"))
        val updated = current.copy(estado = EstadoVinculacionEmbarazo.REVOCADO)
        vinculacionesState.value = vinculacionesState.value + (vinculacionId to updated)
        return Result.success(Unit)
    }

    suspend fun agendarCita(
        vinculacionId: String,
        fecha: String,
        hora: String,
        motivo: String,
        tipo: String
    ): Result<Unit> {
        val vinc = vinculacionesState.value[vinculacionId] ?: return Result.failure(Exception("No encontrada"))
        val cita = CitaEmbarazo(
            id = com.example.nutriia.platform.generateUUID(),
            fecha = fecha,
            hora = hora,
            motivo = motivo,
            tipo = tipo,
            ginecologoUid = vinc.ginecologoUid,
            ginecologoNombre = vinc.ginecologoNombre
        )
        val currentCitas = citasState.value[vinc.mamaUid] ?: emptyList()
        citasState.value = citasState.value + (vinc.mamaUid to (currentCitas + cita))
        return Result.success(Unit)
    }

    suspend fun cancelarCita(vinculacionId: String): Result<Unit> {
        return Result.success(Unit)
    }

    fun observarVinculacionDeLaMama(): Flow<VinculacionEmbarazo?> {
        return vinculacionesState.map { it.values.firstOrNull() }
    }

    fun observarCitasDeLaMama(): Flow<List<CitaEmbarazo>> {
        return citasState.map { it.values.flatten() }
    }

    fun observarVinculacionesDelGinecologo(): Flow<List<VinculacionEmbarazo>> {
        return vinculacionesState.map { it.values.toList() }
    }

    suspend fun obtenerMiPerfilPublico(): Result<GinecologoPublico?> {
        return Result.success(ginecologosState.value.values.firstOrNull())
    }

    private fun generarCodigo(nombre: String): String {
        val prefijo = nombre.take(4).uppercase().filter { it.isLetter() }.padEnd(4, 'X')
        val sufijo  = com.example.nutriia.platform.generateUUID().take(5).uppercase()
        return "GINE-$prefijo-$sufijo"
    }
}

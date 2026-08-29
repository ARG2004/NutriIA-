package com.example.nutriia.payment

import com.example.nutriia.platform.currentTimeMillis
import com.example.nutriia.platform.generateUUID
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore

class PaymentRepository {

    private val db get() = Firebase.firestore
    private val auth get() = Firebase.auth
    private val col get() = db.collection("pagos_teleconsulta")

    suspend fun crearPagoPendiente(
        nutriologoUid: String,
        childId:       String,
        montoCentavos: Int,
        moneda:        String = "MXN",
        padreUid:      String = "",
        paypalOrderId: String = ""
    ): Result<PagoTeleconsulta> {
        val uid = padreUid.ifBlank { auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado")) }

        return try {
            val id = generateUUID()
            val pago = PagoTeleconsulta(
                id            = id,
                padreUid      = uid,
                nutriologoUid = nutriologoUid,
                childId       = childId,
                montoCentavos = montoCentavos,
                moneda        = moneda,
                paypalOrderId = paypalOrderId,
                estado        = EstadoPago.PENDIENTE,
                creadaEn      = currentTimeMillis()
            )
            col.document(id).set(pago)
            Result.success(pago)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmarPago(pagoId: String, paypalOrderId: String = ""): Result<Unit> {
        return try {
            col.document(pagoId).update(
                mapOf(
                    "paypalOrderId" to paypalOrderId,
                    "estado"        to EstadoPago.COMPLETADO.name,
                    "completadoEn"  to currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelarPago(pagoId: String): Result<Unit> {
        return try {
            col.document(pagoId).update(mapOf("estado" to EstadoPago.FALLIDO.name))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verificarPagoCompletado(pagoId: String): Result<Boolean> {
        return try {
            val doc = col.document(pagoId).get()
            val pago = if (doc.exists) {
                try { doc.data<PagoTeleconsulta>().copy(id = doc.id) } catch (e: Exception) { null }
            } else null
            Result.success(pago?.estado == EstadoPago.COMPLETADO)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerPagoVigente(nutriologoUid: String, childId: String): Result<PagoTeleconsulta?> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
        return try {
            val query = col
                .where { "padreUid".equalTo(uid) }
                .where { "nutriologoUid".equalTo(nutriologoUid) }
                .where { "childId".equalTo(childId) }
                .where { "estado".equalTo(EstadoPago.COMPLETADO.name) }
                .get()

            val pago = query.documents.mapNotNull { doc ->
                try { doc.data<PagoTeleconsulta>().copy(id = doc.id) } catch (e: Exception) { null }
            }.firstOrNull { it.llamadaId.isEmpty() }

            Result.success(pago)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun marcarPagoUsado(pagoId: String, llamadaId: String): Result<Unit> {
        return try {
            col.document(pagoId).update(mapOf("llamadaId" to llamadaId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reactivarPago(pagoId: String): Result<Unit> {
        return try {
            col.document(pagoId).update(mapOf("llamadaId" to ""))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

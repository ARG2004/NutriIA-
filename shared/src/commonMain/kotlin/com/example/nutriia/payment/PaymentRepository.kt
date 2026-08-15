package com.example.nutriia.payment

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PaymentRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val col  get() = db.collection("pagos_teleconsulta")

    // ── Crea un documento de pago PENDIENTE antes de abrir PayPal ────────────
    suspend fun crearPagoPendiente(
        nutriologoUid: String,
        childId:       String,
        montoCentavos: Int,
        moneda:        String = "MXN"
    ): Result<PagoTeleconsulta> {
        val padreUid = auth.currentUser?.uid
            ?: return Result.failure(Exception("No autenticado"))

        return try {
            val id   = UUID.randomUUID().toString()
            val pago = PagoTeleconsulta(
                id            = id,
                padreUid      = padreUid,
                nutriologoUid = nutriologoUid,
                childId       = childId,
                montoCentavos = montoCentavos,
                moneda        = moneda,
                estado        = EstadoPago.PENDIENTE,
                creadaEn      = Timestamp.now()
            )
            col.document(id).set(pago.toMap()).await()
            Result.success(pago)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Confirma el pago con el orderId de PayPal ─────────────────────────────
    suspend fun confirmarPago(pagoId: String, paypalOrderId: String): Result<Unit> {
        return try {
            col.document(pagoId).update(
                mapOf(
                    "paypalOrderId" to paypalOrderId,
                    "estado"        to EstadoPago.COMPLETADO.name,
                    "completadoEn"  to Timestamp.now()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Marca como fallido (usuario canceló o error) ──────────────────────────
    suspend fun cancelarPago(pagoId: String): Result<Unit> {
        return try {
            col.document(pagoId).update("estado", EstadoPago.FALLIDO.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Verifica si el pago más reciente para esta consulta está completado ───
    suspend fun verificarPagoCompletado(pagoId: String): Result<Boolean> {
        return try {
            val doc  = col.document(pagoId).get().await()
            val pago = doc.data?.let { PagoTeleconsulta.fromMap(doc.id, it) }
            Result.success(pago?.estado == EstadoPago.COMPLETADO)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Busca un pago COMPLETADO que aún no haya sido usado (llamadaId vacío) ──
    // Solo considera pagos completados en las últimas 24 horas para evitar
    // huérfanos de sesiones anteriores que nunca se consumieron.
    suspend fun obtenerPagoVigente(nutriologoUid: String, childId: String): Result<PagoTeleconsulta?> {
        val padreUid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
        return try {
            val hace24h = Timestamp(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
            val query = col
                .whereEqualTo("padreUid", padreUid)
                .whereEqualTo("nutriologoUid", nutriologoUid)
                .whereEqualTo("childId", childId)
                .whereEqualTo("estado", EstadoPago.COMPLETADO.name)
                .whereEqualTo("llamadaId", "")
                .whereGreaterThanOrEqualTo("completadoEn", hace24h)
                .orderBy("completadoEn", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val doc = query.documents.firstOrNull()
            val pago = doc?.let { PagoTeleconsulta.fromMap(it.id, it.data ?: emptyMap()) }
            Result.success(pago)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Vincula el pago a una llamada para marcarlo como consumido ────────────
    suspend fun marcarPagoUsado(pagoId: String, llamadaId: String): Result<Unit> {
        return try {
            col.document(pagoId).update("llamadaId", llamadaId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Revierte el consumo del pago (vuelve a estar disponible para reintentar
    // sin cobrar de nuevo), usado cuando la llamada no alcanzó el uso mínimo.
    suspend fun reactivarPago(pagoId: String): Result<Unit> = try {
        col.document(pagoId).update("llamadaId", "").await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

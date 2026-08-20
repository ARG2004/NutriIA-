package com.example.nutriia.firebase.firestore

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import com.example.nutriia.shared.Timestamp

suspend fun <T> T.await(): T = this

typealias Query = CollectionReference

class FirebaseFirestore private constructor() {
    companion object {
        private var _instance: FirebaseFirestore? = null
        fun getInstance(): FirebaseFirestore = _instance ?: FirebaseFirestore().also { _instance = it }
    }

    private val delegate get() = Firebase.firestore

    fun collection(path: String): CollectionReference =
        CollectionReference(delegate.collection(path), path)

    suspend fun clearPersistence(): Unit {
        try {
            delegate.clearPersistence()
        } catch (_: Throwable) {
        }
    }
}

class CollectionReference(
    private val delegate: dev.gitlive.firebase.firestore.CollectionReference,
    val path: String
) {
    fun document(id: String = "doc_${com.example.nutriia.platform.currentTimeMillis()}"): DocumentReference =
        DocumentReference(delegate.document(id), "$path/$id", id)

    fun orderBy(field: String, direction: Direction = Direction.ASCENDING): CollectionReference = this
    fun whereEqualTo(field: String, value: Any?): CollectionReference = this

    val snapshots: Flow<QuerySnapshot>
        get() = delegate.snapshots
            .map { QuerySnapshot(it.documents.map { doc -> DocumentSnapshot(doc.id, doc) }) }
            .catch { emit(QuerySnapshot(emptyList())) }

    fun addSnapshotListener(listener: (QuerySnapshot?, Exception?) -> Unit): ListenerRegistration {
        return ListenerRegistration()
    }

    suspend fun get(vararg args: Any?): QuerySnapshot {
        return try {
            val result = delegate.get()
            QuerySnapshot(result.documents.map { DocumentSnapshot(it.id, it) })
        } catch (_: Throwable) {
            QuerySnapshot(emptyList())
        }
    }
}

class DocumentReference(
    private val delegate: dev.gitlive.firebase.firestore.DocumentReference,
    val path: String,
    val id: String
) {
    suspend fun get(vararg args: Any?): DocumentSnapshot {
        return try {
            val snap = delegate.get()
            DocumentSnapshot(id, snap)
        } catch (_: Throwable) {
            DocumentSnapshot(id)
        }
    }

    suspend fun set(data: Any?): Unit {
        try {
            if (data != null) {
                delegate.set(data)
            }
        } catch (_: Throwable) {
        }
    }

    suspend fun update(data: Map<String, Any?>): Unit {
        try {
            val nonNullData = data.filterValues { it != null }
            if (nonNullData.isNotEmpty()) {
                delegate.update(nonNullData)
            }
        } catch (_: Throwable) {
        }
    }

    suspend fun update(field: String, value: Any?, vararg more: Any?): Unit {
        try {
            val map = mutableMapOf<String, Any?>()
            if (value != null) map[field] = value
            var i = 0
            while (i < more.size - 1) {
                val k = more[i] as? String
                val v = more[i + 1]
                if (k != null && v != null) map[k] = v
                i += 2
            }
            if (map.isNotEmpty()) {
                delegate.update(map)
            }
        } catch (_: Throwable) {
        }
    }

    suspend fun delete(): Unit {
        try {
            delegate.delete()
        } catch (_: Throwable) {
        }
    }

    fun collection(subPath: String): CollectionReference =
        CollectionReference(delegate.collection(subPath), "$path/$subPath")

    val snapshots: Flow<DocumentSnapshot>
        get() = delegate.snapshots
            .map { DocumentSnapshot(id, it) }
            .catch { emit(DocumentSnapshot(id)) }

    fun addSnapshotListener(listener: (DocumentSnapshot?, Exception?) -> Unit): ListenerRegistration {
        return ListenerRegistration()
    }
}

class DocumentSnapshot(
    val id: String = "",
    private val delegate: dev.gitlive.firebase.firestore.DocumentSnapshot? = null,
    val rawData: Map<String, Any?> = emptyMap()
) {
    val exists: Boolean get() = delegate?.exists ?: rawData.isNotEmpty()
    fun exists(): Boolean = exists

    // BUG ORIGINAL: esto siempre regresaba rawData (emptyMap() por defecto), porque
    // ningún constructor de DocumentSnapshot en este archivo pasaba rawData real.
    // Resultado: TODO documento leído de Firestore llegaba con .data = {} sin importar
    // que sí existiera en el servidor — así que fromMap() fallaba en silencio para
    // cada documento y las listas quedaban vacías sin ningún error visible.
    // FIX: extraer los campos reales desde el delegate de gitlive usando su propio
    // decoder genérico (data<Map<String, Any?>>()), con fallback a rawData.
    val data: Map<String, Any?>
        get() = try {
            delegate?.data<Map<String, Any?>>() ?: rawData
        } catch (_: Throwable) {
            rawData
        }

    fun get(field: String): Any? = try {
        runCatching { delegate?.get<String?>(field) }.getOrNull()
            ?: runCatching { delegate?.get<Double?>(field) }.getOrNull()
            ?: runCatching { delegate?.get<Long?>(field) }.getOrNull()
            ?: runCatching { delegate?.get<Boolean?>(field) }.getOrNull()
            ?: rawData[field]
    } catch (_: Throwable) {
        rawData[field]
    }

    fun getString(field: String): String? = try {
        runCatching { delegate?.get<String?>(field) }.getOrNull()
            ?: runCatching { delegate?.get<Double?>(field)?.toString() }.getOrNull()
            ?: runCatching { delegate?.get<Long?>(field)?.toString() }.getOrNull()
            ?: (rawData[field] as? String)
            ?: rawData[field]?.toString()
    } catch (_: Throwable) {
        (rawData[field] as? String) ?: rawData[field]?.toString()
    }

    fun getLong(field: String): Long? = try {
        runCatching { delegate?.get<Long?>(field) }.getOrNull()
            ?: runCatching { delegate?.get<Double?>(field)?.toLong() }.getOrNull()
            ?: runCatching { delegate?.get<String?>(field)?.toLongOrNull() }.getOrNull()
            ?: (rawData[field] as? Number)?.toLong()
            ?: (rawData[field] as? String)?.toLongOrNull()
    } catch (_: Throwable) {
        (rawData[field] as? Number)?.toLong() ?: (rawData[field] as? String)?.toLongOrNull()
    }

    fun getDouble(field: String): Double? = try {
        runCatching { delegate?.get<Double?>(field) }.getOrNull()
            ?: runCatching { delegate?.get<Long?>(field)?.toDouble() }.getOrNull()
            ?: runCatching { delegate?.get<String?>(field)?.toDoubleOrNull() }.getOrNull()
            ?: (rawData[field] as? Number)?.toDouble()
            ?: (rawData[field] as? String)?.toDoubleOrNull()
    } catch (_: Throwable) {
        (rawData[field] as? Number)?.toDouble() ?: (rawData[field] as? String)?.toDoubleOrNull()
    }

    fun getBoolean(field: String): Boolean? = try {
        runCatching { delegate?.get<Boolean?>(field) }.getOrNull()
            ?: runCatching { delegate?.get<String?>(field)?.toBooleanStrictOrNull() }.getOrNull()
            ?: (rawData[field] as? Boolean)
            ?: (rawData[field] as? String)?.toBooleanStrictOrNull()
    } catch (_: Throwable) {
        (rawData[field] as? Boolean) ?: (rawData[field] as? String)?.toBooleanStrictOrNull()
    }

    fun getTimestamp(field: String): Timestamp? = try {
        val gitliveT = runCatching { delegate?.get<dev.gitlive.firebase.firestore.Timestamp?>(field) }.getOrNull()
        if (gitliveT != null) {
            Timestamp(gitliveT.seconds, gitliveT.nanoseconds)
        } else {
            val raw = rawData[field]
            if (raw is Timestamp) raw
            else if (raw is dev.gitlive.firebase.firestore.Timestamp) Timestamp(raw.seconds, raw.nanoseconds)
            else Timestamp.now()
        }
    } catch (_: Throwable) {
        Timestamp.now()
    }

    val reference: DocumentReference get() = DocumentReference(delegate!!.reference, "docs/$id", id)
}

class QuerySnapshot(val documents: List<DocumentSnapshot> = emptyList()) {
    val isEmpty: Boolean get() = documents.isEmpty()
    fun size(): Int = documents.size
    fun toObjects(): List<Map<String, Any?>> = documents.map { it.data }
    fun forEach(action: (DocumentSnapshot) -> Unit) = documents.forEach(action)
    fun map(transform: (DocumentSnapshot) -> Any?): List<Any?> = documents.map(transform)
}

class ListenerRegistration {
    fun remove() {}
}

enum class Direction { ASCENDING, DESCENDING }

object FieldValue {
    val delete: Any = "DELETE_FIELD"
    val serverTimestamp: Any = "SERVER_TIMESTAMP"
    fun delete(): Any = delete
    fun serverTimestamp(): Any = serverTimestamp
}

 
package com.example.nutriia.firebase.firestore

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
        } catch (_: Throwable) {}
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

    val snapshots: Flow<QuerySnapshot> get() = delegate.snapshots.map { QuerySnapshot(it.documents.map { doc -> DocumentSnapshot(doc.id, doc) }) }

    fun addSnapshotListener(listener: (QuerySnapshot?, Exception?) -> Unit): ListenerRegistration {
        return ListenerRegistration()
    }

    suspend fun get(vararg args: Any?): QuerySnapshot {
        val result = delegate.get()
        return QuerySnapshot(result.documents.map { DocumentSnapshot(it.id, it) })
    }
}

class DocumentReference(
    private val delegate: dev.gitlive.firebase.firestore.DocumentReference,
    val path: String,
    val id: String
) {
    suspend fun get(vararg args: Any?): DocumentSnapshot {
        val snap = delegate.get()
        return DocumentSnapshot(id, snap)
    }

    suspend fun set(data: Any?): Unit {
        if (data != null) {
            delegate.set(data)
        }
    }

    suspend fun update(data: Map<String, Any?>): Unit {
        val nonNullData = data.filterValues { it != null }
        if (nonNullData.isNotEmpty()) {
            delegate.update(nonNullData)
        }
    }

    suspend fun update(field: String, value: Any?, vararg more: Any?): Unit {
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
    }

    suspend fun delete(): Unit {
        delegate.delete()
    }

    fun collection(subPath: String): CollectionReference =
        CollectionReference(delegate.collection(subPath), "$path/$subPath")

    val snapshots: Flow<DocumentSnapshot> get() = delegate.snapshots.map { DocumentSnapshot(id, it) }

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

    val data: Map<String, Any?>
        get() = try {
            delegate?.data<Map<String, Any?>>() ?: rawData
        } catch (_: Throwable) {
            rawData
        }

    fun get(field: String): Any? = try {
        delegate?.get<Any?>(field) ?: rawData[field]
    } catch (_: Throwable) {
        rawData[field]
    }

    fun getString(field: String): String? = try {
        delegate?.get<String?>(field) ?: (rawData[field] as? String)
    } catch (_: Throwable) {
        rawData[field] as? String
    }

    fun getLong(field: String): Long? = try {
        delegate?.get<Long?>(field) ?: (rawData[field] as? Number)?.toLong()
    } catch (_: Throwable) {
        (rawData[field] as? Number)?.toLong()
    }

    fun getDouble(field: String): Double? = try {
        delegate?.get<Double?>(field) ?: (rawData[field] as? Number)?.toDouble()
    } catch (_: Throwable) {
        (rawData[field] as? Number)?.toDouble()
    }

    fun getBoolean(field: String): Boolean? = try {
        delegate?.get<Boolean?>(field) ?: (rawData[field] as? Boolean)
    } catch (_: Throwable) {
        rawData[field] as? Boolean
    }

    fun getTimestamp(field: String): Timestamp? = try {
        val t = delegate?.get<Timestamp?>(field)
        t ?: (rawData[field] as? Timestamp) ?: Timestamp.now()
    } catch (_: Throwable) {
        (rawData[field] as? Timestamp) ?: Timestamp.now()
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

data class Timestamp(val seconds: Long = 0L, val nanoseconds: Int = 0) : Comparable<Timestamp> {
    val time: Long get() = seconds * 1000 + (nanoseconds / 1_000_000)
    companion object {
        fun now(): Timestamp = Timestamp(com.example.nutriia.platform.currentTimeMillis() / 1000, 0)
    }
    override operator fun compareTo(other: Timestamp): Int {
        val s = seconds.compareTo(other.seconds)
        return if (s != 0) s else nanoseconds.compareTo(other.nanoseconds)
    }
}

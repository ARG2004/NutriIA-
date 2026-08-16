package com.example.nutriia.firebase.firestore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

suspend fun <T> T.await(): T = this

typealias Query = CollectionReference

class FirebaseFirestore {
    companion object {
        fun getInstance(): FirebaseFirestore = FirebaseFirestore()
    }
    fun collection(path: String): CollectionReference = CollectionReference(path)
    suspend fun clearPersistence(): Unit = Unit
}

class CollectionReference(val path: String) {
    fun document(id: String = "doc_${com.example.nutriia.platform.currentTimeMillis()}"): DocumentReference = DocumentReference("$path/$id", id)
    fun orderBy(field: String, direction: Direction = Direction.ASCENDING): CollectionReference = this
    fun whereEqualTo(field: String, value: Any?): CollectionReference = this
    val snapshots: Flow<QuerySnapshot> get() = flowOf(QuerySnapshot(emptyList()))
    fun addSnapshotListener(listener: (QuerySnapshot?, Exception?) -> Unit): ListenerRegistration {
        listener(QuerySnapshot(emptyList()), null)
        return ListenerRegistration()
    }
    suspend fun get(vararg args: Any?): QuerySnapshot = QuerySnapshot(emptyList())
}

class DocumentReference(val path: String, val id: String) {
    suspend fun get(vararg args: Any?): DocumentSnapshot = DocumentSnapshot(id, emptyMap())
    suspend fun set(data: Any?): Unit = Unit
    suspend fun update(data: Map<String, Any?>): Unit = Unit
    suspend fun update(field: String, value: Any?, vararg more: Any?): Unit = Unit
    suspend fun delete(): Unit = Unit
    fun collection(subPath: String): CollectionReference = CollectionReference("$path/$subPath")
    val snapshots: Flow<DocumentSnapshot> get() = flowOf(DocumentSnapshot(id, emptyMap()))
    fun addSnapshotListener(listener: (DocumentSnapshot?, Exception?) -> Unit): ListenerRegistration {
        listener(DocumentSnapshot(id, emptyMap()), null)
        return ListenerRegistration()
    }
}

class DocumentSnapshot(val id: String = "", val rawData: Map<String, Any?> = emptyMap()) {
    val exists: Boolean get() = true
    fun exists(): Boolean = true
    val data: Map<String, Any?> get() = rawData
    fun get(field: String): Any? = rawData[field]
    fun getString(field: String): String? = rawData[field] as? String
    fun getLong(field: String): Long? = (rawData[field] as? Number)?.toLong()
    fun getDouble(field: String): Double? = (rawData[field] as? Number)?.toDouble()
    fun getBoolean(field: String): Boolean? = rawData[field] as? Boolean
    fun getTimestamp(field: String): Timestamp? = (rawData[field] as? Timestamp) ?: Timestamp.now()
    val reference: DocumentReference get() = DocumentReference("docs/$id", id)
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

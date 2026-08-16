package com.google.firebase.firestore

enum class Source { DEFAULT, SERVER, CACHE }

class ListenerRegistration {
    fun remove() {}
}

class DocumentSnapshot(val id: String = "doc_id", val data: Map<String, Any?>? = emptyMap()) {
    val reference: DocumentReference get() = DocumentReference(id)
    fun exists(): Boolean = true
    fun getString(field: String): String? = data?.get(field) as? String
    fun getLong(field: String): Long? = (data?.get(field) as? Number)?.toLong()
    fun getDouble(field: String): Double? = (data?.get(field) as? Number)?.toDouble()
    fun getBoolean(field: String): Boolean? = data?.get(field) as? Boolean
    fun getTimestamp(field: String): com.example.nutriia.shared.Timestamp? = data?.get(field) as? com.example.nutriia.shared.Timestamp
    fun get(field: String): Any? = data?.get(field)
}

class QuerySnapshot(val documents: List<DocumentSnapshot> = emptyList()) {
    val isEmpty: Boolean get() = documents.isEmpty()
    fun size(): Int = documents.size
}

open class Query {
    enum class Direction { ASCENDING, DESCENDING }

    open fun whereEqualTo(field: String, value: Any?): Query = this
    open fun whereIn(field: String, values: List<Any?>): Query = this
    open fun whereArrayContains(field: String, value: Any?): Query = this
    open fun orderBy(field: String, direction: Direction = Direction.ASCENDING): Query = this
    open fun limit(limit: Long): Query = this
    open fun startAt(vararg values: Any): Query = this
    open fun endAt(vararg values: Any): Query = this

    open suspend fun get(source: Source = Source.DEFAULT): QuerySnapshot = QuerySnapshot()
    open fun addSnapshotListener(listener: (QuerySnapshot?, Throwable?) -> Unit): ListenerRegistration {
        listener(QuerySnapshot(), null)
        return ListenerRegistration()
    }
}

class DocumentReference(val id: String = "doc_id") {
    suspend fun get(source: Source = Source.DEFAULT): DocumentSnapshot = DocumentSnapshot(id)
    suspend fun set(data: Any?, options: Any? = null) {}
    suspend fun update(data: Map<String, Any?>) {}
    suspend fun update(field: String, value: Any?, vararg moreFieldsAndValues: Any?) {}
    suspend fun delete() {}
    fun collection(path: String): CollectionReference = CollectionReference()
    fun addSnapshotListener(listener: (DocumentSnapshot?, Throwable?) -> Unit): ListenerRegistration {
        listener(DocumentSnapshot(id), null)
        return ListenerRegistration()
    }
}

class CollectionReference : Query() {
    fun document(path: String = "doc_id"): DocumentReference = DocumentReference(path)
    suspend fun add(data: Any?): DocumentReference = DocumentReference()
}

class FirebaseFirestore {
    fun collection(path: String): CollectionReference = CollectionReference()
    fun document(path: String): DocumentReference = DocumentReference(path)
    suspend fun clearPersistence() {}

    companion object {
        private val instance = FirebaseFirestore()
        fun getInstance(): FirebaseFirestore = instance
    }
}

object FieldValue {
    fun serverTimestamp(): Any = Any()
    fun delete(): Any = Any()
    fun arrayUnion(vararg elements: Any): Any = Any()
    fun arrayRemove(vararg elements: Any): Any = Any()
    fun increment(value: Long): Any = Any()
}

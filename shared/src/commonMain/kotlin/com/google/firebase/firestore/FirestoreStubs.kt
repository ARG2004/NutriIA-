package com.google.firebase.firestore

import com.google.android.gms.tasks.Task
import java.util.Date

class Timestamp(val seconds: Long = 0L, val nanoseconds: Int = 0) : Comparable<Timestamp> {
    fun toDate(): Date = Date(seconds * 1000)
    override fun compareTo(other: Timestamp): Int {
        val s = seconds.compareTo(other.seconds)
        return if (s != 0) s else nanoseconds.compareTo(other.nanoseconds)
    }
    companion object {
        fun now(): Timestamp = Timestamp(0L, 0)
    }
}

enum class MetadataChanges { EXCLUDE, INCLUDE }

open class DocumentSnapshot(
    val id: String = "doc_id",
    val data: Map<String, Any?>? = emptyMap()
) {
    val reference: DocumentReference get() = DocumentReference(id)
    fun exists(): Boolean = true
    fun getData(): Map<String, Any?>? = data
    fun data(): Map<String, Any?>? = data
    fun get(field: String): Any? = data?.get(field)
    fun getString(field: String): String? = data?.get(field) as? String
    fun getLong(field: String): Long? = (data?.get(field) as? Number)?.toLong()
    fun getDouble(field: String): Double? = (data?.get(field) as? Number)?.toDouble()
    fun getBoolean(field: String): Boolean? = data?.get(field) as? Boolean
    fun getTimestamp(field: String): Timestamp? = data?.get(field) as? Timestamp
    inline fun <reified T> toObject(clazz: Any? = null): T? = null
}

class QueryDocumentSnapshot(
    id: String = "query_doc_id",
    data: Map<String, Any?>? = emptyMap()
) : DocumentSnapshot(id, data)

class QuerySnapshot(
    val documents: List<DocumentSnapshot> = emptyList()
) : Iterable<DocumentSnapshot> {
    override fun iterator(): Iterator<DocumentSnapshot> = documents.iterator()
    val isEmpty: Boolean get() = documents.isEmpty()
    val isNotEmpty: Boolean get() = documents.isNotEmpty()
    fun isEmpty(): Boolean = documents.isEmpty()
    fun isNotEmpty(): Boolean = documents.isNotEmpty()
    fun size(): Int = documents.size
}

interface ListenerRegistration {
    fun remove()
}

enum class Source {
    DEFAULT, SERVER, CACHE
}

open class Query {
    open fun whereEqualTo(field: String, value: Any?): Query = this
    open fun whereNotEqualTo(field: String, value: Any?): Query = this
    open fun whereGreaterThan(field: String, value: Any?): Query = this
    open fun whereGreaterThanOrEqualTo(field: String, value: Any?): Query = this
    open fun whereLessThan(field: String, value: Any?): Query = this
    open fun whereLessThanOrEqualTo(field: String, value: Any?): Query = this
    open fun whereIn(field: String, values: List<Any?>): Query = this
    open fun whereNotIn(field: String, values: List<Any?>): Query = this
    open fun whereArrayContains(field: String, value: Any?): Query = this
    open fun whereArrayContainsAny(field: String, values: List<Any?>): Query = this
    open fun orderBy(field: String): Query = this
    open fun orderBy(field: String, direction: Query.Direction): Query = this
    open fun startAt(vararg values: Any?): Query = this
    open fun startAfter(vararg values: Any?): Query = this
    open fun endAt(vararg values: Any?): Query = this
    open fun endBefore(vararg values: Any?): Query = this
    open fun limit(limit: Long): Query = this
    open fun get(source: Source = Source.DEFAULT): Task<QuerySnapshot> = Task()
    open fun addSnapshotListener(listener: (QuerySnapshot?, Exception?) -> Unit): ListenerRegistration {
        listener(QuerySnapshot(), null)
        return object : ListenerRegistration { override fun remove() {} }
    }
    open fun addSnapshotListener(metadataChanges: MetadataChanges, listener: (QuerySnapshot?, Exception?) -> Unit): ListenerRegistration {
        listener(QuerySnapshot(), null)
        return object : ListenerRegistration { override fun remove() {} }
    }

    enum class Direction { ASCENDING, DESCENDING }
}

class CollectionReference(val path: String = "") : Query() {
    fun document(path: String = ""): DocumentReference = DocumentReference(path)
    fun add(data: Any): Task<DocumentReference> = Task()
}

class DocumentReference(val id: String = "doc_ref") {
    val reference: DocumentReference get() = this
    fun get(source: Source = Source.DEFAULT): Task<DocumentSnapshot> = Task()
    fun set(data: Any, options: SetOptions? = null): Task<Unit> = Task()
    fun update(data: Map<String, Any?>): Task<Unit> = Task()
    fun update(field: String, value: Any?, vararg moreFieldsAndValues: Any?): Task<Unit> = Task()
    fun delete(): Task<Unit> = Task()
    fun collection(collectionPath: String): CollectionReference = CollectionReference(collectionPath)
    fun addSnapshotListener(listener: (DocumentSnapshot?, Exception?) -> Unit): ListenerRegistration {
        listener(DocumentSnapshot(id), null)
        return object : ListenerRegistration { override fun remove() {} }
    }
    fun addSnapshotListener(metadataChanges: MetadataChanges, listener: (DocumentSnapshot?, Exception?) -> Unit): ListenerRegistration {
        listener(DocumentSnapshot(id), null)
        return object : ListenerRegistration { override fun remove() {} }
    }
}

class SetOptions {
    companion object {
        fun merge(): SetOptions = SetOptions()
    }
}

class FieldValue {
    companion object {
        fun serverTimestamp(): Any = Timestamp.now()
        fun arrayUnion(vararg elements: Any?): Any = elements.toList()
        fun arrayRemove(vararg elements: Any?): Any = elements.toList()
        fun delete(): Any = ""
        fun increment(value: Long): Any = value
        fun increment(value: Double): Any = value
    }
}

class FirebaseFirestore private constructor() {
    fun collection(collectionPath: String): CollectionReference = CollectionReference(collectionPath)
    fun document(documentPath: String): DocumentReference = DocumentReference(documentPath)
    fun clearPersistence(): Task<Unit> = Task()

    companion object {
        private val instance = FirebaseFirestore()
        fun getInstance(): FirebaseFirestore = instance
    }
}

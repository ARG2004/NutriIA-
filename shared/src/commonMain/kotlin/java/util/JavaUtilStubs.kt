package java.util

class UUID private constructor(private val value: String) {
    override fun toString(): String = value

    companion object {
        private var counter = 1000L
        fun randomUUID(): UUID {
            val ts = kotlin.random.Random.nextLong(100000000000L, 999999999999L)
            val rand = kotlin.random.Random.nextInt(1000, 9999)
            return UUID("uuid-$ts-$rand-${counter++}")
        }
        fun fromString(name: String): UUID = UUID(name)
    }
}

open class Date(var time: Long = 0L) : Comparable<Date> {
    constructor() : this(0L)
    override fun compareTo(other: Date): Int = time.compareTo(other.time)
    override fun toString(): String = "Date($time)"
}

class Locale(val language: String, val country: String = "") {
    companion object {
        val ENGLISH = Locale("en", "US")
        val ROOT = Locale("", "")
        private var defaultLocale = Locale("es", "MX")
        fun getDefault(): Locale = defaultLocale
        fun setDefault(loc: Locale) { defaultLocale = loc }
    }
}

class TimeZone private constructor(val id: String) {
    companion object {
        fun getDefault(): TimeZone = TimeZone("UTC")
        fun getTimeZone(id: String): TimeZone = TimeZone(id)
    }
}

class Calendar private constructor() {
    private val fields = mutableMapOf<Int, Int>()
    var timeInMillis: Long = 0L
    var time: Date
        get() = Date(timeInMillis)
        set(value) { timeInMillis = value.time }

    fun set(field: Int, value: Int) { fields[field] = value }
    fun set(year: Int, month: Int, date: Int) {
        fields[YEAR] = year
        fields[MONTH] = month
        fields[DAY_OF_MONTH] = date
    }
    fun set(year: Int, month: Int, date: Int, hourOfDay: Int, minute: Int, second: Int) {
        fields[YEAR] = year
        fields[MONTH] = month
        fields[DAY_OF_MONTH] = date
        fields[HOUR_OF_DAY] = hourOfDay
        fields[MINUTE] = minute
        fields[SECOND] = second
    }
    fun get(field: Int): Int = fields[field] ?: when (field) {
        YEAR -> 2026
        MONTH -> 7
        DAY_OF_MONTH -> 15
        HOUR_OF_DAY -> 12
        MINUTE -> 0
        SECOND -> 0
        else -> 0
    }
    fun add(field: Int, amount: Int) {
        val cur = get(field)
        fields[field] = cur + amount
    }

    companion object {
        const val YEAR = 1
        const val MONTH = 2
        const val DAY_OF_MONTH = 5
        const val HOUR_OF_DAY = 11
        const val MINUTE = 12
        const val SECOND = 13

        fun getInstance(): Calendar = Calendar()
        fun getInstance(timeZone: TimeZone): Calendar = Calendar()
        fun getInstance(locale: Locale): Calendar = Calendar()
    }
}

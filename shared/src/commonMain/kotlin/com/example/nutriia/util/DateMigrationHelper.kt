package com.example.nutriia.util

object DateMigrationHelper {
    private val regexYyyyMmDd = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val regexDdMmYyyy = Regex("^\\d{2}/\\d{2}/\\d{4}$")

    fun convertYyyyMmDdToDdMmYyyy(dateStr: String): String {
        if (!dateStr.matches(regexYyyyMmDd)) return dateStr
        val parts = dateStr.split("-")
        if (parts.size != 3) return dateStr
        return "${parts[2]}/${parts[1]}/${parts[0]}"
    }

    fun convertDdMmYyyyToYyyyMmDd(dateStr: String): String {
        if (!dateStr.matches(regexDdMmYyyy)) return dateStr
        val parts = dateStr.split("/")
        if (parts.size != 3) return dateStr
        return "${parts[2]}-${parts[1]}-${parts[0]}"
    }
}

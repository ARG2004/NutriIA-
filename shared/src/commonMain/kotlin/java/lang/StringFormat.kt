package java.lang

import java.util.Locale
import kotlin.math.round
import kotlin.math.abs

fun String.Companion.format(locale: Locale, format: String, vararg args: Any?): String {
    var result = format
    var argIndex = 0
    val regex = Regex("%(\\+)?(0\\d+)?(\\.\\d+)?([dfes])")
    
    while (argIndex < args.size) {
        val match = regex.find(result) ?: break
        val arg = args[argIndex++]
        val plusSign = match.groupValues[1] == "+"
        val zeroPad = match.groupValues[2]
        val precision = match.groupValues[3]
        val specifier = match.groupValues[4]

        val formatted = when (specifier) {
            "d" -> {
                val num = (arg as? Number)?.toLong() ?: 0L
                val width = zeroPad?.removePrefix("0")?.toIntOrNull() ?: 0
                val sign = if (plusSign && num > 0) "+" else if (num < 0) "-" else ""
                val absStr = abs(num).toString()
                val padded = if (absStr.length < width) "0".repeat(width - absStr.length) + absStr else absStr
                "$sign$padded"
            }
            "f" -> {
                val num = (arg as? Number)?.toDouble() ?: 0.0
                val decs = precision?.removePrefix(".")?.toIntOrNull() ?: 1
                val factor = when (decs) {
                    0 -> 1.0
                    1 -> 10.0
                    2 -> 100.0
                    3 -> 1000.0
                    4 -> 10000.0
                    else -> 100000.0
                }
                val isNegative = num < 0
                val absNum = abs(num)
                val rounded = round(absNum * factor) / factor
                val parts = rounded.toString().split(".")
                val intPart = parts[0]
                var decPart = if (parts.size > 1) parts[1] else ""
                while (decPart.length < decs) decPart += "0"
                if (decs > 0 && decPart.length > decs) decPart = decPart.substring(0, decs)
                val sign = if (isNegative) "-" else if (plusSign && num > 0) "+" else ""
                val body = if (decs > 0) "$intPart.$decPart" else intPart
                "$sign$body"
            }
            else -> arg?.toString() ?: "null"
        }
        result = result.replaceFirst(match.value, formatted)
    }
    return result
}

fun String.Companion.format(format: String, vararg args: Any?): String =
    String.format(Locale.US, format, *args)

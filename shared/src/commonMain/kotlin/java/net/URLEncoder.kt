package java.net

object URLEncoder {
    fun encode(s: String, enc: String = "UTF-8"): String {
        return s.replace(" ", "%20")
            .replace("@", "%40")
            .replace(":", "%3A")
            .replace("/", "%2F")
            .replace("?", "%3F")
            .replace("#", "%23")
            .replace("&", "%26")
            .replace("=", "%3D")
            .replace("+", "%2B")
    }
}

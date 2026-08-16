package android.net

class Uri(val path: String = "") {
    override fun toString(): String = path

    companion object {
        fun parse(uriString: String): Uri = Uri(uriString)
        fun fromParts(scheme: String, ssp: String, fragment: String?): Uri = Uri("$scheme:$ssp")
    }
}

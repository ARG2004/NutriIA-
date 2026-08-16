package android.view

open class ViewGroup {
    open class LayoutParams(val width: Int = 0, val height: Int = 0) {
        companion object {
            const val MATCH_PARENT = -1
            const val WRAP_CONTENT = -2
        }
    }
}

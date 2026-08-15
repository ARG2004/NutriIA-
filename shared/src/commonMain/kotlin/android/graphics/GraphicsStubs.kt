package android.graphics

import java.io.InputStream

class Matrix {
    fun postRotate(degrees: Float) {}
}

class Bitmap {
    val width: Int = 100
    val height: Int = 100

    enum class CompressFormat { JPEG, PNG, WEBP }

    fun compress(format: CompressFormat, quality: Int, stream: Any): Boolean = true

    companion object {
        fun createBitmap(source: Bitmap, x: Int, y: Int, width: Int, height: Int, m: Matrix?, filter: Boolean): Bitmap = Bitmap()
    }
}

object BitmapFactory {
    fun decodeByteArray(data: ByteArray, offset: Int, length: Int): Bitmap? = Bitmap()
    fun decodeStream(isStream: InputStream?): Bitmap? = Bitmap()
    fun decodeFile(pathName: String): Bitmap? = Bitmap()
}

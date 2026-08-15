package android.graphics

import java.io.ByteArrayOutputStream
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

package android.net

class Network
class NetworkCapabilities {
    fun hasCapability(capability: Int): Boolean = true
    companion object {
        const val NET_CAPABILITY_INTERNET = 12
    }
}

class NetworkRequest {
    class Builder {
        fun addCapability(capability: Int): Builder = this
        fun build(): NetworkRequest = NetworkRequest()
    }
}

class ConnectivityManager {
    open class NetworkCallback {
        open fun onAvailable(network: Network) {}
        open fun onLost(network: Network) {}
    }

    fun registerNetworkCallback(request: NetworkRequest, networkCallback: NetworkCallback) {}
    fun unregisterNetworkCallback(networkCallback: NetworkCallback) {}
    fun getActiveNetwork(): Network? = Network()
    fun getNetworkCapabilities(network: Network?): NetworkCapabilities? = NetworkCapabilities()
}

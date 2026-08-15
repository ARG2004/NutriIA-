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

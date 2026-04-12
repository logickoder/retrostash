package dev.logickoder.retrostash.example

enum class TransportMode {
    OKHTTP,
    KTOR,
}

class TransportModeState(
    initial: TransportMode = TransportMode.OKHTTP,
) {
    var mode: TransportMode = initial
        private set

    fun toggle(): TransportMode {
        mode = if (mode == TransportMode.OKHTTP) {
            TransportMode.KTOR
        } else {
            TransportMode.OKHTTP
        }
        return mode
    }
}

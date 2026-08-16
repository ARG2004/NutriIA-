package org.webrtc.audio

class JavaAudioDeviceModule private constructor() {
    companion object {
        fun builder(context: Any?): Builder = Builder()
    }

    class Builder {
        fun setUseStereoInput(useStereoInput: Boolean): Builder = this
        fun setUseStereoOutput(useStereoOutput: Boolean): Builder = this
        fun setUseHardwareAcousticEchoCanceler(enable: Boolean): Builder = this
        fun setUseHardwareNoiseSuppressor(enable: Boolean): Builder = this
        fun createAudioDeviceModule(): JavaAudioDeviceModule = JavaAudioDeviceModule()
    }
}

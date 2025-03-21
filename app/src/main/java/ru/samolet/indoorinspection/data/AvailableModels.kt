package ru.samolet.indoorinspection.data

sealed interface AvailableModels {
    object MobileNetV3 : AvailableModels
    object EfficientNet : AvailableModels
    object SqueezeNet : AvailableModels
    object MnasNet : AvailableModels

    companion object {
        fun getModelByIndex(index: Int): AvailableModels {
            return when (index) {
                0 -> MobileNetV3
                1 -> EfficientNet
                2 -> SqueezeNet
                3 -> MnasNet
                else -> EfficientNet
            }
        }
    }
}
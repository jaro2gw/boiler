package boiler.parameter

import kotlin.math.roundToInt

data class Parameter(
        val name: String,
        val unit: String,
        val minValue: Int,
        val maxValue: Int,
        val scale: Double = 1.0,
        val normalizationCoefficient: Double = 1.0,
        private var curValue: Int = (minValue + maxValue) / 2
) {
    fun reachedMin() = curValue == minValue
    fun reachedMax() = curValue == maxValue

    fun inc() {
        if (curValue < maxValue) ++curValue
    }

    fun dec() {
        if (curValue > minValue) --curValue
    }

    fun toDouble() = curValue * scale
    fun normalized() = toDouble() * normalizationCoefficient
}

fun Double.twoDecimalPlaces() = this.times(100).roundToInt().div(100.0)
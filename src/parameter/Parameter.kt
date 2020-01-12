package parameter

import kotlin.math.roundToInt

data class Parameter(
        val name: String,
        val unit: String,
        val minValue: Int,
        val maxValue: Int,
        val scale: Double = 1.0,
        var curValue: Int = (minValue + maxValue) / 2
) {
    fun toDouble() = curValue * scale
}

fun Double.twoDecimalPlaces() = this.times(100).roundToInt().div(100.0)
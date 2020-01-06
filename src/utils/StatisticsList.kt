package utils

class StatisticsList(capacity: Int) : LimitedList<Double>(capacity) {
    internal var sum: Double = 0.0
    /*private val average: Double
        get() = if (size > 0) sum / size else 0.0*/

    override fun append(t: Double) {
        super.append(t)
        sum += t
    }

    override fun removeItem(t: Double) {
        sum -= t
    }
}
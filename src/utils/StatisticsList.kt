package utils

class StatisticsList(capacity: Int = 60) : LimitedList<Double>(capacity) {
    internal var sum: Double = 0.0
    /*private val average: Double
        get() = if (size > 0) sum / size else 0.0*/

    override fun onItemAppended(t: Double) {
        sum += t
    }

    override fun onItemRemoved(t: Double) {
        sum -= t
    }
}
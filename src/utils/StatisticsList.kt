package utils

class StatisticsList(capacity: Int = 60) : LimitedList<Double>(capacity) {
    internal var sum: Double = 0.0

    override fun onItemAppended(t: Double) {
        sum += t
    }

    override fun onItemRemoved(t: Double) {
        sum -= t
    }

    override fun clear() {
        super.clear()
        sum = 0.0
    }
}
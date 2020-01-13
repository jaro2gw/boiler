package utils

open class LimitedList<T>(private var capacity: Int) : Iterable<T> {
    init {
        require(capacity > 0)
    }

    private var size: Int = 0

    open fun onItemRemoved(t: T) {}

    open fun onItemAppended(t: T) {}

    private class Node<T>(val t: T, var next: Node<T>? = null)

    // head is the oldest node
    private var head: Node<T>? = null
    // tail is the newest node
    private var tail: Node<T>? = null

    // returns removed element (the oldest one) if the size were to exceed capacity
    fun append(t: T) {
        val node = Node(t)
        if (size == 0) head = node
        else tail?.next = node
        tail = node

        onItemAppended(t)

        if (size == capacity) {
            val item = head?.t
            head = head?.next
            item?.let { onItemRemoved(it) }
        } else ++size
    }

    open fun clear() {
        head = null
        tail = null
        size = 0
    }

    override fun iterator(): Iterator<T> = iterator {
        var node = head
        while (node?.next != null) {
            yield(node.t)
            node = node.next ?: throw ConcurrentModificationException()
        }
    }
}
package utils

abstract class LimitedList<T>(private var capacity: Int) : Iterable<T> {
    init {
        require(capacity > 0)
    }

    private var size: Int = 0

    abstract fun removeItem(t: T)

//    fun getSize() = size

    private class Node<T>(val t: T, var next: Node<T>? = null)

    // head is the oldest node
    private var head: Node<T>? = null
    // tail is the newest node
    private var tail: Node<T>? = null

    // returns removed element (the oldest one) if the size were to exceed capacity
    open fun append(t: T) {
        val node = Node(t)
        if (size == 0) head = node
        else tail?.next = node
        tail = node

        if (size == capacity) {
            head?.t?.let { removeItem(it) }
            head = head?.next
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
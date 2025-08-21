package pt.isel.pc.problemsets.set2

import java.util.concurrent.atomic.AtomicInteger

class UnsafeSuccession<T>(
    private val items: Array<T>
) {
    private var index = AtomicInteger()
    fun next(): T? {
        while (true) {
            val observedValue = index.get()
            val nextValue = if (observedValue < items.size) {
                observedValue + 1
            } else {
                return null
            }

            if (index.compareAndSet(observedValue, nextValue)) {
                return items[observedValue]
            }

        }
    }

}
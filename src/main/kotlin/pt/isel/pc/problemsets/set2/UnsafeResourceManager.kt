package pt.isel.pc.problemsets.set2

import java.util.concurrent.atomic.AtomicInteger

class UnsafeResourceManager(private val obj: AutoCloseable, usages: Int) {
    private var currentUsages = AtomicInteger(usages)

    fun release() {
        while (true) {
            val observedValue = currentUsages.get()
            val nextValue = if (observedValue > 0) {
                observedValue - 1
            } else {
                throw IllegalStateException("Usage count is already zero")
            }
            if (currentUsages.compareAndSet(observedValue, nextValue)) {
                if (nextValue == 0) {
                    return obj.close()
                } else break
            }
        }
    }
}


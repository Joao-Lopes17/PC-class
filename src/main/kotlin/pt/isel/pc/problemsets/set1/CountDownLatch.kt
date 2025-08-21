package pt.isel.pc.problemsets.set1

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class CountDownLatch(initialCount: Int) {
    init { require(initialCount >= 0) { "Initial counter must be non-negative" } }

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var count = initialCount

    fun countDown() = lock.withLock {
        if (count - 1 < 0) return
        if (--count == 0) condition.signalAll()
    }

    fun await() = lock.withLock {
        // fast-path
        if (count == 0) return

        // wait-path
        while (true) {
            condition.await()
            if (count == 0) return
        }
    }

    fun await(timeout: Long, unit: TimeUnit): Boolean {
        lock.withLock {
            // fast-path
            if (count == 0) return true

            // wait-path
            var timeoutInNanos = unit.toNanos(timeout)
            while (true) {
                timeoutInNanos = condition.awaitNanos(timeoutInNanos)

                if (count == 0) return true
                if (timeoutInNanos <= 0) return false
            }
        }
    }

    fun getCount() = lock.withLock { count }

    override fun toString() = lock.withLock { "Count = $count" }
}

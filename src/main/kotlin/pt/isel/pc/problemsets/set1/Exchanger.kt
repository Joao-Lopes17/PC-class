package pt.isel.pc.problemsets.set1

import pt.isel.pc.problemsets.utils.NodeLinkedList
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class Exchanger<T> {

    data class Request<T>(var t: T, var exchange: Boolean)

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val requesters = NodeLinkedList<Request<T>>()

    fun exchange(t: T): T {
        lock.withLock {
            val headRequest = requesters.headNode

            //fast-path
            if (headRequest != null) return headRequest.value.t
                .also {
                    headRequest.value.exchange = true
                    headRequest.value.t = t
                    requesters.remove(headRequest)
                    condition.signal()
                }

            //wait-path
            val selfNode = requesters.enqueue(Request(t, false))
            while (true) {
                try { condition.await() }
                catch (ex: InterruptedException) {
                    if (selfNode.value.exchange) {
                        Thread.currentThread().interrupt()
                        return selfNode.value.t
                    }
                    throw ex
                }
                if (selfNode.value.exchange) return selfNode.value.t
            }
        }
    }

    fun exchange(t: T, timeout: Long, timeoutUnits: TimeUnit): T {
        lock.withLock {
            val headRequest = requesters.headNode

            //fast-path
            if (headRequest != null) return headRequest.value.t
                .also {
                    headRequest.value.exchange = true
                    headRequest.value.t = t
                    requesters.remove(headRequest)
                    condition.signal()
                }

            //wait-path
            val selfNode = requesters.enqueue(Request(t, false))
            var timeoutInNanos = timeoutUnits.toNanos(timeout)
            while (true) {
                try { timeoutInNanos = condition.awaitNanos(timeoutInNanos) }
                catch (ex: InterruptedException) {
                    if (selfNode.value.exchange) {
                        Thread.currentThread().interrupt()
                        return selfNode.value.t
                    }
                    throw ex
                }
                if (selfNode.value.exchange) return selfNode.value.t

                if (timeoutInNanos <= 0) {
                    if (selfNode.value.exchange) return selfNode.value.t
                    throw TimeoutException()
                }
            }
        }
    }
}

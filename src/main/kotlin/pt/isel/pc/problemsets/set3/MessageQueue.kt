package pt.isel.pc.problemsets.set3

import pt.isel.pc.problemsets.utils.NodeLinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MessageQueue<T> {
    private val lock = ReentrantLock()

    data class Request<T>(
        val continuation: CancellableContinuation<T>,
        var message: T?,
        var isDone: Boolean
    )

    private val messageList = NodeLinkedList<T>()
    private val requestList = NodeLinkedList<Request<T>>()
    fun enqueue(message: T) {
        val continuation: CancellableContinuation<T>? = lock.withLock {
            if (requestList.notEmpty) {
                val request = requestList.pull()
                request.value.message = message
                request.value.isDone = true
                request.value.continuation
            } else {
                messageList.enqueue(message)
                null
            }
        }
        continuation?.resume(message)
    }

    suspend fun dequeue(): T {
        var m: T? = null
        var requestNode: NodeLinkedList.Node<Request<T>>? = null
        try {
            return suspendCancellableCoroutine { continuation ->
                lock.withLock {
                    if (messageList.notEmpty) {
                        // fast-path
                        m = messageList.pull().value
                        continuation.resume(m!!)
                    } else {
                        // wait-path
                        requestNode = requestList.enqueue(
                            Request(continuation, null, false),
                        )
                    }
                }
            }
        } catch (ex: CancellationException) {
            if (m != null) {
                return m!!
            }
            val observedNode = requestNode ?: throw ex
            lock.withLock {
                if (observedNode.value.isDone) {
                    return observedNode.value.message!!
                }
                requestList.remove(observedNode)
                throw ex
            }
        }
    }
}
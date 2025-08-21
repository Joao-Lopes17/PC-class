package pt.isel.pc.problemsets.set1

import pt.isel.pc.problemsets.utils.NodeLinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class BlockingMessageQueue<T>(private val capacity: Int) {
    init { require(capacity > 0) { "Capacity must not be negative" } }

    private val lock = ReentrantLock()

    data class InsertRequest<T>(val condition: Condition, val messageList: List<T>, var isDone: Boolean = false)
    data class RemoveRequest<T>(val condition: Condition, var message: T? = null, var isDone: Boolean = false)

    private val insertRequesters = NodeLinkedList<InsertRequest<T>>()
    private val removeRequesters = NodeLinkedList<RemoveRequest<T>>()
    private val messagesList = NodeLinkedList<T>()

    @Throws(InterruptedException::class)
    fun tryEnqueue(messages: List<T>, timeout: Duration): Boolean {
        lock.withLock {

            //fast-path
            if ((capacity + removeRequesters.count) >= (messagesList.count + messages.size) && insertRequesters.empty) {
                var x = 0

                while (removeRequesters.notEmpty) {
                    val headNode = removeRequesters.headNode!!
                    headNode.value.message = messages[x]
                    headNode.value.isDone = true
                    removeRequesters.remove(headNode)
                    headNode.value.condition.signal()
                    x++
                }

                while (messages.size != x)
                    messagesList.enqueue(messages[x++])

                return true
            }

            //wait-path
            var timeoutInNanos = timeout.inWholeNanoseconds
            val selfCondition = lock.newCondition()
            val selfNode = insertRequesters.enqueue(InsertRequest(selfCondition, messages, false))
            while (true) {
                try { timeoutInNanos = selfCondition.awaitNanos(timeoutInNanos) }
                catch (ex: InterruptedException) {
                    if (selfNode.value.isDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    insertRequesters.remove(selfNode)
                    signalRequestersHeadNode()
                    throw ex
                }

                if (selfNode.value.isDone) return true

                if (timeoutInNanos <= 0) {
                    //?
                    if (selfNode.value.isDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    //?
                    insertRequesters.remove(selfNode)
                    signalRequestersHeadNode()
                    return false
                }
            }
        }
    }

    @Throws(InterruptedException::class)
    fun tryDequeue(timeout: Duration): T? {
        lock.withLock {

            //fast-path
            if (messagesList.notEmpty) {
                val message = messagesList.headNode!!.value
                messagesList.pull()

                if (
                    insertRequesters.notEmpty &&
                    insertRequesters.headNode!!.value.messageList.size <= (capacity - messagesList.count + removeRequesters.count)
                ) {
                    var x = 0
                    val messages = insertRequesters.headNode!!.value.messageList
                    while (removeRequesters.notEmpty) {
                        val headNode = removeRequesters.headNode!!
                        headNode.value.message = messages[x]
                        headNode.value.isDone = true
                        removeRequesters.remove(headNode)
                        headNode.value.condition.signal()
                        x++
                    }
                    while (messages.size != x)
                        messagesList.enqueue(messages[x++])

                    val insertRequestersHeadNode = insertRequesters.headNode!!
                    insertRequestersHeadNode.value.isDone = true
                    insertRequesters.pull()
                    insertRequestersHeadNode.value.condition.signal()
                }
                return message
            }

            //wait-path
            var timeoutInNanos = timeout.inWholeNanoseconds
            val selfCondition = lock.newCondition()
            val selfNode = removeRequesters.enqueue(RemoveRequest(selfCondition))
            while (true) {
                try { timeoutInNanos = selfCondition.awaitNanos(timeoutInNanos) }
                catch (ex: InterruptedException) {
                    if (selfNode.value.isDone) {
                        Thread.currentThread().interrupt()
                        return selfNode.value.message
                    }
                    removeRequesters.remove(selfNode)
                    throw ex
                }

                if (selfNode.value.isDone) return selfNode.value.message

                if (timeoutInNanos <= 0) {
                    if (selfNode.value.isDone) {
                        Thread.currentThread().interrupt()
                        return selfNode.value.message
                    }
                    removeRequesters.remove(selfNode)
                    return null
                }
            }
        }
    }

    private fun signalRequestersHeadNode() {
        if (insertRequesters.notEmpty) {
            val requesterHeadNode = insertRequesters.headNode!!
            if ((requesterHeadNode.value.messageList.size + messagesList.count) <= (capacity + removeRequesters.count)) {
                var x = 0
                val messages = requesterHeadNode.value.messageList

                while (removeRequesters.notEmpty) {
                    val headNode = removeRequesters.headNode!!
                    headNode.value.message = messages[x]
                    headNode.value.isDone = true
                    removeRequesters.remove(headNode)
                    headNode.value.condition.signal()
                    x++
                }

                while (messages.size != x)
                    messagesList.enqueue(messages[x++])

                requesterHeadNode.value.isDone = true
                insertRequesters.remove(requesterHeadNode)
                requesterHeadNode.value.condition.signal()
            }
        }
    }
}
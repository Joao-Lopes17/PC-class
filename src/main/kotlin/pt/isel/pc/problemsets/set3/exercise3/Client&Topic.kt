package pt.isel.pc.problemsets.set3.exercise3

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ClientAndTopic {
    private val clientSet = mutableSetOf<RemoteClient>()
    private val topicSet = TopicSet()

    private val lock = ReentrantLock()

    fun getClientSetSize() = lock.withLock { clientSet.size }

    fun addClient(client: RemoteClient) = lock.withLock {
        clientSet.add(client)
    }

    fun removeClient(client: RemoteClient) = lock.withLock {
        clientSet.remove(client)
        topicSet.unsubscribe(client)
    }

    fun getSubs(topic: TopicName): Int = lock.withLock {
        return topicSet.getSubscribersFor(topic).size
    }

    fun publish(message: PublishedMessage) {
        val subscribers: Set<Subscriber>
        lock.withLock {
            subscribers = topicSet.getSubscribersFor(message.topicName)
        }
        subscribers.forEach { it.send(message) }
    }

    fun subscribe(topicName: TopicName, subscriber: Subscriber) = lock.withLock {
        topicSet.subscribe(topicName, subscriber)
    }

    fun unsubscribe(topicName: TopicName, subscriber: Subscriber) = lock.withLock {
        topicSet.unsubscribe(topicName, subscriber)
    }

    fun shutdown() {
        clientSet.forEach { it.shutdown() }
    }
}
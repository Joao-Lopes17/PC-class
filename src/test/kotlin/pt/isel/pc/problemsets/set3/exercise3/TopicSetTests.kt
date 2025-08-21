package pt.isel.pc.problemsets.set3.exercise3

import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class TopicSetTests {

    @Test
    fun `basic functionality tests`() {
        val topicSet = TopicSet()

        val topicNames = Array(3) {
            TopicName(it.toString())
        }
        val subscribers = Array(3) {
            TestSubscriber()
        }

        repeat(3) {
            topicSet.subscribe(topicNames[0], subscribers[it])
        }
        repeat(2) {
            topicSet.subscribe(topicNames[1], subscribers[it])
        }
        repeat(1) {
            topicSet.subscribe(topicNames[2], subscribers[it])
        }

        assertEquals(3, topicSet.getSubscribersFor(topicNames[0]).size)
        assertEquals(2, topicSet.getSubscribersFor(topicNames[1]).size)
        assertEquals(1, topicSet.getSubscribersFor(topicNames[2]).size)

        topicSet.unsubscribe(topicNames[0], subscribers[2])

        assertEquals(2, topicSet.getSubscribersFor(topicNames[0]).size)

        topicSet.unsubscribe(subscribers[2])

        topicSet.getTopicWithName(topicNames[2])
    }

    private class TestSubscriber : Subscriber {
        override fun send(message: PublishedMessage) {
            // Nothing, for testing purposes only
        }
    }
}
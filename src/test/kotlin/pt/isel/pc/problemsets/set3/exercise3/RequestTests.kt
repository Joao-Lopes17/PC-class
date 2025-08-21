package pt.isel.pc.problemsets.set3.exercise3

import kotlinx.coroutines.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pt.isel.pc.problemsets.set3.ourConnect
import pt.isel.pc.problemsets.set3.ourWrite
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import kotlin.test.assertEquals

class PublishSubscribeServerTest {
    private val hostName = "0.0.0.0"
    private val port = 23
    private lateinit var server: Server
    private lateinit var serverJob: Job
    private val nRequests = (1..5)

    @BeforeEach
    fun setUp() {
        server = Server.start(InetSocketAddress(hostName, port))
        serverJob = CoroutineScope(Dispatchers.Default).launch { server.join() }
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
        runBlocking {
            serverJob.join()
        }
    }

    @Test
    fun `test multiple publishes`() {
        runBlocking {
            val client = AsynchronousSocketChannel.open()
            val address = InetSocketAddress("localhost", port)

            client.ourConnect(address)
            val messages = nRequests.map { "PUBLISH topic$it message$it" }

            println(messages)

            messages.forEach {
                val reqByteBuffer = ByteBuffer.wrap(it.toByteArray())
                client.ourWrite(reqByteBuffer)
            }

            delay(1000)

            var receivedMessages = emptyList<String>()
            for(i in nRequests) {
                receivedMessages = receivedMessages + server.getReceivedMessages("topic$i")
            }

            println(receivedMessages)

            assertEquals(5, receivedMessages.size)
            assertEquals(messages, receivedMessages)
        }
    }
    fun getReceivedMessages(topicName:String): List<String> {
        val topic = topicSet.getTopicWithName(TopicName(topicName))
        return topic?.let {
            it.messages.toList()
        } ?: emptyList()
    }
}



package pt.isel.pc.problemsets.set3.exercise3

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set3.MessageQueue
import pt.isel.pc.problemsets.set3.exercise3.protocol.ClientRequest
import pt.isel.pc.problemsets.set3.exercise3.protocol.ClientResponse
import pt.isel.pc.problemsets.set3.exercise3.protocol.ServerPush
import pt.isel.pc.problemsets.set3.exercise3.protocol.parseClientRequest
import pt.isel.pc.problemsets.set3.exercise3.protocol.serialize
import pt.isel.pc.problemsets.set3.exercise3.utils.LineReader
import pt.isel.pc.problemsets.set3.exercise3.utils.SuccessOrError
import pt.isel.pc.problemsets.set3.ourRead
import pt.isel.pc.problemsets.set3.ourWrite
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

/**
 * The component responsible to interact with a remote client, via a [Socket].
 */
class RemoteClient private constructor(
    private val server: Server,
    val clientId: String,
    private val clientSocket: AsynchronousSocketChannel,
    scope: CoroutineScope,
    private val clientAndTopic: ClientAndTopic
) : Subscriber {
    private val controlQueue = MessageQueue<ControlMessage>()
    private val controlCoroutine: Job
    private lateinit var readCoroutine: Job
    private var state = State.RUNNING

    init {
        controlCoroutine = scope.launch {
            readCoroutine = launch {
                logger.info("[{}] Remote client started read coroutine", clientId)
                readLoop()
            }
            logger.info("[{}] Remote client started main coroutine", clientId)
            controlLoop()
        }
    }

    fun shutdown() {
        controlQueue.enqueue(ControlMessage.Shutdown)
    }

    override fun send(message: PublishedMessage) {
        controlQueue.enqueue(ControlMessage.Message(message))
    }

    private suspend fun handleShutdown() {
        if (state != State.RUNNING) {
            return
        }
        val byteBuffer = ByteBuffer.wrap((serialize(ServerPush.Bye)).toByteArray())
        clientSocket.ourWrite(byteBuffer)

        clientSocket.close()
        state = State.SHUTTING_DOWN
    }

    private suspend fun handleMessage(message: PublishedMessage) {
        if (state != State.RUNNING) {
            return
        }

        val byteBuffer = ByteBuffer.wrap((serialize(ServerPush.PublishedMessage(message))).toByteArray())
        clientSocket.ourWrite(byteBuffer)
    }

    private suspend fun handleClientSocketLine(line: String) {
        if (state != State.RUNNING) {
            return
        }
        val response = when (val res = parseClientRequest(line)) {
            is SuccessOrError.Success -> when (val request = res.value) {
                is ClientRequest.Publish -> {
                    clientAndTopic.publish(PublishedMessage(request.topic, request.message))
                    val subscribers = clientAndTopic.getSubs(request.topic)
                    ClientResponse.OkPublish(subscribers)
                }

                is ClientRequest.Subscribe -> {
                    request.topics.forEach {
                        clientAndTopic.subscribe(it, this)
                    }
                    ClientResponse.OkSubscribe
                }

                is ClientRequest.Unsubscribe -> {
                    request.topics.forEach {
                        clientAndTopic.unsubscribe(it, this)
                    }
                    ClientResponse.OkUnsubscribe
                }
            }

            is SuccessOrError.Error -> {
                ClientResponse.Error(res.error)
            }
        }

        val byteBuffer = ByteBuffer.wrap((serialize(response)).toByteArray())
        clientSocket.ourWrite(byteBuffer)
    }


    private fun handleClientSocketError(throwable: Throwable) {
        logger.info("Client socket operation thrown: {}", throwable.message)
    }

    private fun handleClientSocketEnded() {
        if (state != State.RUNNING) {
            return
        }
        state = State.SHUTTING_DOWN
    }

    private fun handleReadLoopEnded() {
        state = State.SHUTDOWN
    }

    private suspend fun controlLoop() {
        try {
            val byteBuffer = ByteBuffer.wrap((serialize(ServerPush.Hi)).toByteArray())
            clientSocket.ourWrite(byteBuffer)
            while (state != State.SHUTDOWN) {
                val controlMessage = controlQueue.dequeue()
                logger.info("[{}] main coroutine received {}", clientId, controlMessage)
                when (controlMessage) {
                    ControlMessage.Shutdown -> handleShutdown()
                    is ControlMessage.Message -> handleMessage(controlMessage.value)
                    is ControlMessage.ClientSocketLine -> handleClientSocketLine(controlMessage.value)
                    ControlMessage.ClientSocketEnded -> handleClientSocketEnded()
                    is ControlMessage.ClientSocketError -> handleClientSocketError(controlMessage.throwable)
                    ControlMessage.ReadLoopEnded -> handleReadLoopEnded()
                }
            }
        } finally {
            logger.info("[{}] remote client ending", clientId)
            clientAndTopic.removeClient(this)
            server.remoteClientEnded(this)
        }
    }


    private suspend fun readLoop() {
        try {
            val reader = LineReader { clientSocket.ourRead(it) }
            while (true) {
                val line: String? = reader.readLine()
                if (line == null) {
                    logger.info("[{}] end of input stream reached", clientId)
                    controlQueue.enqueue(ControlMessage.ClientSocketEnded)
                    return
                }
                logger.info("[{}] line received: {}", clientId, line)
                controlQueue.enqueue(ControlMessage.ClientSocketLine(line))
            }
        } catch (ex: Throwable) {
            logger.info("[{}]Exception on read loop: {}, {}", clientId, ex.javaClass.name, ex.message)
            controlQueue.enqueue(ControlMessage.ClientSocketError(ex))
        } finally {
            logger.info("[{}] client loop ending", clientId)
            controlQueue.enqueue(ControlMessage.ReadLoopEnded)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteClient::class.java)
        fun start(
            server: Server,
            clientId: String,
            socket: AsynchronousSocketChannel,
            scope: CoroutineScope,
            clientAndTopic: ClientAndTopic
        ) {
            clientAndTopic.addClient(RemoteClient(server, clientId, socket, scope, clientAndTopic))
        }
    }

    private sealed interface ControlMessage {
        data class Message(val value: PublishedMessage) : ControlMessage
        data object Shutdown : ControlMessage
        data object ClientSocketEnded : ControlMessage
        data class ClientSocketError(val throwable: Throwable) : ControlMessage
        data class ClientSocketLine(val value: String) : ControlMessage
        data object ReadLoopEnded : ControlMessage
    }

    private enum class State {
        RUNNING,
        SHUTTING_DOWN,
        SHUTDOWN,
    }
}

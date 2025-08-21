package pt.isel.pc.problemsets.set3.exercise3

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set3.MessageQueue
import pt.isel.pc.problemsets.set3.ourAccept
import java.net.SocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel


/**
 * The server component.
 */
class Server private constructor(
    private val serverSocket: AsynchronousServerSocketChannel,
    private val controlQueue: MessageQueue<ControlMessage>,
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private var controlCoroutine: Job
    private lateinit var acceptCoroutine: Job
    private val clientAndTopic = ClientAndTopic()

    private var currentClientId = 0
    private val usersLimit = 5
    private var state = State.RUNNING
    private var acceptCoroutineEnded = false

    init {
        controlCoroutine = scope.launch {
            acceptCoroutine = launch {
                acceptLoop()
            }
            controlLoop()
        }
    }

    fun shutdown() {
        controlQueue.enqueue(ControlMessage.Shutdowm)
    }

    fun remoteClientEnded(client: RemoteClient) {
        controlQueue.enqueue(ControlMessage.RemoteClientEnded(client))
    }

    suspend fun join() {
        controlCoroutine.join()
    }

    private fun handleNewClientSocket(clientSocket: AsynchronousSocketChannel, scope: CoroutineScope) {
        if (state != State.RUNNING) {
            clientSocket.close()
            return
        }

        val newId = currentClientId++
        RemoteClient.start(this, newId.toString(), clientSocket, scope, clientAndTopic)
        logger.info("Server: started new remote client")
    }

    private fun handleRemoteClientEnded(remoteClient: RemoteClient) {
        logger.info("Server: remote client ended {}", remoteClient.clientId)
        if (state == State.SHUTTING_DOWN) {
            if (clientAndTopic.getClientSetSize() == 0 && acceptCoroutineEnded) {
                state = State.SHUTDOWN
            }
        }
    }

    private fun handleServerIsFull(clientSocket: AsynchronousSocketChannel) {
        clientSocket.close()
    }

    private fun handleShutdown() {
        if (state != State.RUNNING) {
            return
        }
        startShutdown()
    }

    private fun startShutdown() {
        serverSocket.close()
        clientAndTopic.shutdown()
        state = State.SHUTTING_DOWN
    }

    private fun handleAcceptLoopEnded() {
        acceptCoroutineEnded = true
        if (state != State.SHUTTING_DOWN) {
            logger.info("Accept loop ended unexpectedly")
            startShutdown()
        }
        if (clientAndTopic.getClientSetSize() == 0) {
            state = State.SHUTDOWN
        }
    }

    private suspend fun controlLoop() {
        try {
            supervisorScope {
                while (state != State.SHUTDOWN) {
                    try {
                        when (val controlMessage = controlQueue.dequeue()) {
                            is ControlMessage.NewClientSocket -> handleNewClientSocket(
                                controlMessage.clientSocket,
                                this,
                            )

                            is ControlMessage.RemoteClientEnded -> handleRemoteClientEnded(controlMessage.remoteClient)
                            is ControlMessage.ServerIsFull -> handleServerIsFull(controlMessage.clientSocket)
                            ControlMessage.Shutdowm -> handleShutdown()
                            ControlMessage.AcceptLoopEnded -> handleAcceptLoopEnded()
                        }
                    } catch (ex: Throwable) {
                        logger.info("Unexpected exception, ignoring it", ex)
                    }
                }
            }
        } finally {
            logger.info("server ending")
        }

    }

    private suspend fun acceptLoop() {
        try {
            while (true) {
                val clientSocket = serverSocket.ourAccept()
                if (clientAndTopic.getClientSetSize() >= usersLimit) {
                    logger.info("Server: remote client didn't started because server is full")
                    controlQueue.enqueue(ControlMessage.ServerIsFull(clientSocket))
                } else {
                    logger.info("New client socket accepted")
                    controlQueue.enqueue(ControlMessage.NewClientSocket(clientSocket))
                }
            }
        } catch (ex: Exception) {
            logger.info("Exception on accept loop: {}", ex.message)
            // continue
        } finally {
            controlQueue.enqueue(ControlMessage.AcceptLoopEnded)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Server::class.java)
        fun start(address: SocketAddress): Server {
            val serverSocket = AsynchronousServerSocketChannel.open()
            serverSocket.bind(address)
            val controlQueue = MessageQueue<ControlMessage>()
            return Server(serverSocket, controlQueue)
        }
    }

    private sealed interface ControlMessage {
        data class NewClientSocket(val clientSocket: AsynchronousSocketChannel) : ControlMessage
        data class RemoteClientEnded(val remoteClient: RemoteClient) : ControlMessage
        data class ServerIsFull(val clientSocket: AsynchronousSocketChannel) : ControlMessage

        data object Shutdowm : ControlMessage
        data object AcceptLoopEnded : ControlMessage
    }

    private enum class State {
        RUNNING,
        SHUTTING_DOWN,
        SHUTDOWN,
    }
}

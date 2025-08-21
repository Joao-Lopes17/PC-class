package pt.isel.pc.problemsets.set3

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import kotlin.test.Test
import java.nio.channels.AsynchronousSocketChannel

class AsynchronousServerSocketChannelTest {
    @Test
    fun `using synchronous IO`() {
        try {
            val socket = Socket()
            val address = InetSocketAddress("httpbin.org", 80)
            socket.connect(address)
            val requestString = "GET /get HTTP/1.1\r\nHost:httpbin.org\r\n\r\n"
            val requestBytes = requestString.toByteArray()
            socket.getOutputStream().write(requestBytes)
            val responseBytes = ByteArray(1024)
            val readLen = socket.getInputStream().read(responseBytes, 0, responseBytes.size)
            val responseString = String(responseBytes, 0, readLen)
            println(responseString)
        } catch (ex: Throwable) {
            println(ex.message)
        }
    }

    @Test
    fun `using asynchronous IO`() {
        runBlocking {
            val socket = AsynchronousSocketChannel.open()
            val address = InetSocketAddress("httpbin.org", 80)
            socket.ourConnect(address)

            val requestString = "GET /get HTTP/1.1\r\nHost:httpbin.org\r\n\r\n"
            val requestByteBuffer = ByteBuffer.wrap(requestString.toByteArray())
            socket.ourWrite(requestByteBuffer)

            val responseByteBuffer = ByteBuffer.allocate(1024)
            val rspLen = socket.ourRead(responseByteBuffer)

            val responseBytes = responseByteBuffer.array()
            val responseString = String(responseBytes, 0, rspLen)

            println(responseString)
        }
    }

    @Test
    fun `test accept & connect`() {
        val port = 12345
        val server = AsynchronousServerSocketChannel.open().bind(InetSocketAddress(port))
        runBlocking {
            val client = AsynchronousSocketChannel.open()
            val address = InetSocketAddress("localhost", port)

            client.ourConnect(address)
            server.ourAccept()
        }
    }

    @Test
    fun `test read & write`() {
        val port = 54321
        val server = AsynchronousServerSocketChannel.open().bind(InetSocketAddress(port))
        runBlocking {
            val client = AsynchronousSocketChannel.open()
            val address = InetSocketAddress("localhost", port)

            client.ourConnect(address)
            val clientChannel = server.ourAccept()

            val reqString = "Testing read and write"
            val reqByteBuffer = ByteBuffer.wrap(reqString.toByteArray())
            val reqLen = client.ourWrite(reqByteBuffer)

            val rspByteBuffer = ByteBuffer.allocate(1024)
            val rspLen = clientChannel.ourRead(rspByteBuffer)

            val rspBytes = rspByteBuffer.array()
            val rspString = String(rspBytes, 0, rspLen)

            assertEquals(reqLen, rspLen)
            assertEquals(reqString, rspString)
        }
    }
}
package pt.isel.pc.problemsets.set3

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun AsynchronousServerSocketChannel.ourAccept(): AsynchronousSocketChannel {
    return suspendCancellableCoroutine { continuation ->
        try {
            this.accept(
                Unit,
                object : CompletionHandler<AsynchronousSocketChannel, Unit> {
                    override fun completed(result: AsynchronousSocketChannel, attachment: Unit?) {
                        continuation.resume(result) {
                            result.close()
                        }
                    }

                    override fun failed(exc: Throwable?, attachment: Unit?) {
                        continuation.resumeWithException(exc!!)
                    }
                },
            )
        } catch (ex: Throwable) {
            continuation.resumeWithException(ex)
        }
    }
}


suspend fun AsynchronousSocketChannel.ourConnect(address: SocketAddress): Void? {
    return suspendCancellableCoroutine { continuation ->
        try {
            this.connect(
                address,
                Unit,
                object : CompletionHandler<Void, Unit> {
                    override fun completed(result: Void?, attachment: Unit?) {
                        continuation.resume(result)
                    }

                    override fun failed(exc: Throwable?, attachment: Unit?) {
                        continuation.resumeWithException(exc!!)                     // !!
                    }
                },
            )
        } catch (ex: Throwable) {
            continuation.resumeWithException(ex)
        }
    }
}

suspend fun AsynchronousSocketChannel.ourRead(dst: ByteBuffer): Int {
    return suspendCancellableCoroutine { continuation ->
        try {
            this.read(
                dst,
                Unit,
                object : CompletionHandler<Int, Unit> {
                    override fun completed(result: Int, attachment: Unit) {
                        continuation.resume(result)
                    }

                    override fun failed(exc: Throwable, attachment: Unit) {
                        continuation.resumeWithException(exc)
                    }
                },
            )
        } catch (ex: Throwable) {
            continuation.resumeWithException(ex)
        }
    }
}


suspend fun AsynchronousSocketChannel.ourWrite(src: ByteBuffer): Int {
    return suspendCancellableCoroutine { continuation ->
        try {
            this.write(
                src,
                Unit,
                object : CompletionHandler<Int, Unit> {
                    override fun completed(result: Int, attachment: Unit) {
                        continuation.resume(result)
                    }

                    override fun failed(exc: Throwable, attachment: Unit) {
                        continuation.resumeWithException(exc)
                    }
                },
            )
        } catch (ex: Throwable) {
            continuation.resumeWithException(ex)
        }
    }
}

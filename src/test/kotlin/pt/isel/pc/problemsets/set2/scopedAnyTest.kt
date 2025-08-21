package pt.isel.pc.problemsets.set2

import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScopedAnyTest {
    @Test
    fun `all complete`() {
        val f1 = CompletableFuture<Int>()
        val f2 = CompletableFuture<Int>()
        val f3 = CompletableFuture<Int>()
        val listFutures = listOf(f1, f2, f3)
        val future = Future<Int>()
        val sut = future.scopedAny(listFutures) { println(it) }

        f1.complete(2)
        f2.complete(3)
        f3.complete(4)

        assertEquals(2, sut.get())
    }

    @Test
    fun `all exception`() {
        val f1 = CompletableFuture<Int>()
        val f2 = CompletableFuture<Int>()
        val f3 = CompletableFuture<Int>()
        val listFutures = listOf(f1, f2, f3)
        val future = Future<Int>()
        val sut = future.scopedAny(listFutures) { println(it) }

        val thr1 = Throwable("Fail 1")
        val thr2 = Throwable("Fail 2")
        val thr3 = Throwable("Fail 3")

        f1.completeExceptionally(thr1)
        f2.completeExceptionally(thr2)
        f3.completeExceptionally(thr3)

        assertFailsWith<Throwable> { sut.get() }
    }

    @Test
    fun `one complete`() {
        val f1 = CompletableFuture<Int>()
        val f2 = CompletableFuture<Int>()
        val f3 = CompletableFuture<Int>()
        val listFutures = listOf(f1, f2, f3)
        val future = Future<Int>()
        val sut = future.scopedAny(listFutures) { println(it) }

        val thr2 = Throwable("Fail 2")
        val thr3 = Throwable("Fail 3")

        f2.completeExceptionally(thr2)
        f1.complete(1)
        f3.completeExceptionally(thr3)

        assertEquals(1, sut.get())
    }
}

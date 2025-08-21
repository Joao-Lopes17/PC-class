package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pt.isel.pc.problemsets.utils.TestHelper
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.seconds

class BlockingMessageQueueTest {

    companion object {
        private const val N_OF_THREADS = 1000
    }

    @Test
    fun `try enqueue and try dequeue`() {
        val sync = BlockingMessageQueue<String>(2)
        val testHelper = TestHelper(3.seconds)
        testHelper.createAndStartMultiple(N_OF_THREADS) { _, isDone ->
            while (!isDone()) {
                assertTrue(sync.tryEnqueue(listOf("a"), INFINITE))
                val sut = sync.tryDequeue(INFINITE)
                assertEquals(sut,"a")
            }
        }
        testHelper.join()
    }

    @Test
    fun interrupt() {
        val sync = BlockingMessageQueue<String>(1)
        val testHelper = TestHelper(5.seconds)
        val th1 = testHelper.thread {
            assertThrows<InterruptedException> { sync.tryEnqueue(listOf("a", "b"), INFINITE) }
        }

        testHelper.thread { sync.tryEnqueue(listOf("c"), INFINITE) }
        th1.interrupt()
        testHelper.join()
        val th3 = testHelper.thread { assertThrows<InterruptedException> { sync.tryDequeue(INFINITE) } }
        testHelper.thread {
            val sut = sync.tryDequeue(INFINITE)
            assertTrue { sut == "c" }
        }

        th3.interrupt()
        testHelper.join()
    }

    @Test
    fun timeoutA() {
        val sync = BlockingMessageQueue<String>(2)
        val testHelper = TestHelper(6.seconds)
        testHelper.createAndStartMultiple(N_OF_THREADS) { _, isDone ->
            while (!isDone())
                assertFalse(sync.tryEnqueue(listOf("a", "b", "c"), 1.microseconds))
        }
        testHelper.join()
        testHelper.createAndStartMultiple(N_OF_THREADS) { _, isDone ->
            while (!isDone())
                assertEquals(null, sync.tryDequeue(1.microseconds))
        }
        testHelper.join()

    }
}
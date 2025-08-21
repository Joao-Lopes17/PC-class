package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pt.isel.pc.problemsets.utils.TestHelper
import java.lang.AssertionError
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

class CountDownLatchTest {

    companion object {
        private const val INITIAL_COUNT = 5
        private const val N_OF_THREADS = 10
    }

    @Test
    fun `countDownLatch test`() {
        val sut = CountDownLatch(INITIAL_COUNT)
        val testHelper = TestHelper(5.seconds)

        testHelper.thread { sut.await() }
        testHelper.createAndStartMultiple(N_OF_THREADS){ _, isDone ->
            while (!isDone())
                testHelper.thread { sut.countDown() }
        }
        testHelper.join()
        assertEquals(sut.getCount(), 0)
    }

    @Test
    fun `countDownLatch timeout lower test`() {
        val sut = CountDownLatch(INITIAL_COUNT)
        val testHelper = TestHelper(5.seconds)

        testHelper.thread { assertFalse(sut.await(3, TimeUnit.SECONDS)) }

        testHelper.join()
    }

    @Test
    fun `countDownLatch timeout higher test`() {
        val sut = CountDownLatch(INITIAL_COUNT)
        val testHelper = TestHelper(5.seconds)

        testHelper.thread { assertThrows<AssertionError> {sut.await(8, TimeUnit.SECONDS)} }

        testHelper.join()
    }

    @Test
    fun `countDownLatch InterruptedException test`() {
        val sut = CountDownLatch(INITIAL_COUNT)
        val testHelper = TestHelper(5.seconds)

        val th1 = testHelper.thread { assertThrows<InterruptedException> { sut.await() } }
        th1.interrupt()

        val th2 = testHelper.thread { assertThrows<InterruptedException> { sut.await(8, TimeUnit.SECONDS) } }
        th2.interrupt()

        testHelper.join()
    }
}
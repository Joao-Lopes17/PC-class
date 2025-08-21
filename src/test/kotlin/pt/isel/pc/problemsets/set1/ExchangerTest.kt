package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError
import pt.isel.pc.problemsets.utils.TestHelper
import java.lang.AssertionError
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class ExchangerTest {

    companion object {
        private const val N_OF_THREADS = 10
    }

    @Test
    fun exchange() {
        val sync = Exchanger<String>()
        val testHelper = TestHelper(5.seconds)

        testHelper.thread { assertTrue(sync.exchange("a") == "b") }
        testHelper.thread { assertTrue(sync.exchange("b") == "a") }
        testHelper.join()
    }

    @Test
    fun `exchange with multiple threads`() {
        val sync = Exchanger<Int>()
        val testHelper = TestHelper(5.seconds)
        val map = mutableMapOf<Int, Int>()

        testHelper.createAndStartMultiple(N_OF_THREADS) { thr, _ ->
            val sut = sync.exchange(thr)
            map[thr] = sut
        }

        testHelper.join()

        map.keys.forEach { key ->
            val value = map[key]
            assertEquals(key, map[value])
        }
    }

    @Test
    fun `exchanger timeout lower test`(){
        val sut = Exchanger<String>()
        val testHelper = TestHelper(5.seconds)

        testHelper.thread { assertThrows<TimeoutException> {
            sut.exchange("a",3,TimeUnit.SECONDS)
        } }

        testHelper.join()
    }

    @Test
    fun interrupt(){
        val sut = Exchanger<String>()
        val testHelper = TestHelper(5.seconds)

        val th1 = testHelper.thread { assertThrows<InterruptedException> {
            sut.exchange("a",3,TimeUnit.SECONDS)
        } }
        th1.interrupt()

        testHelper.join()
    }
}
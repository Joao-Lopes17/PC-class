package pt.isel.pc.problemsets.set2

import pt.isel.pc.problemsets.set2.UnsafeSuccession
import pt.isel.pc.problemsets.set1.BlockingMessageQueueTest
import pt.isel.pc.problemsets.utils.TestHelper
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class UnsafeSuccessionTest {
    companion object {
        private const val N_OF_THREADS = 200
    }

    @Test
    fun next() {
        val array = arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        val retArray = ConcurrentLinkedQueue<Int>()
        val sut = UnsafeSuccession(array)
        val testHelper = TestHelper(3.seconds)

        testHelper.createAndStartMultiple(N_OF_THREADS) { _ , _ ->
            val item = sut.next()
            if (item != null) retArray.add(item)
        }
        testHelper.join()

        assertEquals(array.toList(), retArray.sorted())
    }
}

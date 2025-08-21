package pt.isel.pc.problemsets.set1

import pt.isel.pc.problemsets.utils.TestHelper
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RaceTest {

    companion object {
        private const val N_OF_THREADS = 5
        fun mat1(): () -> Int = { 1 }
        fun mat2(): () -> Int = { 2 }
        fun mat3(): () -> Int = { 3 }
    }
    @Test
    fun raceWorking() {
        val testHelper = TestHelper(3.seconds)
        testHelper.createAndStartMultiple(N_OF_THREADS) { _, _ ->
            val sut = race(listOf(mat1(), mat2(), mat3()), Duration.INFINITE)
            assertTrue { sut == 1 || sut == 2 || sut == 3 }
        }
        testHelper.join()
    }
}
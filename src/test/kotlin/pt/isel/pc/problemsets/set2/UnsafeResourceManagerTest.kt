package pt.isel.pc.problemsets.set2

import pt.isel.pc.problemsets.set2.UnsafeResourceManager
import pt.isel.pc.problemsets.utils.TestHelper
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class UnsafeResourceManagerTest {
    companion object {
        private const val N_OF_THREADS_RELEASE = 200
        private const val N_OF_THREADS = 9
    }

    @Test
    fun release() {
        val usage = 10
        val close = Close()
        val sut = UnsafeResourceManager(close , usage)
        val testHelper = TestHelper(3.seconds)

        testHelper.createAndStartMultiple(N_OF_THREADS_RELEASE) { _ , _ ->
            try {
                sut.release()
            }
            catch (e: IllegalStateException){
                println(e)
            }
        }
        testHelper.join()

        assertEquals(close.closed, true)
    }

    @Test
    fun dontClose() {
        val usage = 10
        val close = Close()
        val sut = UnsafeResourceManager(close , usage)
        val testHelper = TestHelper(3.seconds)
        testHelper.createAndStartMultiple(N_OF_THREADS) { _, _ ->
            try {
                sut.release()
            }
            catch (e: IllegalStateException){
                println(e)
            }
        }
        testHelper.join()

        assertEquals(close.closed, false)
    }
}



class Close : AutoCloseable {
    var closed = false

    override fun close() {
        println("No more uses")
        closed = true
    }
}

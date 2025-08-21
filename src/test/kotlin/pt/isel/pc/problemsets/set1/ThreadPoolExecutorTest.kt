package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pt.isel.pc.problemsets.utils.NodeLinkedList
import pt.isel.pc.problemsets.utils.TestHelper
import java.lang.AssertionError
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.seconds

class ThreadPoolExecutorTest {
    companion object {
        private const val KEEP_ALIVE_TIME = 2
        private const val MAX_THREAD_POOL_SIZE = 5
        private const val NUMBER_OF_TASKS = 10
    }

    @Test
    fun `try execute, shutdown and awaitTermination`() {

        val thrPool = ThreadPoolExecutor(MAX_THREAD_POOL_SIZE, KEEP_ALIVE_TIME.seconds)

        val taskCount = AtomicInteger(0)
        val completedTasks = NodeLinkedList<Int>()

        val testHelper = TestHelper(5.seconds)

        //half working, half waiting
        testHelper.createAndStartMultiple(NUMBER_OF_TASKS) { _, _ ->
            thrPool.execute(Runnable {
                val taskId = taskCount.incrementAndGet()
                completedTasks.enqueue(taskId)
            })
        }

        testHelper.join()

        testHelper.thread {
            thrPool.shutDown()
        }

        testHelper.join()

        val shutdownCompleted = thrPool.awaitTermination(Duration.INFINITE)
        Assertions.assertTrue(shutdownCompleted, "Shutdown should have completed")
        Assertions.assertEquals(NUMBER_OF_TASKS, completedTasks.count, "Not all tasks were completed")

    }

    @Test
    fun `low time awaitTermination`() {
        val thrPool = ThreadPoolExecutor(MAX_THREAD_POOL_SIZE, KEEP_ALIVE_TIME.seconds)

        val taskCount = AtomicInteger(0)
        val completedTasks = NodeLinkedList<Int>()

        val testHelper = TestHelper(5.seconds)

        testHelper.createAndStartMultiple(NUMBER_OF_TASKS) { _, _ ->
            thrPool.execute(Runnable {
                val taskId = taskCount.incrementAndGet()
                completedTasks.enqueue(taskId)
            })
        }

        testHelper.join()

        thrPool.shutDown()


        val shutdownCompleted = thrPool.awaitTermination(1.microseconds)
        Assertions.assertFalse(shutdownCompleted, "Shutdown should have completed")

        Assertions.assertEquals(NUMBER_OF_TASKS, completedTasks.count, "Not all tasks were completed")
    }

    @Test
    fun interrupt() {
        val thrPool = ThreadPoolExecutor(MAX_THREAD_POOL_SIZE, KEEP_ALIVE_TIME.seconds)

        val taskCount = AtomicInteger(0)
        val completedTasks = NodeLinkedList<Int>()

        val testHelper = TestHelper(5.seconds)

        val th = testHelper.thread { assertThrows<InterruptedException> {
            thrPool.execute(Runnable {
                val taskId = taskCount.incrementAndGet()
                completedTasks.enqueue(taskId)
            })
            thrPool.shutDown()
            thrPool.awaitTermination(1.seconds)
        }  }
        th.interrupt()
        testHelper.join()
    }
}
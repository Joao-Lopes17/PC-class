package pt.isel.pc.problemsets.set3

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageQueueTest {
    @Test
    fun `fast path`() {
        runBlocking {
            val queue = MessageQueue<Int>()
            launch {
                queue.enqueue(1)
            }
            delay(1000)
            val sut = queue.dequeue()
            assertEquals(1, sut)
        }

    }
    @Test
    fun `wait path`() {
        runBlocking {
            val queue = MessageQueue<Int>()
            launch {
                val sut = queue.dequeue()
                assertEquals(1, sut)
            }
            launch {
                delay(1000)
                queue.enqueue(1)
            }
        }

    }
    @Test
    fun `cancellation on wait path`() {
        runBlocking {
            val semaphore = Semaphore(1, 1)
            val queue = MessageQueue<Int>()
            val c1 = launch {
                val sut = queue.dequeue()
                semaphore.release()
                assertEquals(1, sut)

            }
            delay(1000)
            queue.enqueue(1)
            c1.cancel()
            semaphore.acquire()
        }
    }
//@Test
//fun `stress test`(){
//    runBlocking {
//        val queue = MessageQueue<Int>()
//        re
//    }
//}
}
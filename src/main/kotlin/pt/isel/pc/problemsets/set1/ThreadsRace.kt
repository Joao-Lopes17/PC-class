package pt.isel.pc.problemsets.set1

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
class ThreadsRace <T>{

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var flag = false
    private val threadsWorking = ConcurrentLinkedQueue<Thread>()
    private val threadsFinished = ConcurrentLinkedQueue<T>()

    @Throws(InterruptedException::class)
    fun createRace(suppliers: List<()->T>) {
        lock.withLock {
            for (supplier in suppliers) {
                val thr = Thread {
                    val execution = supplier()
                    if(threadsFinished.isEmpty()) {
                        threadsFinished.add(execution)
                        flag = true
                        signal()
                    }
                    //win(execution, Thread.currentThread())
                }
                threadsWorking.add(thr)
            }
            threadsWorking.forEach { it.start() }
        }
    }
    fun waitWinner(timeout: Duration): T?{
        lock.withLock {
            //fast-path
            if(flag){
                stopThreads(threadsWorking)
                return threadsFinished.peek()
            }
            //wait-path
            var timeoutInNanos = timeout.inWholeNanoseconds
            while (true) {
                try {
                    timeoutInNanos = condition.awaitNanos(timeoutInNanos)
                } catch (ex: InterruptedException) {
                    /*
                    O mesmo deve acontecer caso a thread onde a
                    função race foi executada seja alvo de uma interrupção.
                     */
                    stopThreads(threadsWorking)
                    if(flag){
                        return threadsFinished.peek()
                    }
                    throw ex
                }
                /*
                A produção de um valor por uma destas funções deve cancelar a execução das restantes funções,
                usando o mecanismo de interrupções. A chamada da função deve apenas acabar quando todas as threads
                criadas internamente tiverem acabado, retornando o primeiro valor a ter sido produzido por um dos suppliers
                */
                if (flag) {
                    stopThreads(threadsWorking)
                    return threadsFinished.peek()

                }
                /*
                Caso tenha decorrido a duração timeout desde o início da chamada e nenhum valor tiver sido produzido,
                então deve ser tentado o cancelamento de todas as funções
                 */
                if (timeoutInNanos <= 0){
                    stopThreads(threadsWorking)
                    return null
                }

            }
        }

    }

    private fun signal(){
        lock.withLock {
            condition.signalAll()
        }
    }

    private fun win(execution:T, thr:Thread) {
        lock.withLock {
            flag = true
            threadsWorking.remove(thr)
            threadsFinished.add(execution)
            condition.signalAll()
        }
    }

    private fun stopThreads(threads:ConcurrentLinkedQueue<Thread>){
        for (thread in threads) {
            thread.interrupt()
        }
        for (thread in threads) {
            thread.join()
        }

    }
}

fun<T> race(suppliers: List<()->T>, timeout: Duration): T?{
    val sync = ThreadsRace<T>()
    sync.createRace(suppliers)
    val result =sync.waitWinner(timeout)
    return result
}

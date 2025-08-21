package pt.isel.pc.problemsets.set1

import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import pt.isel.pc.problemsets.utils.NodeLinkedList

class ThreadPoolExecutor(
    private val maxThreadPoolSize: Int,
    private val keepAliveTime: Duration,
) {
    private val lock = ReentrantLock()
    private var nOfThreads: Int = 0
    private var workItems = NodeLinkedList<Runnable>()
    private var shutDownFlag = false
    private val terminationCondition = lock.newCondition()
    private val waitingThreads = NodeLinkedList<Condition>()
    init {
        require(maxThreadPoolSize > 0) { "maxThreadPoolSize must be greater than zero" }
    }

    private sealed interface GetNextWorkItemResult {
        data class WorkItem(val item: Runnable) : GetNextWorkItemResult
        data object Exit : GetNextWorkItemResult
    }

    private fun waitForWork(timeout: Duration): GetNextWorkItemResult {
        lock.withLock {
            //fast-path
            if (workItems.notEmpty) {
                return GetNextWorkItemResult.WorkItem(workItems.pull().value)
            }

            if (shutDownFlag) GetNextWorkItemResult.Exit
            //wait-path
            var timeoutInNanos = timeout.inWholeNanoseconds
            val selfCondition = lock.newCondition()
            val selfNode = waitingThreads.enqueue(selfCondition)
            while (true) {
                try {
                    timeoutInNanos = selfCondition.awaitNanos(timeoutInNanos)
                } catch (ex: InterruptedException) {
                    if (workItems.notEmpty) return GetNextWorkItemResult.WorkItem(workItems.pull().value)
                    waitingThreads.remove(selfNode)
                    GetNextWorkItemResult.Exit
                }
                if (workItems.notEmpty) {
                    return GetNextWorkItemResult.WorkItem(workItems.pull().value)
                }
                if(shutDownFlag) return GetNextWorkItemResult.Exit
                if (timeoutInNanos <= 0) {
                    GetNextWorkItemResult.Exit
                }
            }
        }
    }

    @Throws(RejectedExecutionException::class)
    fun execute(workItem: Runnable) {
        lock.withLock {
            try {
                if (!shutDownFlag) {
                    if (nOfThreads < maxThreadPoolSize && waitingThreads.empty) {
                        val thread = Thread {
                            var work = workItem
                            while (true) {
                                safeRun(work)
                                when (val res = waitForWork(keepAliveTime)) {
                                    is GetNextWorkItemResult.WorkItem -> work = res.item
                                    GetNextWorkItemResult.Exit -> {
                                        nOfThreads--
                                        if (nOfThreads == 0 && shutDownFlag) signal()
                                        break
                                    }
                                }
                            }
                        }
                        thread.start()
                        nOfThreads += 1
                    } else {
                        workItems.enqueue(workItem)
                        if(waitingThreads.notEmpty)waitingThreads.pull().value.signal()
                    }
                }
                else throw RejectedExecutionException("ShutDown")
            } catch (ex: RejectedExecutionException) {
                //Ignore
            }
        }
    }

    private fun signal() {
        lock.withLock { terminationCondition.signalAll() }
    }

    fun shutDown() {
        lock.withLock {
            for (condition in waitingThreads)
                condition.signal()
            shutDownFlag = true
        }
    }


    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Duration): Boolean {
        lock.withLock {
            //fast-path
            if (nOfThreads == 0) {
                return true
            }
            //wait-path
            var timeoutInNanos = timeout.inWholeNanoseconds
            val selfCondition = terminationCondition

            while (true) {
                try {
                    timeoutInNanos = selfCondition.awaitNanos(timeoutInNanos)
                } catch (ex: InterruptedException) {
                    throw ex
                }
                if (nOfThreads == 0) {
                    return true
                }
                if (timeoutInNanos <= 0) {
                    return false
                }
            }
        }
    }


    companion object {
        private fun safeRun(runnable: Runnable) {
            try {
                runnable.run()
            } catch (ex: Throwable) {
                // ignore
            }
        }
    }
}
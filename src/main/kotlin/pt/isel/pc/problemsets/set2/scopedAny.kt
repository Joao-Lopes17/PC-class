package pt.isel.pc.problemsets.set2

import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class Future<T> {
    fun scopedAny(futures: List<CompletableFuture<T>>, onSuccess: (T) -> Unit): CompletableFuture<T> {
        val f = CompletableFuture<T>()
        val box = Box(futures.size, f, onSuccess)
        futures.forEach { future ->
            future.handleAsync { value, throwable ->
                control(value, throwable, box)
            }
        }
        return f
    }

    private fun control(t: T?, thr: Throwable?, box: Box<T>) {
        if (thr != null) {
            box.addFail(thr)
        } else {
            box.addSuccess(t!!)
        }
    }
}

class Box<T>(private val futuresSize: Int, private val f: CompletableFuture<T>, private val onSuccess: (T) -> Unit) {
    private var counter = AtomicInteger()
    private var exception = Exception()
    private var success: AtomicReference<T> = AtomicReference()

    fun addSuccess(result: T) {
        while (true) {
            if (success.compareAndSet(null, result)) {
                onSuccess(success.get())
            }
            val observedCounter = counter.get()
            val nextCounter = observedCounter + 1
            if (counter.compareAndSet(observedCounter, nextCounter)) {
                if (nextCounter == futuresSize) {
                    f.complete(success.get())
                }
                return
            }

        }
    }

    fun addFail(thr: Throwable?) {
        exception.addSuppressed(thr)
        while (true) {
            val observedCounter = counter.get()
            val nextCounter = observedCounter + 1
            if (counter.compareAndSet(observedCounter, nextCounter)) {
                if (nextCounter == futuresSize) {
                    if (success.get() != null) {
                        f.complete(success.get())
                    } else {
                        f.completeExceptionally(exception)
                    }
                }
                return
            }
        }
    }
}

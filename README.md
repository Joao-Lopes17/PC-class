# Concurrent Programming Exercises

This repository contains the solutions developed throughout the **Concurrent Programming** course.  
The work is organized into three main sets of exercises, each focusing on different aspects of multithreading, synchronization, and concurrent systems in Kotlin/Java.

---

## 📂 Set 1 – Custom Synchronizers and Thread Management
In this set, several **thread-safe synchronizers** were implemented from scratch, without relying on Java's built-in concurrency utilities. Each synchronizer was tested with dedicated programs to validate its correctness.

### Implemented components
1. **CountDownLatch**  
   - Custom implementation with similar semantics to Java’s `CountDownLatch`.

2. **Exchanger**  
   - Thread-safe data exchanger for two threads to synchronize and swap elements.

3. **BlockingMessageQueue<T>**  
   A bounded blocking FIFO message queue supporting multiple producers and consumers.  
   - Blocking enqueue/dequeue with timeout.  
   - Fairness guaranteed for both messages and waiting consumers.  
   - Sensitive to thread interruptions.  

4. **ThreadPoolExecutor**  
   - Dynamic pool of worker threads, with `maxThreadPoolSize` and `keepAliveTime`.  
   - Implements `execute`, `shutdown`, and `awaitTermination`.  
   - Supports task rejection after shutdown.  

5. **Race Function**  
   ```kotlin
   fun <T> race(suppliers: List<()->T>, timeout: Duration): T?
   ```
   - Runs all suppliers in parallel virtual threads, returns the first produced value, and cancels the rest.

## 📂 Set 2 – Lock-Free Thread-Safe Classes and Futures

This set focused on building lock-free thread-safe data structures and working with CompletableFuture.

### Implemented components

1. **SafeSuccession<T>**
    - A thread-safe version of a sequential element provider (next()), implemented without locks.

2. **SafeResourceManager**
    - A lock-free manager that closes an AutoCloseable when its usage count reaches zero.

3. **scopedAny Function**
   ```
   fun <T> scopedAny(
    futures: List<CompletableFuture<T>>,
    onSuccess: (T) -> Unit
    ): CompletableFuture<T>
   ```
   - Completes only after all futures finish.
   - Returns the first successful result.
   - If all fail, throws an aggregated exception.
   - Calls onSuccess at most once.
   - Lock acquisition minimized.



## 📂 Set 3 – Coroutines and Publish-Subscribe Server

This set explored asynchronous programming with Kotlin coroutines and the design of a distributed system.

### Implemented components

1. Suspend extensions for NIO channels
    - Non-blocking connect, accept, read, and write.
    - Support for cancellation and synchronous-like API.

2. MessageQueue<T>
    - FIFO queue for coroutine communication.
    - Suspendable enqueue/dequeue with cancellation support.

3. Publish-Subscribe Server
    - Clients interact via TCP/IP, sending requests as text lines.
    - Supported commands:
        - SUBSCRIBE topics...
        - UNSUBSCRIBE topics...
        - PUBLISH topic message
    - Messages are broadcast to subscribers in real time.
    - Uses coroutines for scalability (not thread-per-client).


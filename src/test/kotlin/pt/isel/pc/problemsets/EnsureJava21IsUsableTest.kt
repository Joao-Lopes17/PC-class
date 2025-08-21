package pt.isel.pc.problemsets

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EnsureJava21IsUsableTest {
    @Test
    fun `can use virtual thread`() {
        var shared: Int = 1
        val th = Thread.ofVirtual().start {
            shared += 1
        }
        th.join()
        assertEquals(2, shared)
    }
}
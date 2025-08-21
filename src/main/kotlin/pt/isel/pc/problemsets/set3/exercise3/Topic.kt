package pt.isel.pc.problemsets.set3.exercise3

/**
 * Represents a topic and the subscribers to those topics.
 *
 */
class Topic(
    val name: TopicName,
) {
    val subscribers = mutableSetOf<Subscriber>()
    val messages = mutableListOf<String>()

    fun addMessage(message: String) {
        messages.add(message)
    }
}

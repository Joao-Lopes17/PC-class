package pt.isel.pc.problemsets.set3.exercise3

import pt.isel.pc.problemsets.set3.exercise3.protocol.ClientRequest
import pt.isel.pc.problemsets.set3.exercise3.protocol.ClientRequestError
import pt.isel.pc.problemsets.set3.exercise3.protocol.ClientResponse
import pt.isel.pc.problemsets.set3.exercise3.protocol.ServerPush
import pt.isel.pc.problemsets.set3.exercise3.protocol.parseClientRequest
import pt.isel.pc.problemsets.set3.exercise3.protocol.serialize
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseTests {

    @Test
    fun `test success parse cases`() {
        assertEquals(
            ClientRequest.Publish(TopicName("t1"), "hello world"),
            parseClientRequest("PUBLISH t1 hello world").successOrThrow,
        )
        assertEquals(
            ClientRequest.Publish(TopicName("t1"), "hello world"),
            parseClientRequest(" PUBLISH t1 hello world").successOrThrow,
        )
        assertEquals(
            ClientRequest.Publish(TopicName("t1"), " hello world"),
            parseClientRequest("PUBLISH t1  hello world").successOrThrow,
        )
        assertEquals(
            ClientRequest.Publish(TopicName("t1"), " hello world  "),
            parseClientRequest("PUBLISH t1  hello world  ").successOrThrow,
        )
        assertEquals(
            ClientRequest.Publish(TopicName("t1"), ""),
            parseClientRequest("PUBLISH t1").successOrThrow,
        )
        assertEquals(
            ClientRequest.Publish(TopicName("t1"), ""),
            parseClientRequest("PUBLISH t1 ").successOrThrow,
        )
        assertEquals(
            ClientRequest.Publish(TopicName("t1"), ""),
            parseClientRequest("  PUBLISH t1 ").successOrThrow,
        )
    }

    @Test
    fun `test error parse cases`() {
        assertEquals(
            ClientRequestError.MissingCommandName,
            parseClientRequest("").errorOrThrow,
        )
        assertEquals(
            ClientRequestError.MissingCommandName,
            parseClientRequest(" ").errorOrThrow,
        )
        assertEquals(
            ClientRequestError.UnknownCommandName,
            parseClientRequest("NOT-A-COMMAND").errorOrThrow,
        )
        assertEquals(
            ClientRequestError.InvalidArguments,
            parseClientRequest("PUBLISH").errorOrThrow,
        )
        assertEquals(
            ClientRequestError.InvalidArguments,
            parseClientRequest("SUBSCRIBE").errorOrThrow,
        )
    }

    @Test
    fun `test toString`() {
        assertEquals(
            "+ 0\n",
            serialize(ClientResponse.OkPublish(0)),
        )

        assertEquals(
            "+\n",
            serialize(ClientResponse.OkSubscribe),
        )

        assertEquals(
            "+\n",
            serialize(ClientResponse.OkUnsubscribe),
        )

        assertEquals(
            "-INVALID_ARGUMENTS\n",
            serialize(ClientResponse.Error(ClientRequestError.InvalidArguments)),
        )

        assertEquals(
            ">the-topic the content\n",
            serialize(
                ServerPush.PublishedMessage(
                    PublishedMessage(TopicName("the-topic"), "the content"),
                ),
            ),
        )
    }
}
package net.alfss.demowsclient

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.alfss.demowscommon.entities.WsInputMessageEntity
import net.alfss.demowscommon.entities.WsInputMessageTypeEntity
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration

fun main(args: Array<String>) {
    val client = ReactorNettyWebSocketClient()
    val mapper = jacksonObjectMapper()

    val commandList = listOf(
        WsInputMessageEntity(
            typeMessage = WsInputMessageTypeEntity.SUBSCRIBE,
            payload = "tours/9d1cfe43-b917-4a22-8406-2494bedf3f72"
        ),
        WsInputMessageEntity(
            typeMessage = WsInputMessageTypeEntity.SUBSCRIBE,
            payload = "tours/9d1cfe43-b917-4a22-8406-2494bedf3f71"
        ),
        WsInputMessageEntity(
            typeMessage = WsInputMessageTypeEntity.UNSUBSCRIBE,
            payload = "tours/9d1cfe43-b917-4a22-8406-2494bedf3f71"
        ),
        WsInputMessageEntity(
            typeMessage = WsInputMessageTypeEntity.PONG,
            payload = "PONG"
        )
    )

    val cliexc0 = client.execute(
        URI.create("ws://localhost:8000/ws")
    ) { session ->
        session.send(
            Flux.fromIterable(commandList)
                .map { mapper.writeValueAsString(it) }
                .map(session::textMessage)
        )
            .thenMany(session.receive()
                .map { msg -> msg.payloadAsText }
                .log())
            .then()
    }

    cliexc0.block(Duration.ofSeconds(1000L))
}
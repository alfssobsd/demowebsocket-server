package net.alfss.demowsclient

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.alfss.demowscommon.entities.WsInputMessageEntity
import net.alfss.demowscommon.entities.WsInputMessageTypeEntity
import net.alfss.demowscommon.entities.WsOutputMessageEntity
import net.alfss.demowscommon.entities.WsOutputMessageTypeEntity
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.IOException
import java.net.URI
import java.time.Duration

val mapper = jacksonObjectMapper()

fun main(args: Array<String>) {
    val client = ReactorNettyWebSocketClient()


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
            typeMessage = WsInputMessageTypeEntity.SUBSCRIBE,
            payload = "tours/9d1cfe43-b917-4a22-8406-2494bed88888"
        ),
        WsInputMessageEntity(
            typeMessage = WsInputMessageTypeEntity.UNSUBSCRIBE,
            payload = "tours/9d1cfe43-b917-4a22-8406-2494bedf3f71"
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
                .map { toMessage(it.payloadAsText) }
                .doOnEach {
                    when (it.get()!!.typeMessage) {
                        WsOutputMessageTypeEntity.PING -> {
                            session.send(
                                Mono.just(
                                    WsInputMessageEntity(
                                        typeMessage = WsInputMessageTypeEntity.PONG,
                                        payload = "PONG"
                                    )
                                ).map { mapper.writeValueAsString(it) }
                                    .map(session::textMessage)
                            ).subscribe()
                            println(it.get())
                        }
                        else -> println(it.get())
                    }
                }
                .log())
            .then()
    }

    cliexc0.block(Duration.ofSeconds(1000L))
}


fun toMessage(payload: String): WsOutputMessageEntity {
    try {
        return mapper.readValue(payload, WsOutputMessageEntity::class.java)
    } catch (e: IOException) {
        throw RuntimeException("Invalid JSON:$payload", e)
    }
}
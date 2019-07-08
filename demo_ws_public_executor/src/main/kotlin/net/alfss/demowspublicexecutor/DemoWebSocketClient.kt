import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.alfss.demowscommon.entities.WsCommandEntity
import net.alfss.demowscommon.entities.WsCommandTypeEntity
import net.alfss.demowscommon.entities.WsInputMessageEntity
import net.alfss.demowscommon.entities.WsInputMessageTypeEntity
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration

fun main(args: Array<String>) {
    val client = ReactorNettyWebSocketClient()
    val mapper = jacksonObjectMapper()

    val subscribeCommand = WsInputMessageEntity(
        typeMessage = WsInputMessageTypeEntity.SUBSCRIBE,
        payload = "tours/9d1cfe43-b917-4a22-8406-2494bedf3f72"
    )
    val json = mapper.writeValueAsString(subscribeCommand)

    val cliexc0 = client.execute(
        URI.create("ws://localhost:8080/ws")
    ) { session ->
        session.send(
            Mono.just(session.textMessage(json))
        )
            .thenMany(session.receive()
                .map { msg -> msg.payloadAsText }
                .log())
            .then()
    }

//    val cliexc1 = client.execute(
//        URI.create("ws://localhost:8080/ws")
//    ) { session ->
//        session.send(
//            Mono.just(session.textMessage(json))
//        )
//            .thenMany(session.receive()
//                .map { msg -> msg.payloadAsText }
//                .log())
//            .then()
//    }


    cliexc0.block(Duration.ofSeconds(1000L))
}
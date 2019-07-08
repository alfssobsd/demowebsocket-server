package net.alfss.demowspublicexecutor.usercases

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class KeepAliveUseCase {

    @Value("\${app.ws.keepalive-timeout-in-seconds}")
    val keepAliveMessageTimeout = 30L

    private val logger = LoggerFactory.getLogger(SubcribeChannelUseCase::class.java)

    fun execute(session: WebSocketSession, connQueueName: String): Mono<Void> {
        return session.send(
            Flux.interval(Duration.ofSeconds(keepAliveMessageTimeout))
                .map { "PING" }
                .map(session::textMessage)
                .doOnNext {
                    logger.info("Ping $connQueueName")
                }
        )

    }
}
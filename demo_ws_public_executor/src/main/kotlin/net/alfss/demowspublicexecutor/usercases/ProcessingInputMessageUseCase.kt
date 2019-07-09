package net.alfss.demowspublicexecutor.usercases

import net.alfss.demowscommon.entities.WsInputMessageTypeEntity
import net.alfss.demowscommon.utils.WsInputMessageMapperUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@Service
class ProcessingInputMessageUseCase(
    private val subscribeChannelUseCase: SubcribeChannelUseCase,
    private val unsubscribeUseCase: UnsubscribeUseCase,
    private val wsInputMessageMapper: WsInputMessageMapperUtil
) {
    private val logger = LoggerFactory.getLogger(ProcessingInputMessageUseCase::class.java)

    @Value("\${app.ws.prefix-channel}")
    val prefixChannel = "channel-"

    @Value("\${app.ws.prefix-channel-history}")
    val prefixChannelHistory = "channel-history-"

    fun execute(session: WebSocketSession, connQueueName: String): Mono<Void> {
        logger.info("Processing input message")
        return session.receive()
            .filter { it.type == WebSocketMessage.Type.TEXT }
            .map { wsInputMessageMapper.toMessage(it.payloadAsText) }
            .doOnNext {
                logger.info("Processing type=${it.typeMessage}, payload=${it.payload}")
                when (it.typeMessage) {
                    WsInputMessageTypeEntity.SUBSCRIBE -> {
                        subscribeChannelUseCase.execute(
                            session = session,
                            connQueueName = connQueueName,
                            channelName = "$prefixChannel${it.payload}",
                            historyChannelName = "$prefixChannelHistory${it.payload}"
                        ).subscribe()

                    }
                    WsInputMessageTypeEntity.UNSUBSCRIBE -> unsubscribeUseCase.execute(
                        session = session,
                        connQueueName = connQueueName,
                        channelName = "$prefixChannel${it.payload}"
                    ).subscribe()
                    WsInputMessageTypeEntity.PONG -> logger.info("received PONG")
                }
            }.log().then()
    }
}
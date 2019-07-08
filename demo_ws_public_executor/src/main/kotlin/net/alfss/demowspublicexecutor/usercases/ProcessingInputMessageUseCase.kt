package net.alfss.demowspublicexecutor.usercases

import net.alfss.demowscommon.entities.WsInputMessageTypeEntity
import net.alfss.demowscommon.utils.WsInputMessageMapperUtil
import net.alfss.demowspublicexecutor.entrypoints.websocket.WsController
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@Service
class ProcessingInputMessageUseCase(
    private val subscribeChannelUseCase: SubcribeChannelUseCase,
    private val wsInputMessageMapper: WsInputMessageMapperUtil
) {
    private val logger = LoggerFactory.getLogger(ProcessingInputMessageUseCase::class.java)

    //TODO:Переписать обработчик
    fun execute(session: WebSocketSession, connQueueName: String): Mono<Void> {
        return session.receive()
            .filter { it.type == WebSocketMessage.Type.TEXT }
            .map { wsInputMessageMapper.toMessage(it.payloadAsText) }
            .doOnNext {
                logger.info("Processing type=${it.typeMessage}, payload=${it.payload}")
                when (it.typeMessage) {
                    WsInputMessageTypeEntity.SUBSCRIBE -> subscribeChannelUseCase.execute(
                        session,
                        connQueueName,
                        it.payload
                    ).subscribe()
                    WsInputMessageTypeEntity.UNSUBSCRIBE -> logger.info("UNSUBSCRIBE")
                    WsInputMessageTypeEntity.PONG -> logger.info("PONG")
                }
            }.then()
    }
}
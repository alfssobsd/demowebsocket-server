package net.alfss.demowspublicexecutor.entrypoints.websocket

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.alfss.demowscommon.entities.*
import net.alfss.demowscommon.utils.InternalMessageMapperUtil
import net.alfss.demowscommon.utils.WsInputMessageMapperUtil
import net.alfss.demowspublicexecutor.usercases.KeepAliveUseCase
import net.alfss.demowspublicexecutor.usercases.ListenOuputQueueUseCase
import net.alfss.demowspublicexecutor.usercases.ProcessingInputMessageUseCase
import net.alfss.demowspublicexecutor.usercases.SubcribeChannelUseCase
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.util.*

@Component
class WsController(
    private val internalMessageMapper: InternalMessageMapperUtil,
    private val listenOuputQueueUseCase: ListenOuputQueueUseCase,
    private val keepAliveUseCase: KeepAliveUseCase,
    private val processingInputMessageUseCase: ProcessingInputMessageUseCase
) : WebSocketHandler {

    private val logger = LoggerFactory.getLogger(WsController::class.java)
    val mapper = jacksonObjectMapper()

    @Value("\${app.ws.prefix-connection-queue}")
    val prefixConnQueue = "connection-"

    override fun handle(session: WebSocketSession): Mono<Void> {
        val connQueueName = "$prefixConnQueue${UUID.randomUUID()}"

        val listenOutputQueue = listenOuputQueueUseCase.execute(session, connQueueName).then()
        val keepAlive = keepAliveUseCase.execute(session, connQueueName).then()
        val input = processingInputMessageUseCase.execute(session, connQueueName).then()

        return Mono.zip(listenOutputQueue, input, keepAlive).then()
    }
}
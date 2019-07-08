package net.alfss.demowspublicexecutor.usercases

import net.alfss.demowscommon.entities.InternalMessageEntity
import net.alfss.demowscommon.entities.InternalMessageTypeEntity
import net.alfss.demowscommon.utils.InternalMessageMapperUtil
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ListenOuputQueueUseCase(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val internalMessageMapper: InternalMessageMapperUtil
    ) {

    private val logger = LoggerFactory.getLogger(ListenOuputQueueUseCase::class.java)

    fun execute(session: WebSocketSession, connQueueName: String): Flux<InternalMessageEntity> {
        logger.info("Start listening queue $connQueueName")
        return redisTemplate.listenToChannel(connQueueName)
            .map { internalMessageMapper.toMessage(it.message) }
            .doOnNext{
                logger.info("Received message from $connQueueName, $it")
                when(it.typeMessage) {
                    InternalMessageTypeEntity.COMMAND_DISCONNECT -> session.close().subscribe()
                    InternalMessageTypeEntity.TEXT_MESSAGE -> session.send(Mono.just(session.textMessage(it.payload))).subscribe()
                    else -> logger.info("Unknown message type")
                }
            }
    }
}
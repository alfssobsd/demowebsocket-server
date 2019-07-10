package net.alfss.demowspublicexecutor.usercases

import net.alfss.demowscommon.entities.InternalMessageTypeEntity
import net.alfss.demowscommon.entities.WsOutputMessageEntity
import net.alfss.demowscommon.entities.WsOutputMessageTypeEntity
import net.alfss.demowscommon.utils.InternalMessageMapperUtil
import net.alfss.demowscommon.utils.WsOutputMessageMapperUtil
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Range
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.sql.Timestamp

@Service
class UnsubscribeUseCase(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val internalMessageMapper: InternalMessageMapperUtil,
    private val wsOutputMessageMapper: WsOutputMessageMapperUtil
) {
    private val logger = LoggerFactory.getLogger(SubcribeChannelUseCase::class.java)

    fun execute(session: WebSocketSession, connQueueName: String, channelName: String): Mono<Void> {
        logger.info("Removing subscription conn = $connQueueName channel = $channelName")

        val sendResponseUnsubscribe = session.send(
            Mono.just(
                WsOutputMessageEntity(
                    typeMessage = WsOutputMessageTypeEntity.RESPONSE_UNSUBSCRIBE,
                    payload = "UNSUBSCRIBE_OK($channelName)",
                    code = 200
                )
            )
                .map { wsOutputMessageMapper.toJson(it) }
                .map(session::textMessage)
        )

        val removeSubscription = redisTemplate.opsForZSet()
            .range(channelName, Range(0L, -1L))
            .map { internalMessageMapper.toMessage(it) }
            .filter { it.typeMessage == InternalMessageTypeEntity.SUBSCRIPTION }
            .filter { it.payload == connQueueName }
            .doOnNext {
                redisTemplate.opsForZSet().remove(channelName, internalMessageMapper.toJson(it)).subscribe()
                logger.info(
                    "Subscription removed conn = $connQueueName channel = $channelName ${internalMessageMapper.toJson(
                        it
                    )}"
                )
            }.then()

        return sendResponseUnsubscribe.and(removeSubscription).then()
    }
}
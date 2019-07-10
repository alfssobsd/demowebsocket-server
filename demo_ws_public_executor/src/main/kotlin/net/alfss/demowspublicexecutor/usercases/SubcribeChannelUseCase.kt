package net.alfss.demowspublicexecutor.usercases

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import net.alfss.demowscommon.entities.InternalMessageEntity
import net.alfss.demowscommon.entities.InternalMessageTypeEntity
import net.alfss.demowscommon.entities.WsOutputMessageEntity
import net.alfss.demowscommon.entities.WsOutputMessageTypeEntity
import net.alfss.demowscommon.utils.InternalMessageMapperUtil
import net.alfss.demowscommon.utils.WsOutputMessageMapperUtil
import org.springframework.data.domain.Range
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import java.sql.Timestamp


@Service
class SubcribeChannelUseCase(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val internalMessageMapper: InternalMessageMapperUtil,
    private val wsOutputMessageMapper: WsOutputMessageMapperUtil,
    private val authorizeUseCase: AuthorizeUseCase
) {
    private val logger = LoggerFactory.getLogger(SubcribeChannelUseCase::class.java)

    fun execute(
        session: WebSocketSession,
        connQueueName: String,
        channelName: String,
        historyChannelName: String
    ): Mono<Void> {
        logger.info("Subscribe conn=$connQueueName channel = $channelName")

        if (!authorizeUseCase.execute(channelName)) {
            return session.send(
                Mono.just(
                    WsOutputMessageEntity(
                        typeMessage = WsOutputMessageTypeEntity.RESPONSE_SUBSCRIBE,
                        payload = "SUBSCRIBE_ACCESS_DENIED($channelName)",
                        code = 403
                    )
                )
                    .map { wsOutputMessageMapper.toJson(it) }
                    .map(session::textMessage)
            ).then()
        }

        val subscribeEntity = InternalMessageEntity(
            typeMessage = InternalMessageTypeEntity.SUBSCRIPTION,
            payload = connQueueName,
            createdAt = Timestamp(System.currentTimeMillis())
        )

        val subscribeToChannel = redisTemplate.opsForZSet()
            .add(channelName, internalMessageMapper.toJson(subscribeEntity), 1.0).log().then()

        val readHistoryChannel = redisTemplate.opsForZSet()
            .range(historyChannelName, Range(0L, -1L))
            .map { internalMessageMapper.toMessage(it) }
            .filter { it.typeMessage == InternalMessageTypeEntity.TEXT_MESSAGE }
            .doOnNext {
                session.send(
                    Mono.just(
                        WsOutputMessageEntity(
                            typeMessage = WsOutputMessageTypeEntity.DATA,
                            payload = it.payload,
                            code = 200
                        )
                    )
                        .map { entity -> wsOutputMessageMapper.toJson(entity) }
                        .map(session::textMessage)
                ).subscribe()
                logger.info("Received history message $it channel = $historyChannelName")
            }.then()

        val sendResponseSubscribe = session.send(
            Mono.just(
                WsOutputMessageEntity(
                    typeMessage = WsOutputMessageTypeEntity.RESPONSE_SUBSCRIBE,
                    payload = "SUBSCRIBE_OK($channelName)",
                    code = 200
                )
            )
                .map { wsOutputMessageMapper.toJson(it) }
                .map(session::textMessage)
        )

        return readHistoryChannel.and(subscribeToChannel).and(sendResponseSubscribe).then()
    }
}
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
import org.springframework.web.reactive.socket.WebSocketSession
import java.sql.Timestamp


@Service
class SubcribeChannelUseCase(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val internalMessageMapper: InternalMessageMapperUtil,
    private val wsOutputMessageMapper: WsOutputMessageMapperUtil
) {
    private val logger = LoggerFactory.getLogger(SubcribeChannelUseCase::class.java)

    fun execute(
        session: WebSocketSession,
        connQueueName: String,
        channelName: String,
        historyChannelName: String
    ): Mono<Void> {
        //call AuthorizeUseCase before
        logger.info("Subscribe conn=$connQueueName channel = $channelName")
        val subscribeEntity = InternalMessageEntity(
            typeMessage = InternalMessageTypeEntity.SUBSCRIPTION,
            payload = connQueueName,
            createdAt = Timestamp(System.currentTimeMillis())
        )

        val subscribeToChannel = redisTemplate.opsForList()
            .leftPush(channelName, internalMessageMapper.toJson(subscribeEntity)).log().then()

        val readHistoryChannel = session.send(redisTemplate.opsForList()
            .range(historyChannelName, 0, -1)
            .map { internalMessageMapper.toMessage(it) }
            .filter { it.typeMessage == InternalMessageTypeEntity.TEXT_MESSAGE }
            .map { WsOutputMessageEntity(typeMessage = WsOutputMessageTypeEntity.DATA, payload = it.payload) }
            .map {  wsOutputMessageMapper.toJson(it) }
            .map(session::textMessage)
            .doOnNext {
                logger.info("Received history message $it channel = $historyChannelName")
            }
        )

        val sendResponseSubscribe = session.send(
            Mono.just(WsOutputMessageEntity(typeMessage = WsOutputMessageTypeEntity.RESPONSE_SUBSCRIBE, payload = "SUBSCRIBE_OK($channelName)"))
                .map { wsOutputMessageMapper.toJson(it) }
                .map(session::textMessage)
        )

        return Mono.zip(sendResponseSubscribe, readHistoryChannel, subscribeToChannel).then()
    }
}
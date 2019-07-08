package net.alfss.demowspublicexecutor.usercases

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.alfss.demowscommon.entities.InternalMessageEntity
import net.alfss.demowscommon.entities.InternalMessageTypeEntity
import net.alfss.demowscommon.utils.InternalMessageMapperUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.reactive.socket.WebSocketSession
import java.sql.Timestamp


@Service
class SubcribeChannelUseCase(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val internalMessageMapper: InternalMessageMapperUtil
) {
    private val logger = LoggerFactory.getLogger(SubcribeChannelUseCase::class.java)
    val mapper = jacksonObjectMapper()

    @Value("\${app.ws.prefix-channel}")
    val prefixChannel = "channel-"

    @Value("\${app.ws.prefix-channel-history}")
    val prefixChannelHistory = "channel-history-"

    fun execute(session: WebSocketSession, connQueueName: String, channelName: String): Mono<Void> {
        //call AuthorizeUseCase before
        logger.info("Subscribe conn=$connQueueName to channel $channelName")
        val subscribeEntity = InternalMessageEntity(
            typeMessage = InternalMessageTypeEntity.SUBSCRIPTION,
            payload = connQueueName,
            createdAt = Timestamp(System.currentTimeMillis())
        )

        val subscribeToChannel = redisTemplate.opsForList()
            .leftPush("$prefixChannel$channelName", internalMessageMapper.toJson(subscribeEntity)).log().then()

        val readHistoryChannel = session.send(redisTemplate.opsForList()
            .range("$prefixChannelHistory$channelName", 0, -1)
            .map { internalMessageMapper.toMessage(it) }
            .filter { it.typeMessage == InternalMessageTypeEntity.TEXT_MESSAGE }
            .map { it.payload }
            .map(session::textMessage)
            .doOnNext {
                logger.info("Received message $it from history")
            }
        )

        return Mono.zip(readHistoryChannel, subscribeToChannel).then()
    }
}
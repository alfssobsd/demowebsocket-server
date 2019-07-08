package net.alfss.demowspublicexecutor.usercases

import net.alfss.demowscommon.entities.WsChannelSubscriptionEntity
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.alfss.demowscommon.entities.WsMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.reactive.socket.WebSocketSession
import java.io.IOException
import java.sql.Timestamp


@Service
class SubcribeChannelUseCase(
    private val redisTemplate: ReactiveRedisTemplate<String, String>
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
        val subscribeEntity =
            WsChannelSubscriptionEntity(connQueue = connQueueName, createdAt = Timestamp(System.currentTimeMillis()))

        val subscribeToChannel = redisTemplate.opsForList()
            .leftPush("$prefixChannel$channelName", toJSON(subscribeEntity)).log().then()

        val readHistoryChannel = session.send(redisTemplate.opsForList()
            .range("$prefixChannelHistory$channelName", 0, -1)
            .map{ toMessage(it).payload }
            .map(session::textMessage)
            .doOnNext{
                logger.info("preapare send $it")
            }
            .log()
        )

        return Mono.zip(readHistoryChannel, subscribeToChannel).then()
    }


    private fun toJSON(subscriptionEntity: WsChannelSubscriptionEntity): String {
        try {
            return mapper.writeValueAsString(subscriptionEntity)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }

    private fun toMessage(payload: String): WsMessage {
        try {
            return mapper.readValue(payload, WsMessage::class.java)
        } catch (e: IOException) {
            throw RuntimeException("Invalid Command JSON:$payload", e);
        }
    }
}
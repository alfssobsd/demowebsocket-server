package net.alfss.demowspushexecutor.usecases

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.alfss.demowscommon.entities.WsChannelSubscriptionEntity
import net.alfss.demowscommon.entities.WsMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.sql.Timestamp

@Service
class PushMessageUseCase(
    private val redisTemplate: ReactiveRedisTemplate<String, String>
) {
    private val logger = LoggerFactory.getLogger(PushMessageUseCase::class.java)
    val mapper = jacksonObjectMapper()

    @Value("\${app.ws.prefix-channel}")
    val prefixChannel = "channel-"

    @Value("\${app.ws.prefix-channel-history}")
    val prefixChannelHistory = "channel-history-"

    @Value("\${app.ws.max-connection-time-in-seconds}")
    val subscribeLifeTimeSeconds = 180L

    @Value("\${app.ws.max-history-life-time-in-seconds}")
    val historyLifeTimeSeconds = 30L

    fun execute(payload: String): Mono<Void> {
        val channelName = "tours/9d1cfe43-b917-4a22-8406-2494bedf3f72"
        var subscriptionPositionNumber = -1L
        var historyPositionNumber = -1L
        val currentTime = Timestamp(System.currentTimeMillis())
        val message = WsMessage(payload = payload, createdAt = currentTime)

        logger.info("push message to channel $prefixChannel$channelName")

        val saveHistory = redisTemplate.opsForList().range("$prefixChannelHistory$channelName", 0, -1)
            .map { toMessage(it) }
            .doOnNext {
                if (Timestamp(it.createdAt.time + historyLifeTimeSeconds * 1000) < currentTime) {
                    logger.info("Mark expired message ${it.createdAt} + $historyLifeTimeSeconds seconds < $currentTime")
                    historyPositionNumber -= 1L
                }
            }.doOnComplete {
                logger.info("[$prefixChannelHistory$channelName] removing expired messages")
                redisTemplate.opsForList().trim("$prefixChannelHistory$channelName", 0L, historyPositionNumber)
                    .subscribe()
                logger.info("[$prefixChannelHistory$channelName] add new message to history")
                redisTemplate.opsForList().leftPush("$prefixChannelHistory$channelName", messageToJson(message)).subscribe()
            }.log().then()

        val sendMessageToSubscribers = redisTemplate.opsForList()
            .range("$prefixChannel$channelName", 0, -1)
            .map { toSubscribeEntity(it) }
            .doOnNext {

                if (Timestamp(it.createdAt.time + subscribeLifeTimeSeconds * 1000) < currentTime) {
                    logger.info("Mark expired subscription ${it.createdAt} + $subscribeLifeTimeSeconds seconds < $currentTime")
                    subscriptionPositionNumber -= 1L
                }
                logger.info("[$prefixChannel$channelName] publish message to ${it.connQueue}")
                redisTemplate.convertAndSend(it.connQueue, messageToJson(message)).subscribe()
            }
            .doOnComplete {
                logger.info("[$prefixChannel$channelName] removing expired subscriptions")
                redisTemplate.opsForList().trim("$prefixChannel$channelName", 0L, subscriptionPositionNumber).subscribe()
            }.then()

        return saveHistory.and(sendMessageToSubscribers)
    }

    private fun toSubscribeEntity(payload: String): WsChannelSubscriptionEntity {
        try {
            return mapper.readValue(payload, WsChannelSubscriptionEntity::class.java)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }

    private fun messageToJson(message: WsMessage): String {
        try {
            return mapper.writeValueAsString(message)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }

    private fun toMessage(payload: String): WsMessage {
        try {
            return mapper.readValue(payload, WsMessage::class.java)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }
}
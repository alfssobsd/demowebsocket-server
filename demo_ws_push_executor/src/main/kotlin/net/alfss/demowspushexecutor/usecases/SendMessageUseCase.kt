package net.alfss.demowspushexecutor.usecases

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.alfss.demowscommon.entities.InternalMessageEntity
import net.alfss.demowscommon.entities.InternalMessageTypeEntity
import net.alfss.demowscommon.utils.InternalMessageMapperUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Range
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.sql.Timestamp

@Service
class SendMessageUseCase(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val internalMessageMapper: InternalMessageMapperUtil
) {
    private val logger = LoggerFactory.getLogger(SendMessageUseCase::class.java)
    val mapper = jacksonObjectMapper()

    @Value("\${app.ws.prefix-channel}")
    val prefixChannel = "channel-"

    @Value("\${app.ws.prefix-channel-history}")
    val prefixChannelHistory = "channel-history-"

    @Value("\${app.ws.max-connection-time-in-seconds}")
    val subscribeLifeTimeSeconds = 180L

    @Value("\${app.ws.max-history-life-time-in-seconds}")
    val historyLifeTimeSeconds = 30L

    fun execute(channel: String, payload: String): Mono<Void> {
        val channelName = "$prefixChannel$channel"
        val historyChannelName = "$prefixChannelHistory$channel"
        val currentTime = Timestamp(System.currentTimeMillis())
        val message = InternalMessageEntity(
            typeMessage = InternalMessageTypeEntity.TEXT_MESSAGE,
            payload = payload,
            createdAt = currentTime
        )

        val appendHistoryChannel = addMessageToHistory(historyChannelName, message)
        val removeExpiredMessages = removeExpiredMessagesFromHistory(historyChannelName, currentTime)

        val removeExpiredSubscriptions = removeExpiredSubscriptions(channelName, currentTime)
        val sendMessageToSubscribers = sendMessageToSubscribers(channelName, message)

        return appendHistoryChannel
            .and(removeExpiredMessages)
            .and(sendMessageToSubscribers)
            .and(removeExpiredSubscriptions)
    }

    private fun addMessageToHistory(historyChannelName: String, message: InternalMessageEntity): Mono<Void> {
        logger.info("Adding $message to history channel = $historyChannelName")
        return redisTemplate.opsForZSet().add(historyChannelName, internalMessageMapper.toJson(message), 1.0).then()
    }

    private fun sendMessageToSubscribers(channelName: String, message: InternalMessageEntity): Mono<Void> {
        return redisTemplate.opsForZSet()
            .range(channelName, Range(0L, -1L))
            .map { internalMessageMapper.toMessage(it) }
            .filter { it.typeMessage == InternalMessageTypeEntity.SUBSCRIPTION }
            .doOnNext {
                logger.info("Sending message $message conn = ${it.payload} channel = $channelName")
                redisTemplate.convertAndSend(it.payload, internalMessageMapper.toJson(message)).subscribe()
            }.then()
    }

    private fun removeExpiredMessagesFromHistory(historyChannelName: String, currentTime: Timestamp): Mono<Void> {
        return redisTemplate.opsForZSet().range(historyChannelName, Range(0L, -1L))
            .map { internalMessageMapper.toMessage(it) }
            .filter { it.typeMessage == InternalMessageTypeEntity.TEXT_MESSAGE }
            .doOnNext {
                if (Timestamp(it.createdAt.time + historyLifeTimeSeconds * 1000) < currentTime) {
                    logger.info("Removing expired messages channel = $historyChannelName  ${it.createdAt} + $historyLifeTimeSeconds seconds < $currentTime")
                    redisTemplate.opsForZSet().remove(historyChannelName, internalMessageMapper.toJson(it)).subscribe()
                }
            }.then()
    }

    private fun removeExpiredSubscriptions(channelName: String, currentTime: Timestamp): Mono<Void> {
        val disconnectMessage = InternalMessageEntity(
            typeMessage = InternalMessageTypeEntity.COMMAND_DISCONNECT,
            payload = "DISCONNECT",
            createdAt = currentTime
        )
        return redisTemplate.opsForZSet()
            .range(channelName, Range(0L, -1L))
            .map { internalMessageMapper.toMessage(it) }
            .filter { it.typeMessage == InternalMessageTypeEntity.SUBSCRIPTION }
            .doOnNext {
                if (Timestamp(it.createdAt.time + subscribeLifeTimeSeconds * 1000) < currentTime) {
                    logger.info("Removing expired subscriptions channel = $channelName ${it.createdAt} + $subscribeLifeTimeSeconds seconds < $currentTime")

                    //Стоит ли вообще вышибать клиента или сделать лучше обнавление подписки?
                    logger.info("Sending disconnect command conn =  ${it.payload}")
                    redisTemplate.convertAndSend(it.payload, internalMessageMapper.toJson(disconnectMessage))
                        .subscribe()
                    redisTemplate.opsForZSet().remove(channelName, internalMessageMapper.toJson(it)).subscribe()
                }
            }.then()

    }
}
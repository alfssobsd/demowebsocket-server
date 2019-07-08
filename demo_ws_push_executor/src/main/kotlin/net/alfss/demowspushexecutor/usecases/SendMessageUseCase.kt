package net.alfss.demowspushexecutor.usecases

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.alfss.demowscommon.entities.InternalMessageEntity
import net.alfss.demowscommon.entities.InternalMessageTypeEntity
import net.alfss.demowscommon.utils.InternalMessageMapperUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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

    fun execute(payload: String): Mono<Void> {
        val channel = "tours/9d1cfe43-b917-4a22-8406-2494bedf3f72"
        val channelName = "$prefixChannel$channel"
        val historyChannelName = "$prefixChannelHistory$channelName"
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
        logger.info("[$historyChannelName] add new message to history")
        return redisTemplate.opsForList().leftPush(historyChannelName, internalMessageMapper.toJson(message)).then()
    }

    private fun sendMessageToSubscribers(channelName: String, message: InternalMessageEntity): Mono<Void> {
        return redisTemplate.opsForList()
            .range(channelName, 0, -1)
            .map { internalMessageMapper.toMessage(it) }
            .filter { it.typeMessage == InternalMessageTypeEntity.SUBSCRIPTION }
            .doOnNext {
                logger.info("[$prefixChannel$channelName] send message to ${it.payload}")
                redisTemplate.convertAndSend(it.payload, internalMessageMapper.toJson(message)).subscribe()
            }.then()
    }

    private fun removeExpiredMessagesFromHistory(historyChannelName: String, currentTime: Timestamp): Mono<Void> {
        var historyPositionNumber = -1L
        return redisTemplate.opsForList().range(historyChannelName, 0, -1)
            .map { internalMessageMapper.toMessage(it) }
            .filter { it.typeMessage == InternalMessageTypeEntity.TEXT_MESSAGE }
            .doOnNext {
                if (Timestamp(it.createdAt.time + historyLifeTimeSeconds * 1000) < currentTime) {
                    logger.info("Mark expired message ${it.createdAt} + $historyLifeTimeSeconds seconds < $currentTime")
                    historyPositionNumber -= 1L
                }
            }.doOnComplete {
                logger.info("[$historyChannelName] removing expired messages")
                redisTemplate.opsForList().trim(historyChannelName, 0L, historyPositionNumber)
                    .subscribe()
            }.then()
    }

    private fun removeExpiredSubscriptions(channelName: String, currentTime: Timestamp): Mono<Void> {
        var subscriptionPositionNumber = -1L
        val disconnectMessage = InternalMessageEntity(
            typeMessage = InternalMessageTypeEntity.COMMAND_DISCONNECT,
            payload = "DISCONNECT",
            createdAt = currentTime
        )
        return redisTemplate.opsForList()
            .range(channelName, 0, -1)
            .map { internalMessageMapper.toMessage(it) }
            .filter { it.typeMessage == InternalMessageTypeEntity.SUBSCRIPTION }
            .doOnNext {
                if (Timestamp(it.createdAt.time + subscribeLifeTimeSeconds * 1000) < currentTime) {
                    logger.info("Mark expired subscription ${it.createdAt} + $subscribeLifeTimeSeconds seconds < $currentTime")
                    subscriptionPositionNumber -= 1L

                    //Стоит ли вообще вышибать клиента или сделать лучше обнавление подписки?
                    logger.info("Send disconnect command to ${it.payload}")
                    redisTemplate.convertAndSend(it.payload, internalMessageMapper.toJson(disconnectMessage))
                        .subscribe()
                }
            }
            .doOnComplete {
                logger.info("[$channelName] removing expired subscriptions")
                redisTemplate.opsForList().trim(channelName, 0L, subscriptionPositionNumber)
                    .subscribe()
            }.then()

    }
}
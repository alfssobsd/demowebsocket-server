package net.alfss.demowscommon.utils

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.alfss.demowscommon.entities.InternalMessageEntity
import net.alfss.demowscommon.entities.WsInputMessageEntity
import net.alfss.demowscommon.entities.WsOutputMessageEntity
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class WsOutputMessageMapperUtil {
    val mapper = jacksonObjectMapper()

    fun toMessage(payload: String): WsOutputMessageEntity {
        try {
            return mapper.readValue(payload, WsOutputMessageEntity::class.java)
        } catch (e: IOException) {
            throw RuntimeException("Invalid JSON:$payload", e)
        }
    }

    fun toJson(messageEntity: WsOutputMessageEntity): String {
        try {
            return mapper.writeValueAsString(messageEntity)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }
}
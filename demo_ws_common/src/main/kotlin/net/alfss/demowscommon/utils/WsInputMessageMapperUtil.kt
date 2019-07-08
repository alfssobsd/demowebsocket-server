package net.alfss.demowscommon.utils

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.alfss.demowscommon.entities.InternalMessageEntity
import net.alfss.demowscommon.entities.WsInputMessageEntity
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class WsInputMessageMapperUtil {
    val mapper = jacksonObjectMapper()

    fun toMessage(payload: String): WsInputMessageEntity {
        try {
            return mapper.readValue(payload, WsInputMessageEntity::class.java)
        } catch (e: IOException) {
            throw RuntimeException("Invalid JSON:$payload", e);
        }
    }

    fun toJson(messageEntity: WsInputMessageEntity): String {
        try {
            return mapper.writeValueAsString(messageEntity)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }
}
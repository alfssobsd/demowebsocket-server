package net.alfss.demowscommon.utils

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.alfss.demowscommon.entities.InternalMessageEntity
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class InternalMessageMapperUtil {
    val mapper = jacksonObjectMapper()

    fun toMessage(payload: String): InternalMessageEntity {
        try {
            return mapper.readValue(payload, InternalMessageEntity::class.java)
        } catch (e: IOException) {
            throw RuntimeException("Invalid JSON:$payload", e);
        }
    }

    fun toJson(messageEntity: InternalMessageEntity): String {
        try {
            return mapper.writeValueAsString(messageEntity)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }
}
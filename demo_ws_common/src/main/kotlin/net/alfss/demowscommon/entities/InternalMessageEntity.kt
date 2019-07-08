package net.alfss.demowscommon.entities

import com.fasterxml.jackson.annotation.JsonProperty
import java.sql.Timestamp

/**
 * Data class for internal exchange
 * like command to close connection or store subscription, messages in channels
 * */
data class InternalMessageEntity (
    @JsonProperty("type") val typeMessage: InternalMessageTypeEntity,
    @JsonProperty("payload") val payload: String,
    @JsonProperty("created_at") val createdAt: Timestamp
)
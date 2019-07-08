package net.alfss.demowscommon.entities

import com.fasterxml.jackson.annotation.JsonProperty
import java.sql.Timestamp

@Deprecated("not used any more")
data class WsMessage(
    @JsonProperty("payload") val payload: String,
    @JsonProperty("created_at") val createdAt: Timestamp
)
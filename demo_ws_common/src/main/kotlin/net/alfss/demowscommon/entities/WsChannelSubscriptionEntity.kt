package net.alfss.demowscommon.entities

import com.fasterxml.jackson.annotation.JsonProperty
import java.sql.Timestamp

@Deprecated("not used any more")
data class WsChannelSubscriptionEntity(
    @JsonProperty("conn_queue") val connQueue: String,
    @JsonProperty("created_at")val createdAt: Timestamp
)
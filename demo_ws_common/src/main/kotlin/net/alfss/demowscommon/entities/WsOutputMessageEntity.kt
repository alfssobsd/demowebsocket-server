package net.alfss.demowscommon.entities

import com.fasterxml.jackson.annotation.JsonProperty

data class WsOutputMessageEntity (
    @JsonProperty("type") val typeMessage: WsOutputMessageTypeEntity,
    @JsonProperty("payload")val payload: String
)
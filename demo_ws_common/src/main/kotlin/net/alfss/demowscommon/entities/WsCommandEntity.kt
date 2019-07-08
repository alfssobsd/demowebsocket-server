package net.alfss.demowscommon.entities

import com.fasterxml.jackson.annotation.JsonProperty

@Deprecated("not used any more")
data class WsCommandEntity(
    @JsonProperty("type_command") val typeCommand: WsCommandTypeEntity,
    @JsonProperty("payload")val payload: String
)
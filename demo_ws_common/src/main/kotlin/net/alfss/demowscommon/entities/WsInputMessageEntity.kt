package net.alfss.demowscommon.entities

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Data class for send message from Client to Server
 * like subscribe,unsubscribe action
* */
data class WsInputMessageEntity(
    @JsonProperty("type") val typeMessage: WsInputMessageTypeEntity,
    @JsonProperty("payload")val payload: String
)
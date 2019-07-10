package net.alfss.demowspushexecutor.entrypoins.http.entities

import com.fasterxml.jackson.annotation.JsonProperty

data class HttpPushMessageEntity (
    @JsonProperty("channel") val channel: String,
    @JsonProperty("payload") val payload: String
)
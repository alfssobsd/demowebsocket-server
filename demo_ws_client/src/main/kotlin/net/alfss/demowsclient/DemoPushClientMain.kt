package net.alfss.demowsclient

import net.alfss.demowspushexecutor.entrypoins.http.entities.HttpPushMessageEntity
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.sql.Timestamp

fun main(args: Array<String>) {
    val client = WebClient.builder().baseUrl("http://localhost:8001").build()

    val message = HttpPushMessageEntity("tours/9d1cfe43-b917-4a22-8406-2494bedf3f72", Timestamp(System.currentTimeMillis()).toString())

    val result = client.post()
        .uri("/notification/message")
        .contentType(MediaType.APPLICATION_JSON)
        .syncBody(message)
        .retrieve()
        .bodyToMono(Void::class.java).log().then()

    result.block()
}
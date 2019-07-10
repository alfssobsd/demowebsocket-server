package net.alfss.demowspushexecutor.entrypoins.http

import net.alfss.demowspushexecutor.entrypoins.http.entities.HttpPushMessageEntity
import net.alfss.demowspushexecutor.usecases.SendMessageUseCase
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import javax.validation.Valid

@RestController
@RequestMapping("/notification")
class NotificationController(
    private val sendMessageUseCase: SendMessageUseCase
) {

    @PostMapping("/message")
    fun pushMessage(@Valid @RequestBody pushMessageEntity: HttpPushMessageEntity): Mono<Void> {
        return sendMessageUseCase.execute(channel = pushMessageEntity.channel, payload = pushMessageEntity.payload)
    }
}
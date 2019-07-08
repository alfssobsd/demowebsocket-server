package net.alfss.demowspushexecutor.entrypoins.http

import net.alfss.demowspushexecutor.usecases.SendMessageUseCase
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/notification")
class NotificationController(
    private val sendMessageUseCase: SendMessageUseCase
) {

    @PostMapping("/message")
    fun pushMessage(): Mono<Void> {
        return sendMessageUseCase.execute("test message!21212")
    }
}
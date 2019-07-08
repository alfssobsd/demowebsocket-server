package net.alfss.demowspushexecutor.entrypoins.http

import net.alfss.demowspushexecutor.usecases.PushMessageUseCase
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/push_message")
class PushMessageController(
    private val pushMessageUseCase: PushMessageUseCase
) {

    @PostMapping
    fun pushMessage(): Mono<Void> {
        return pushMessageUseCase.execute("test message!21212")
    }
}
package net.alfss.demowspublicexecutor.usercases

import org.springframework.stereotype.Service

@Service
class AuthorizeUseCase {

    fun execute(channelName:String): Boolean {
        when(channelName) {
            "channel-tours/9d1cfe43-b917-4a22-8406-2494bedf3f71" -> return true
            "channel-tours/9d1cfe43-b917-4a22-8406-2494bedf3f72" -> return true
            else -> return false
        }
    }
}
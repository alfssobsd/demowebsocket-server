package net.alfss.demowspushexecutor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@ComponentScan(basePackages = ["net.alfss.demowspushexecutor", "net.alfss.demowscommon"])
@SpringBootApplication
class DemoWsPushExecutorMain

fun main(args: Array<String>) {
    runApplication<DemoWsPushExecutorMain>(*args)
}
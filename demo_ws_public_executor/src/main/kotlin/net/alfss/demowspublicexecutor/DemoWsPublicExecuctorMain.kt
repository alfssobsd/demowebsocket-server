package net.alfss.demowspublicexecutor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan


@ComponentScan(basePackages = ["net.alfss.demowspublicexecutor", "net.alfss.demowscommon"])
@SpringBootApplication
class DemoWsPublicExecuctorMain

fun main(args: Array<String>) {
    runApplication<DemoWsPublicExecuctorMain>(*args)
}
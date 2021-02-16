package com.mdreamfever.lolicon

import com.mdreamfever.lolicon.config.LoliconConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@EnableConfigurationProperties(LoliconConfig::class)
@SpringBootApplication
class LoliconApplication

fun main(args: Array<String>) {
    runApplication<LoliconApplication>(*args)
}

package com.mdreamfever.lolicon.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import kotlin.properties.Delegates

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConfigurationProperties("lolicon")
class LoliconConfig {
    lateinit var apiKey: String
    var cache by Delegates.notNull<Int>()
    var size1200 by Delegates.notNull<Boolean>()
    var autoDeleteDownloadImage by Delegates.notNull<Boolean>()
    var reuseImage by Delegates.notNull<Boolean>()
    var confounding by Delegates.notNull<Boolean>()
}
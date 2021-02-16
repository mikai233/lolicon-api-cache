package com.mdreamfever.lolicon.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mdreamfever.lolicon.config.LoliconConfig
import com.mdreamfever.lolicon.utils.LoliconUtils
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
class SetuController {

    @Autowired
    private lateinit var loliconConfig: LoliconConfig

    @Autowired
    private lateinit var loliconUtils: LoliconUtils

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun info(): String {
        return ObjectMapper().writeValueAsString(
            mapOf(
                "调用接口" to "/setu",
                "参数" to "r18=true or r18=false 非必须 默认r18=false",
                "配置" to "/config or 应用目录yml"
            )
        )
    }

    @GetMapping("/config")
    fun config(
        @RequestParam(required = false) apikey: String?,
        @RequestParam(required = false) cache: Int?,
        @RequestParam(required = false) size1200: Boolean?,
        @RequestParam(required = false) autoDeleteDownloadImage: Boolean?,
        @RequestParam(required = false) confounding: Boolean?,
        @RequestParam(required = false) reuseImage: Boolean?,
    ): LoliconConfig {
        return loliconConfig.let {
            it.apiKey = apikey ?: loliconConfig.apiKey
            it.cache = cache ?: loliconConfig.cache
            it.size1200 = size1200 ?: true
            it.autoDeleteDownloadImage = autoDeleteDownloadImage ?: false
            it.confounding = confounding ?: true
            it.reuseImage = reuseImage ?: true
            it
        }
    }

    @GetMapping("/setu", produces = [MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE])
    fun setu(@RequestParam r18: Boolean = false): ByteArray? {
        return runBlocking {
            loliconUtils.getImage(r18)
        }
    }
}
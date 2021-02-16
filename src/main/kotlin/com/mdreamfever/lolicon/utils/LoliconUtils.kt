package com.mdreamfever.lolicon.utils

import com.mdreamfever.lolicon.config.LoliconConfig
import com.mdreamfever.lolicon.entity.ImageStatus
import com.mdreamfever.lolicon.entity.LoliconResponse
import com.mdreamfever.lolicon.entity.LoliconSetu
import com.mdreamfever.lolicon.entity.LoliconSetuDB
import com.mdreamfever.lolicon.repository.LoliconRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.RequestEntity
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.DigestUtils
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.annotation.PostConstruct
import kotlin.random.Random


@Component
class LoliconUtils {
    private var quota = 10
    private var quotaMinTtl = 0
    private var lastInvokeAt = System.currentTimeMillis()
    private val mutex = Mutex()
    private lateinit var r18Channel: Channel<LoliconSetu>
    private lateinit var nR18Channel: Channel<LoliconSetu>

    @Autowired
    private lateinit var loliconRepository: LoliconRepository
    private val logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var loliconConfig: LoliconConfig

    private lateinit var restTemplate: RestTemplate

    @ExperimentalCoroutinesApi
    @PostConstruct
    private fun init() {
        val factory = SimpleClientHttpRequestFactory()
        factory.apply {
            setConnectTimeout(60 * 1000)
            setReadTimeout(60 * 1000)
        }
        restTemplate = RestTemplate(factory)
        r18Channel = Channel(loliconConfig.cache)
        nR18Channel = Channel(loliconConfig.cache)
        GlobalScope.launch {
            launch {
                loliconDownloadImage()
            }
            launch {
                loliconPrepareImageInfo()
            }
            if (loliconConfig.autoDeleteDownloadImage) {
                launch {
                    deleteImage()
                }
            }
        }
    }

    /**
     * @param r18 0为非 R18，1为 R18，2为混合
     * @param keyword  若指定关键字，将会返回从插画标题、作者、标签中模糊搜索的结果
     * @param num 一次返回的结果数量，范围为1到10，不提供 APIKEY 时固定为1；在指定关键字的情况下，结果数量可能会不足指定的数量
     * @param proxy 设置返回的原图链接的域名，你也可以设置为disable来得到真正的原图链接
     * @return
     */
    private suspend fun requestImageInfo(
        r18: Int = 0,
        keyword: String = "",
        num: Int = 1,
        proxy: String = "i.pixiv.cat"
    ) {
        val apiKey = loliconConfig.apiKey
        val size1200 = loliconConfig.size1200
        if (quota <= 0) {
            logger.warn("当日调用额度已不足")
            delay(60 * 1000)
        }
        if (quotaMinTtl + lastInvokeAt < System.currentTimeMillis()) {
            delay(System.currentTimeMillis() - lastInvokeAt - quotaMinTtl)
            logger.info("requestImageInfo delay")
        }
        lastInvokeAt = System.currentTimeMillis()
        logger.info(
            "invoke api, api key: {}, size1200: {}, r18: {}, keyword: {}, num: {}, proxy: {}, quota: {}, quotaMinTtl: {}, lastInvokeAt: {}",
            apiKey,
            size1200,
            r18,
            keyword,
            num,
            proxy,
            quota,
            quotaMinTtl,
            Date(lastInvokeAt),
        )
        val result = restTemplate.getForEntity(
            "https://api.lolicon.app/setu/?apikey=${apiKey}&r18=${r18}&keyword=${keyword}&num=${num}&proxy=${proxy}&size1200=${size1200}",
            LoliconResponse::class.java
        )
        val body = result.body
        if (body != null) {
            logger.info("send image info to channel, size: {}", body.data.size)
            quota = body.quota
            quotaMinTtl = body.quotaMinTtl
            if (r18 == 0) {
                body.data.forEach {
                    nR18Channel.send(it)
                }
            } else {
                body.data.forEach {
                    r18Channel.send(it)
                }
            }
        }
    }

    private suspend fun downloadImage(url: String, save: Boolean = true): ByteArray? {
        val headers = HttpHeaders()
        headers["user-agent"] =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36"
        try {
            val requestEntity = RequestEntity.get(url).headers(headers).build()
            val result = restTemplate.exchange(requestEntity, ByteArray::class.java)
            val byteArray = result.body
            if (byteArray != null && save) {
                val hex = DigestUtils.md5DigestAsHex(url.toByteArray())
                val file = File("images/${hex}.${url.split(".").last()}")
                if (!file.parentFile.exists()) {
                    file.parentFile.mkdir()
                }
                val deferred = withContext(Dispatchers.IO) {
                    async(Dispatchers.IO) {
                        FileOutputStream(file).use {
                            it.write(byteArray)
                        }
                    }
                }
            }
            return byteArray
        } catch (e: Exception) {
            logger.error("download image failed, {}", e.message)
        }
        return null
    }

    private suspend fun loliconPrepareImageInfo() {
        coroutineScope {
            launch {
                while (true) {
                    requestImageInfo(r18 = 0, num = loliconConfig.cache)
                    delay(2000)
                }
            }
            launch {
                while (true) {
                    requestImageInfo(r18 = 1, num = loliconConfig.cache)
                    delay(2000)
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    private suspend fun loliconDownloadImage() {
        suspend fun doDownload(r18: Boolean) {
            while (true) {
                val unused = mutex.withLock { loliconRepository.findAllByImageStatus(ImageStatus.UNUSED) }
                val size = unused.filter { it.r == r18 }.size
                if (size < loliconConfig.cache) {
                    val channel = if (r18) r18Channel else nR18Channel
                    val loliconSetu = channel.receiveOrNull() ?: continue
                    logger.info("receive r18 image info from channel: {}", loliconSetu.title)
                    val byteArray = downloadImage(loliconSetu.url)
                    if (byteArray != null) {
                        saveImageDB(
                            LoliconSetuDB(
                                id = null,
                                imageStatus = ImageStatus.UNUSED,
                                r = loliconSetu.r18,
                                setu = loliconSetu
                            )
                        )
                        logger.info("save r18 image to directory images: {}", loliconSetu.toString())
                    }
                }
                delay(2000)
            }
        }
        coroutineScope {
            launch {
                doDownload(true)
            }
            launch {
                doDownload(false)
            }
        }
    }

    suspend fun getImage(r18: Boolean): ByteArray? {
        val loliconSetuDB = mutex.withLock {
            loliconRepository.findFirstByImageStatusAndR(
                imageStatus = ImageStatus.UNUSED,
                r = r18,
            )
        }
        return if (loliconSetuDB != null) {
            logger.info("get image: {}", loliconSetuDB.setu)
            mutex.withLock { loliconRepository.save(loliconSetuDB.copy(imageStatus = ImageStatus.USED)) }
            val hex = DigestUtils.md5DigestAsHex(loliconSetuDB.setu.url.toByteArray())
            val file = File("images/${hex}.${loliconSetuDB.setu.url.split(".").last()}")
            if (file.exists()) {
                val byteArray = file.inputStream().readBytes()
                if (loliconConfig.confounding) {
                    byteArray[byteArray.size - 1] = Random(System.currentTimeMillis()).nextBytes(1).first()
                }
                byteArray
            } else {
                null
            }
        } else {
            requestImageInfo(r18 = if (r18) 1 else 0)
            val loliconSetu = r18Channel.receive()
            val byteArray = downloadImage(loliconSetu.url)
            if (loliconConfig.confounding && byteArray != null) {
                byteArray[byteArray.size - 1] = Random(System.currentTimeMillis()).nextBytes(1).first()
            }
            byteArray
        }
    }

    private suspend fun saveImageDB(loliconSetuDB: LoliconSetuDB) {
        mutex.withLock {
            loliconRepository.save(
                loliconSetuDB
            )
        }
    }

    private suspend fun deleteImage() {
        while (true) {
            delay(60 * 1000)
            val usedImage = mutex.withLock {
                loliconRepository.findAllByImageStatus(ImageStatus.USED)
            }
            usedImage.forEach {
                val hex = DigestUtils.md5DigestAsHex(it.setu.url.toByteArray())
                val fileName = "${hex}.${it.setu.url.split(".").last()}"
                val imageFile = File("images/${fileName}")
                try {
                    logger.info("try to delete image: {}", fileName)
                    if (imageFile.exists()) {
                        imageFile.delete()
                        logger.info("image: {} delete success", fileName)
                    } else {
                        logger.warn("image: {} not exists", fileName)
                    }
                    mutex.withLock {
                        loliconRepository.delete(it)
                    }
                } catch (e: Exception) {
                    logger.error("{} delete failed, {}", fileName, e.message)
                }
            }
        }
    }
}
package com.mdreamfever.lolicon

import com.mdreamfever.lolicon.config.LoliconConfig
import com.mdreamfever.lolicon.utils.LoliconUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class LoliconApplicationTests {

    @Autowired
    private lateinit var loliconConfig: LoliconConfig

    @Autowired
    private lateinit var loliconUtils: LoliconUtils

    @Test
    fun contextLoads() {
    }
}

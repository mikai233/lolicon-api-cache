package com.mdreamfever.lolicon.repository

import com.mdreamfever.lolicon.entity.ImageStatus
import com.mdreamfever.lolicon.entity.LoliconSetuDB
import org.springframework.data.jpa.repository.JpaRepository

interface LoliconRepository : JpaRepository<LoliconSetuDB, Int> {
    fun findAllByImageStatus(
        imageStatus: ImageStatus
    ): List<LoliconSetuDB>

    fun findFirstByImageStatusAndR(
        imageStatus: ImageStatus, r: Boolean
    ): LoliconSetuDB?
}
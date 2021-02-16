package com.mdreamfever.lolicon.entity

import javax.persistence.*

enum class ImageStatus {
    UNUSED,
    USED,
}

@Entity
data class LoliconSetuDB(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int?,
    @Enumerated(EnumType.ORDINAL)
    val imageStatus: ImageStatus,
    val r: Boolean,
    @Embedded val setu: LoliconSetu
)

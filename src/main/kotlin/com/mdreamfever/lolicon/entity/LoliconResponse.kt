package com.mdreamfever.lolicon.entity

data class LoliconResponse(
    val code: Int,
    val msg: String,
    val quota: Int,
    val quotaMinTtl: Int,
    val count: Int,
    val data: List<LoliconSetu>
)

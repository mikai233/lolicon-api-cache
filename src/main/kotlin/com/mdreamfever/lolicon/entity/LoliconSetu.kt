package com.mdreamfever.lolicon.entity

import javax.persistence.ElementCollection
import javax.persistence.Embeddable

@Embeddable
data class LoliconSetu(
    var pid: Int,
    var p: Int,
    var uid: Int,
    var title: String,
    var author: String,
    var url: String,
    var r18: Boolean,
    var width: Int,
    var height: Int,
    @ElementCollection(targetClass = String::class)
    var tags: List<String>
)

package com.kuaigeglobal.epnova.core.network.apis.entity

data class FeedbackDetail(
    val id: String,
    val type: String,
    val submittedOn: String,
    val description: String,
    val imageUrls: List<String>,
    val status: String,
    val reply: String? = null,
    val replyOn: String? = null,
    val replyImageUrls: List<String> = emptyList()
)

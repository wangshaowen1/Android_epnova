package com.kuaigeglobal.epnova.core.network.apis.request


data class EditDramaBody(
    val dramasId : String?,
    val dramasName: String? = null,
    val scriptIntro: String? = null,
): UserTokenBody()
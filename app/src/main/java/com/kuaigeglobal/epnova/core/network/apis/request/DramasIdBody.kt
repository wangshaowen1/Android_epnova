package com.kuaigeglobal.epnova.core.network.apis.request

data class DramasIdBody(
    val dramasId: String?,
): UserTokenBody()

data class DramaContentBody(
    val dramasId: String?,
    val scriptContent: String?,
): UserTokenBody()


data class DramasSeriesId(
    val dramasSeriesId: String?,
    val dramasId: String?=null,
    val autoUnlockStatus: Int?=null
): UserTokenBody()
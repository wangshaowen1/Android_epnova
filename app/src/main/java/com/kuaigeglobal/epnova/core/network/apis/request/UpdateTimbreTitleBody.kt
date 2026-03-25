package com.kuaigeglobal.epnova.core.network.apis.request

data class UpdateTimbreTitleBody(
    val timbreTitle: String? = null,
    val userTimbreId: String? = null,
) : UserTokenBody()

data class UserTimbreIdBody(
    val userTimbreId: String? = null
) : UserTokenBody()
package com.kuaigeglobal.epnova.core.network.apis.request

data class FcmTokenBody(
    val fcmToken: String?,
) : UserTokenBody()
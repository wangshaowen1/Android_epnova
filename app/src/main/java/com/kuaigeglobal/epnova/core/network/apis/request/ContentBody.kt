package com.kuaigeglobal.epnova.core.network.apis.request

data class ContentBody(
    val content: String? = null,
) : UserTokenBody()

data class SaveScriptBody(
    val workId: String? = null,
    val storyId: String? = null,//当集得id
    val content: String? = null,
) : UserTokenBody()
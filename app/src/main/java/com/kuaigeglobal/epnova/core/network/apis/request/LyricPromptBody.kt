package com.kuaigeglobal.epnova.core.network.apis.request

data class LyricPromptBody(
    val lyricPrompt: String?,
    val musicGenConfigId: String?
) : UserTokenBody()
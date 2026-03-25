package com.kuaigeglobal.epnova.core.network.apis.response

data class LyricWithLineData(
    val musicLyric: String?,
    val status: Int?//状态 0-生成中，1-成功，2-失败
)
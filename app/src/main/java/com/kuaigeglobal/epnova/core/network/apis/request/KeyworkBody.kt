package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 搜索关键词（仅匹配作品标题，忽略大小写）
 */
data class KeyworkBody(
    val keyword: String? = null,
) : UserTokenBody()
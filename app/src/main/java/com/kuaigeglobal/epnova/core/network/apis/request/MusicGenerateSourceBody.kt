package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 歌曲生成来源上报
 */
data class MusicGenerateSourceBody(
    /**
     * 歌曲id
     */
    val musicId: String?,
    /**
     * 来源类型 [1=灵感写歌 2=DeepSeek写歌 3=爆款写歌 4=歌曲爆改示例歌曲弹窗]
     */
    val sourceType: Int?,
    /**
     * 来源id(来源类型=1、2时传灵感id =3时传歌曲爆改示例歌曲ID =4时传弹窗ID)
     */
    val sourceId: String?
) : UserTokenBody()
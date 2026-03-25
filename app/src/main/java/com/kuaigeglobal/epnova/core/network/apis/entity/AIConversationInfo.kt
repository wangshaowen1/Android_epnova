package com.kuaigeglobal.epnova.core.network.apis.entity

data class AIConversationInfo(
    val chatSessionId: String?,//会话id
    val sessionName: String?,//会话（第一句）
    val modeType: Int?,//模式类型 1-填词成歌 2-灵感写歌 3-歌曲爆改
    val createTime: Long?, // 时间戳
    var groupType: String? = null//自己加的分组类型，用于ui显示（今天，近7天，近30天）
)
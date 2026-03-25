package com.kuaigeglobal.epnova.core.network.apis.request

data class PowerCalculateBody(
    val strategyType: Int? = null,//策略类型（1-资产图、2-分镜图、3-分镜视频、4-小说文本解析）
    val count: Int? = null,//字符数量
)

data class PowerCalculateData(
    val strategyType: Int? = null,//策略类型（1-资产图、2-分镜图、3-分镜视频、4-小说文本解析）
    val pointCost: Long? = null,//预估算力消耗
)
package com.kuaigeglobal.epnova.core.network.apis.entity

data class InspirationInfo(
    /**
     * 灵感标题，旧版兼容字段。20260710弃用
     */
    val configCode: String? = null,
    /**
     * 灵感内容，旧版兼容字段。20260710弃用
     */
    val configValue: String? = null,
    /**
     * 灵感案例使用次数上报 ID
     */
    val inspirationConfigId: String? = null,
    /**
     * 灵感标题，后续替代 configCode。
     */
    val inspirationTitle: String? = null,
    /**
     * 灵感内容，后续替代 configValue。
     */
    val inspirationContent: String? = null,
)

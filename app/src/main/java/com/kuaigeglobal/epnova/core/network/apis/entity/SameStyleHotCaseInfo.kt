package com.kuaigeglobal.epnova.core.network.apis.entity

/** 创作同款热门案例。 */
data class SameStyleHotCaseInfo(
    val hotCaseId: String? = null,
    val workTitle: String? = null,
    val coverUrl: String? = null,
    val tags: List<String>? = null,
    val recommendText: String? = null
)

/** 根据热门案例获取的同款创作预填数据。 */
data class SameStylePrefillInfo(
    val hotCaseId: String? = null,
    val workTitle: String? = null,
    val formulaText: String? = null,
    val inspirationTitle: String? = null,
    val inspirationContent: String? = null
)

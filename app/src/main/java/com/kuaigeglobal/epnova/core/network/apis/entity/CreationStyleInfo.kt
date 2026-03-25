package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * {
 *             "styleId": "主键id",
 *             "styleName": "风格名称",
 *             "styleCoverUrl": "风格封面",
 *             "promptText": "风格提示语",
 *             "vipStatus": "是否vip使用:0否,1是"
 *         }
 */
data class CreationStyleInfo(
    val promptText: String? = null,
    val styleCoverUrl: String? = null,
    val styleId: String? = null,
    val styleName: String? = null,
    val vipStatus: Int? = null
) {
    val isVip: Boolean get() = vipStatus == 1
}
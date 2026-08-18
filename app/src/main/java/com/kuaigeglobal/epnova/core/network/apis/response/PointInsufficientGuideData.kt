package com.kuaigeglobal.epnova.core.network.apis.response

/**
 * 算力不足时的付费引导信息。
 */
data class PointInsufficientGuideData(
    /**
     * 前端付费引导类型：
     * 0 原卡付费逻辑(原先逻辑)
     * 1 算力首充弹窗
     * 2 会员购买页
     * 3 算力购买半屏弹窗
     */
    val guideType: Int? = null,
) {
    companion object {
        /** 原卡付费逻辑 */
        const val GUIDE_TYPE_LEGACY = 0
        /** 算力首充弹窗 */
        const val GUIDE_TYPE_FIRST_RECHARGE = 1
        /** 会员购买页 */
        const val GUIDE_TYPE_MEMBER = 2
        /** 算力购买半屏弹窗 */
        const val GUIDE_TYPE_RECHARGE_DIALOG = 3
    }
}

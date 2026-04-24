package com.kuaigeglobal.epnova.core.network.apis.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 *  {
 *         "needShow": "是否需要展示弹窗（0-不需要展示，1-需要展示）",
 *         "giftName": "礼包名称 （示例：One -Time Only）",
 *         "giftUrl": "弹窗礼包图片URL",
 *         "coionsMarketingTime": "倒计时剩余毫秒：expire_time - 当前时间（null表示不需要受限制）",
 *         "coionsNum": "基础奖励(500)",
 *         "giveNum": "额外奖励(250)",
 *         "coionsMarketingTag": "额外赠送百分比（示例：+50%）",
 *         "coionsPrice": "当前价格（单位：元，示例：4.99）",
 *         "coionsConfigCode": "金币商品信息配置编码下单时要提交的商品编号",
 *         "appleProductId": "Apple商品ID",
 *         "googleProductId": "Google商品ID"
 *     }
 */
@Parcelize
data class FirstRechargeDialogData(
    val appleProductId: String? = null,
    val coionsConfigCode: String? = null,
    val coionsMarketingTag: String? = null,
    val coionsMarketingTime: Long? = null,
    val coionsNum: Int? = null,
    val coionsPrice: String? = null,
    val giftName: String? = null,
    val giftUrl: String? = null,
    val giveNum: Int? = null,
    val googleProductId: String? = null,
    val needShow: Int? = null
) : Parcelable {
    fun isNeedShow(): Boolean {
        return needShow == 1
    }
}
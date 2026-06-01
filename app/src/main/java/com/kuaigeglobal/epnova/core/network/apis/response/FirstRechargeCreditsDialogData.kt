package com.kuaigeglobal.epnova.core.network.apis.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Introduce: 算力首充礼包弹窗数据
 * {
 *   "code": 200,
 *   "msg": "SUCCESS",
 *   "data": {
 *     "pricePointText": null,
 *     "extraGiftText": null,
 *     "giftTag": null,
 *     "popupInvalidTime": 1780318410473,
 *     "needShow": true,
 *     "imageUrl": "https://kuaichupian.oss-cn-shenzhen.aliyuncs.com/pic/kcp/appPopup/android.png",
 *     "pointConfigVO": {
 *       "pointConfigId": 44,
 *       "pointConfigCode": 20605130000001,
 *       "appleProductId": null,
 *       "pointName": "2600",
 *       "pointPrice": "19.90",
 *       "pointNum": 2600,
 *       "pointMarketingTime": 1780318410473,
 *       "pointMarketingText": "+600 credits",
 *       "pointMarketingTag": "Bonus 30%"
 *     },
 *     "payWayConfig": {
 *       "wayConfigCode": 27513170808832,
 *       "payWay": 5,
 *       "waySortNo": 1,
 *       "payMerchantConfigId": 11440747323392,
 *       "isDefault": 0
 *     }
 *   }
 * }
 *
 */
@Parcelize
data class FirstRechargeCreditsDialogData(
    val pricePointText: String? = null,
    val extraGiftText: String? = null,
    val giftTag: String? = null,
    val popupInvalidTime: Long? = null,
    val needShow: Boolean? = null,
    val imageUrl: String? = null,
    val pointConfigVO: FirstRechargeCreditsPointConfig? = null,
    val payWayConfig: FirstRechargeCreditsPayWayConfig? = null,
) : Parcelable {
    fun isNeedShow(): Boolean {
        return needShow == true
    }
}

@Parcelize
data class FirstRechargeCreditsPointConfig(
    val pointConfigId: String? = null,
    val pointConfigCode: String? = null,
    val appleProductId: String? = null,
    val pointName: String? = null,
    val pointPrice: String? = null,
    val pointNum: Long? = null,
    val pointMarketingTime: Long? = null,
    val pointMarketingText: String? = null,
    val pointMarketingTag: String? = null,
) : Parcelable

@Parcelize
data class FirstRechargeCreditsPayWayConfig(
    val wayConfigCode: String? = null,
    val payWay: Int? = null,
    val waySortNo: Int? = null,
    val payMerchantConfigId: String? = null,
    val isDefault: Int? = null,
) : Parcelable

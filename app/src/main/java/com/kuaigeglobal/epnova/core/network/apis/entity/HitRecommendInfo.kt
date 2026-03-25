package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import com.kuaigeglobal.epnova.common.ext.formatPlays
import com.kuaigeglobal.epnova.common.utils.Utils
import kotlinx.parcelize.Parcelize

/**
 * {
 *                 "popularRecommendDramasId": "爆款推荐ID",
 *                 "dramasCover": "封面图片URL",
 *                 "dramasName": "剧本名称",
 *                 "tags": [
 *                     "泛型String"
 *                 ],
 *                 "introduction": "简介",
 *                 "vipStatus": "是否会员可用：0-否，1-是"
 *             }
 */
data class HitRecommendInfo(
    val dramasCover: String? = null,
    val dramasName: String? = null,
    val introduction: String? = null,
    val popularRecommendDramasId: String? = null,
    val tags: List<String>? = null,
    val styleId: String? = null,//短剧风格id（直接传，是对应的）
    val videoRatio: Int? = null,//视频比例（直接传，是对应的）
    val vipStatus: Int? = null
) {
    fun isVipValid(): Boolean {
        return vipStatus == 1
    }
}

@Parcelize
data class HitTabInfo(
    val tabId: String? = null,
    val tabName: String? = null,
    val tabType: Int? = null// "Tab类型：1-推荐Tab，2-分类Tab"
) : Parcelable

/**
 *  {
 *                 "strategyInfoId": "攻略资讯ID",
 *                 "strategyTitle": "标题",
 *                 "strategyCover": "封面图片URL",
 *                 "viewCount": "观看人数",
 *                 "h5Url": "H5页面地址",
 *                 "createTime": "记录创建时间"
 *             }
 */
data class StrategyInfo(
    val createTime: Long? = null,
    val h5Url: String? = null,
    val strategyCover: String? = null,
    val strategyInfoId: String? = null,
    val strategyTitle: String? = null,
    val viewCount: Int? = null
) {
    fun getShowViewCount(): String {
        return viewCount?.formatPlays() ?: "0"
    }
}

/**
 *         "totalEnroll": "累计入驻",
 *         "totalCreation": "累计创作",
 *         "shortDramasMarketValue": "短剧市场规模"
 *     }
 */
data class ShortDramasStatisticsData(
    val shortDramasMarketValue: Int? = null,
    val totalCreation: Int? = null,
    val totalEnroll: Int? = null
)
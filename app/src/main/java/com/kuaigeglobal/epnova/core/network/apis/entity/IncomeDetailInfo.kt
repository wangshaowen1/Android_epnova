package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * {
 *     "incomeDetailId": "收益明细业务 ID。",
 *     "workId": "作品 ID。",
 *     "storyId": "剧集 ID。",
 *     "workTitle": "作品标题",
 *     "workCoverUrl": "作品封面 URL",
 *     "playCount": "播放量，取作品表真实播放量 play_count_true。",
 *     "incomeSourceType": "收益来源类型：1-付费解锁，2-广告解锁。",
 *     "incomeTime": "收益产生时间，毫秒时间戳。",
 *     "incomeAmountCent": "创作者实际收益金额，单位：分。",
 *     "incomeAmountText": "创作者实际收益展示文案，示例：+$100.23。"
 * }
 */
@Parcelize
data class IncomeDetailInfo(
    val incomeDetailId: Long? = null,
    val workId: Long? = null,
    val storyId: Long? = null,
    val workTitle: String? = null,
    val workCoverUrl: String? = null,
    val playCount: Int? = null,
    val incomeSourceType: Int? = null,
    val incomeTime: Long? = null,
    val incomeAmountCent: Long? = null,
    val incomeAmountText: String? = null
) : Parcelable

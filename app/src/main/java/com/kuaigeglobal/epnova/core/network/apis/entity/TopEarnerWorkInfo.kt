package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import com.kuaigeglobal.epnova.common.ext.toUnitText
import kotlinx.parcelize.Parcelize

/**
 * {
 *     "workId": "作品ID",
 *     "userId": "作者用户ID",
 *     "workTitle": "作品名称",
 *     "workCoverUrl": "封面图",
 *     "workDesc": "作品简介",
 *     "workStyleCode": "作品风格code",
 *     "workRatio": "视频比例：16:9、4:3、1:1、3:4、9:16、21:9、adaptive(自动)",
 *     "totalEpisodeCnt": "集数",
 *     "playCount": "播放量(初始化1000-10000)",
 *     "totalEarnings": "后端转成字符串，作品累计收益（单位：元）字符串，四舍五入保留整数",
 *     "workStoryItemList": "剧集列表",
 *     "platformLinks": "发布平台列表，为空表示未配置任何平台"
 * }
 */
@Parcelize
data class TopEarnerWorkInfo(
    val workId: String? = null,
    val userId: String? = null,
    val workTitle: String? = null,
    val workCoverUrl: String? = null,
    val workDesc: String? = null,
    val workStyleCode: String? = null,
    val workRatio: String? = null,
    val totalEpisodeCnt: Int? = null,
    val playCount: Int? = null,
    val totalEarnings: String? = null,
    val workStoryItemList: List<TopEarnerWorkStoryItem>? = null,
    val platformLinks: List<TopEarnerPlatformLinkItem>? = null
) : Parcelable {

    fun getFormatPlaycount(): String? {
        return playCount?.toUnitText()
    }

    fun getTotalEarningsText(): String {
        return totalEarnings.orEmpty()
    }
}

/**
 * {
 *     "storyId": "所属剧集ID",
 *     "playCount": "播放量(初始化1000-10000)",
 *     "videoUrl": "当前视频url",
 *     "duration": "视频时长(秒)",
 *     "workRatio": "视频比例：16:9、4:3、1:1、3:4、9:16、21:9、adaptive(自动)"
 * }
 */
@Parcelize
data class TopEarnerWorkStoryItem(
    val storyId: String? = null,
    val playCount: Int? = null,
    val videoUrl: String? = null,
    val duration: Int? = null,
    val workRatio: String? = null
) : Parcelable

/**
 * {
 *     "platformCode": "平台code",
 *     "platformName": "平台名称",
 *     "platformLogoUrl": "平台logo URL",
 *     "h5Url": "作品在该平台的H5跳转链接，为空则不可跳转"
 * }
 */
@Parcelize
data class TopEarnerPlatformLinkItem(
    val platformCode: String? = null,
    val platformName: String? = null,
    val platformLogoUrl: String? = null,
    val h5Url: String? = null
) : Parcelable

package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import com.kuaigeglobal.epnova.core.User
import kotlinx.parcelize.Parcelize

/**
 * 短剧系列详情信息
 */
@Parcelize
data class DramasSeriesInfo(
    /**
     * 短剧剧集表id
     */
    val dramasSeriesId: String?,

    /**
     * 剧集作者用户ID
     */
    val authorUserId: String?,

    /**
     * 短剧系列名称
     */
    val seriesName: String?,

    /**
     * 短剧系列封面URL
     */
    val seriesCoverUrl: String?,

    /**
     * 短剧系列简介
     */
    val seriesIntro: String?,

    /**
     * 剧集当前总集数/总作品数
     */
    var totalWorkNum: Int?,

    /**
     * 追剧总数/观看数
     */
    val watchingNum: Int?,

    /**
     * 追剧总数字符串表示
     */
    val watchingNumStr: String?,

    /**
     * 播放次数
     */
    val playNum: Int?,

    /**
     * 播放次数字符串表示
     */
    val playNumStr: String?,

    /**
     * 追剧状态 0-未追剧 1-已追剧
     */
    val followStatus: Int?,

    /**
     * 每集解锁价格,单位：里
     */
    val seriesPrice: Long?,

    /**
     * 上架状态 [0=未上架 1=已上架]
     */
    val launchStatus: Int?,

    /**
     * 自动解锁状态 0 = 非自动解锁 1 = 自动解锁
     */
    var autoUnlockStatus: Int?
) : Parcelable {

    /**
     * 判断当前系列是否为当前用户所有
     */
    fun isMine(): Boolean {
        return User.get().isSameId(authorUserId)
    }

    /**
     * 判断是否已上架
     */
    fun isLaunched(): Boolean {
        return launchStatus == 1
    }

    /**
     * 判断是否已追剧
     */
    fun isFollowing(): Boolean {
        return followStatus == 1
    }

    /**
     * 判断是否支持自动解锁
     */
    fun isAutoUnlockEnabled(): Boolean {
        return autoUnlockStatus == 1
    }
}
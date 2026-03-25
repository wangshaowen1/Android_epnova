package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 *  {
 *             "likeNum": "点赞次数",
 *             "likeNumStr": "点赞次数字符串",
 *             "playNum": "播放次数",
 *             "playNumStr": "播放次数str",
 *             "likeStatus": "点赞状态，0未点赞，1已经点赞",
 *             "watchingStatus": "追剧状态 0-未追剧 1-已经追剧",
 *             "watchingNum": "追剧数量",
 *             "watchingNumStr": "追剧数量str",
 *             "profitPointNum": "收益积分数量"
 *         }
 */
@Parcelize
data class Interaction(
    var likeNum: Int? = null,
    var likeNumStr: String? = null,
    var playNum: Int? = null,
    var playNumStr: String? = null,
    var likeStatus: Int? = null,
    var watchingStatus: Int? = null,
    var watchingNum: Int? = null,
    var watchingNumStr: String? = null,
    var profitPointNum: Int? = null,

    var commentsNum: Int? = null,
    var commentsNumStr: String? = null,

) : Parcelable{
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Interaction

        if (likeNum != other.likeNum) return false
        if (playNum != other.playNum) return false
        if (likeStatus != other.likeStatus) return false
        if (watchingStatus != other.watchingStatus) return false
        if (watchingNum != other.watchingNum) return false
        if (profitPointNum != other.profitPointNum) return false
        if (commentsNum != other.commentsNum) return false
        if (likeNumStr != other.likeNumStr) return false
        if (playNumStr != other.playNumStr) return false
        if (watchingNumStr != other.watchingNumStr) return false
        if (commentsNumStr != other.commentsNumStr) return false

        return true
    }

    override fun hashCode(): Int {
        var result = likeNum ?: 0
        result = 31 * result + (playNum ?: 0)
        result = 31 * result + (likeStatus ?: 0)
        result = 31 * result + (watchingStatus ?: 0)
        result = 31 * result + (watchingNum ?: 0)
        result = 31 * result + (profitPointNum ?: 0)
        result = 31 * result + (commentsNum ?: 0)
        result = 31 * result + (likeNumStr?.hashCode() ?: 0)
        result = 31 * result + (playNumStr?.hashCode() ?: 0)
        result = 31 * result + (watchingNumStr?.hashCode() ?: 0)
        result = 31 * result + (commentsNumStr?.hashCode() ?: 0)
        return result
    }
}
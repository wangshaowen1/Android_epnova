package com.kuaigeglobal.epnova.core.network.apis.entity

import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.toUnitText
import com.kuaigeglobal.epnova.core.User
import com.nhsd.basic.ext.getResString

/**
 * {
 *                 "workId": "作品 ID",
 *                 "workTitle": "作品标题",
 *                 "workCoverUrl": "作品封面图 URL",
 *                 "totalEpisodeCnt": "作品总集数，用于展示「EP.x / EP.y」中的 y",
 *                 "lastSortNo": "用户最近观看到的集序号（sort_no），用于展示「EP.x / EP.y」中的 x",
 *                 "playProgress": "该最近观看集内的播放进度百分比，0～100，用于缩略图底部进度条",
 *                 "lastPlayTime": "最近一次有效播放/浏览时间，列表排序依据"
 *             }
 */
data class WorkVOInfo(
    val workId: String? = null,
    val userId: String? = null,//作者userId
    val workTitle: String? = null,
    val nickName: String? = null,
    val workDesc: String? = null,
    val workCoverUrl: String? = null,
    val lastSortNo: Int? = null,
    val workRatio: String? = null,
    val playProgress: Int? = null,
    val playCount: Int? = null,
    var collected: Boolean? = null,
    val collectCount: Int? = null,
    val totalEpisodeCnt: Int? = null,
    val lastPlayTime: Long? = null,
    val collectionTime: Long? = null,
    /** 作品来源：1-公司创作，2-用户创作' */
    val workSource: Int? = null,
) {
    fun isMine(): Boolean {
        return User.get().isSameId(userId)
    }

    fun isUserCreated(): Boolean {
        return (workSource ?: 0) == 2
    }

    fun getFormatPlaycount(): String? {
        return playCount?.toUnitText()
    }

    fun getFormatEp(): String {
        return if (lastSortNo != null && totalEpisodeCnt != null) {
            "${R.string.vol_number.getResString()}${lastSortNo} / ${R.string.vol_number.getResString()}${totalEpisodeCnt}"
        } else {
            ""
        }
    }

    /**
     * 判断最近是否有播放进度
     */
    fun isPlayProgress(): Boolean {
        return playProgress != null && playProgress > 0
    }
}


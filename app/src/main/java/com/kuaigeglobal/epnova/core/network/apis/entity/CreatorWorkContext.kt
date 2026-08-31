package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import androidx.room.Ignore
import com.kuaigeglobal.epnova.core.User
import kotlinx.parcelize.Parcelize

/**
 * 播放器内需要的作品上下文。
 *
 * 播放核心仍是 CreatorEpisodeInfo；这个类只补充标题、封面、平台等作品级展示信息。
 */
@Parcelize
data class CreatorWorkContext(
    /**作品 ID。*/
    val workId: String? = null,

    /**作者用户 ID。*/
    val userId: String? = null,

    /**作品标题，用于播放器标题和列表展示。*/
    val workTitle: String? = null,

    /**作品简介，用于播放器说明区。*/
    val workDesc: String? = null,

    /**作品封面，视频不可用或加载前可作为封面。*/
    val workCoverUrl: String? = null,

    /**作品默认视频比例。剧集没有比例时使用该值兜底。*/
    val workRatio: String? = null,

    /**作品规划或总集数。*/
    val totalEpisodeCnt: Int? = null,

    /**作品已完成集数，用于判断整部剧是否完结。*/
    val finishedEpisodeCnt: Int? = null,

    /**作品播放量。*/
    val playCount: Int? = null,

    /**TopEarners 收益展示字段。*/
    val totalEarnings: String? = null,

    /**上架时间，预留给 FastFilm 风格数据。*/
    val shelfTime: Long? = null,

    /**发布平台列表。*/
    val platformLinks: List<CreatorPlatformInfo>? = null
) : Parcelable {

    /**总集数有效且全部创作完成时，整部剧才算完结。*/
    fun isAllEpisodesCompleted(): Boolean {
        val total = totalEpisodeCnt ?: return false
        return total > 0 && (finishedEpisodeCnt ?: 0) >= total
    }

    /**是否当前登录用户自己的作品；新播放器据此切换举报菜单和创作者编辑/下载操作。*/
    fun isMine(): Boolean {
        return User.get().isSameId(userId)
    }
}

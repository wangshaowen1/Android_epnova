package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 创作播放器最终消费的播放单元。
 *
 * 不建议在分页源阶段过早生成该模型；列表 source 应优先保持作品或剧集维度，
 * 进入播放器时再映射为这个播放单元。
 */
@Parcelize
data class CreatorPlayableEpisode(
    /**剧集播放信息，播放器只从这里取 videoUrl、duration、storyId 等播放字段。*/
    val episode: CreatorEpisodeInfo = CreatorEpisodeInfo(),

    /**作品级展示上下文。*/
    val workContext: CreatorWorkContext = CreatorWorkContext(),

    /**播放来源，用于区分是否允许跨作品切换。*/
    val source: CreatorPlaySource = CreatorPlaySource.TOP_EARNERS,

    /**当前 feed 的稳定 key，通常是 workId 或 workId + storyId。*/
    val feedKey: String? = null,

    /**当前 feed 内的顺序，只表达 UI/播放位置，不作为业务 ID。*/
    val feedIndex: Long = 0L
) : Parcelable

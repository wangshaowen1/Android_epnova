package com.kuaigeglobal.epnova.feature.creatorplay.mapper

import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorEpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlaySource
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlayableEpisode
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkContext
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo

/**
 * Creator work/episode 到播放器播放单元的转换器。
 *
 * MyCreations 和作品详情都不跨作品切换，因此这里要求调用方显式传入 source。
 */
object CreatorWorkSeriesMapper {

    /**将当前作品内的一个 episode 包装成播放器可消费的播放单元。*/
    fun toPlayableEpisode(
        work: CreatorWorkInfo?,
        episode: CreatorEpisodeInfo?,
        source: CreatorPlaySource,
        feedIndex: Long,
        feedKey: String? = null
    ): CreatorPlayableEpisode? {
        work ?: return null
        episode ?: return null
        val normalizedEpisode = episode.withWorkFallback(work)
        return CreatorPlayableEpisode(
            episode = normalizedEpisode,
            workContext = work.toCreatorWorkContext(),
            source = source,
            feedKey = feedKey ?: buildEpisodeFeedKey(work, normalizedEpisode, feedIndex),
            feedIndex = feedIndex
        )
    }

    /**作品详情剧集列表 source，只在当前作品的剧集内上下切换。*/
    fun toWorkSeriesEpisodes(
        work: CreatorWorkInfo?,
        episodes: List<CreatorEpisodeInfo>? = work?.workStoryItemList
    ): List<CreatorPlayableEpisode> {
        return episodes.orEmpty().mapIndexedNotNull { index, episode ->
            toPlayableEpisode(
                work = work,
                episode = episode,
                source = CreatorPlaySource.WORK_SERIES,
                feedIndex = index.toLong()
            )
        }
    }

    /**MyCreations source，只播放当前作品自己的剧集，不跨到其它作品。*/
    fun toMyCreationsEpisodes(
        work: CreatorWorkInfo?,
        episodes: List<CreatorEpisodeInfo>? = work?.workStoryItemList
    ): List<CreatorPlayableEpisode> {
        return episodes.orEmpty().mapIndexedNotNull { index, episode ->
            toPlayableEpisode(
                work = work,
                episode = episode,
                source = CreatorPlaySource.MY_CREATIONS,
                feedIndex = index.toLong()
            )
        }
    }

    /**抽取播放器需要的作品上下文，避免播放器直接依赖完整 CreatorWorkInfo。*/
    fun CreatorWorkInfo.toCreatorWorkContext(): CreatorWorkContext {
        return CreatorWorkContext(
            workId = workId,
            userId = userId,
            workTitle = workTitle,
            workDesc = workDesc,
            workCoverUrl = workCoverUrl,
            workRatio = workRatio,
            totalEpisodeCnt = totalEpisodeCnt,
            finishedEpisodeCnt = finishedEpisodeCnt,
            playCount = playCount,
            totalEarnings = totalEarnings,
            shelfTime = shelfTime,
            platformLinks = platformLinks
        )
    }

    /**剧集缺少标题、简介、比例或封面时，用作品字段兜底，避免播放器 UI 空白。*/
    private fun CreatorEpisodeInfo.withWorkFallback(work: CreatorWorkInfo): CreatorEpisodeInfo {
        return copy(
            workId = workId ?: work.workId,
            storyTitle = storyTitle ?: work.workTitle,
            storySummary = storySummary ?: work.workDesc,
            workRatio = workRatio ?: work.workRatio,
            workCoverUrl = workCoverUrl ?: work.workCoverUrl
        )
    }

    /**同一作品下可能有多集，feedKey 必须精确到 episode，不能只用 workId。*/
    private fun buildEpisodeFeedKey(
        work: CreatorWorkInfo,
        episode: CreatorEpisodeInfo,
        feedIndex: Long
    ): String {
        val workId = work.workId ?: episode.workId
        val storyId = episode.storyId
        return when {
            !workId.isNullOrEmpty() && !storyId.isNullOrEmpty() -> "$workId-$storyId"
            !storyId.isNullOrEmpty() -> storyId
            !workId.isNullOrEmpty() -> "$workId-$feedIndex"
            else -> feedIndex.toString()
        }
    }
}

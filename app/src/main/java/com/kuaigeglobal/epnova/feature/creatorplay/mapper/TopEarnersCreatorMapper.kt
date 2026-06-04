package com.kuaigeglobal.epnova.feature.creatorplay.mapper

import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorEpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlaySource
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlayableEpisode
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkContext
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo

/**
 * TopEarners 推荐作品在 Creator 体系内的归一化边界。
 *
 * recommendWorkList 已直接反序列化为 CreatorWorkInfo，这里只处理 TopEarners/FastFilm
 * 单集作品的兜底字段，不再做旧 DTO 到 CreatorWorkInfo 的逐字段搬运。
 */
object TopEarnersCreatorMapper {

    /**归一化 TopEarners work，补齐单集作品里 episode 缺失的作品级展示字段。*/
    fun normalizeWork(work: CreatorWorkInfo?): CreatorWorkInfo? {
        work ?: return null
        return work.copy(
            workStoryItemList = work.workStoryItemList.orEmpty().map { episode ->
                episode.withTopEarnersFallback(work)
            }
        )
    }

    /**从共享 CreatorWorkInfo feed 派生播放单元；TopEarners 是唯一允许跨作品切换的 source。*/
    fun toPlayableEpisodeOrNull(
        work: CreatorWorkInfo?,
        feedIndex: Long,
        feedKey: String? = null
    ): CreatorPlayableEpisode? {
        val normalizedWork = normalizeWork(work) ?: return null
        val episode = normalizedWork.workStoryItemList.orEmpty()
            .firstOrNull()
            ?: return null
        return CreatorPlayableEpisode(
            episode = episode,
            workContext = normalizedWork.toCreatorWorkContext(),
            source = CreatorPlaySource.TOP_EARNERS,
            feedKey = feedKey ?: buildTopEarnersFeedKey(normalizedWork, episode, feedIndex),
            feedIndex = feedIndex
        )
    }

    /**批量转换播放单元；后续播放器从同一个 work feed 派生播放 feed 时使用。*/
    fun toPlayableEpisodes(works: List<CreatorWorkInfo>?): List<CreatorPlayableEpisode> {
        return works.orEmpty().mapIndexedNotNull { index, work ->
            toPlayableEpisodeOrNull(work, feedIndex = index.toLong())
        }
    }

    /**播放器标题、封面、平台等作品级展示信息从 work 上下文取。*/
    private fun CreatorWorkInfo.toCreatorWorkContext(): CreatorWorkContext {
        return CreatorWorkContext(
            workId = workId,
            userId = userId,
            workTitle = workTitle,
            workDesc = workDesc,
            workCoverUrl = workCoverUrl,
            workRatio = workRatio,
            totalEpisodeCnt = totalEpisodeCnt,
            playCount = playCount,
            totalEarnings = totalEarnings,
            platformLinks = platformLinks.orEmpty()
        )
    }

    /**TopEarners 单集接口可能省略标题、简介、封面和比例，播放器展示时用作品字段兜底。*/
    private fun CreatorEpisodeInfo.withTopEarnersFallback(work: CreatorWorkInfo): CreatorEpisodeInfo {
        return copy(
            workId = workId ?: work.workId,
            storyTitle = storyTitle ?: work.workTitle,
            storySummary = storySummary ?: work.workDesc,
            workRatio = workRatio ?: work.workRatio,
            playCount = playCount ?: work.playCount,
            workCoverUrl = workCoverUrl ?: work.workCoverUrl
        )
    }

    /**TopEarners 当前一作品一集；这里仍保留 storyId 兜底，避免未来多集时 key 冲突。*/
    private fun buildTopEarnersFeedKey(
        work: CreatorWorkInfo,
        episode: CreatorEpisodeInfo,
        feedIndex: Long
    ): String {
        val workId = work.workId ?: episode.workId
        val storyId = episode.storyId
        return when {
            !workId.isNullOrEmpty() && !storyId.isNullOrEmpty() -> "$workId-$storyId"
            !workId.isNullOrEmpty() -> workId
            !storyId.isNullOrEmpty() -> storyId
            else -> feedIndex.toString()
        }
    }
}

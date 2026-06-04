package com.kuaigeglobal.epnova.feature.creatorplay.data

import androidx.paging.PagingData
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorEpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlaySource
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlayableEpisode
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import com.kuaigeglobal.epnova.feature.creatorplay.mapper.CreatorWorkSeriesMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 当前作品内的 Creator 播放 feed 仓库。
 *
 * MyCreations 和作品详情都只允许在当前作品内上下切换，因此这里按 source + workId
 * 缓存当前入口提交的剧集列表，不复用旧内容播放器 FeedRepository。
 */
object WorkSeriesCreatorFeedRepository {

    private val playableFeeds = mutableMapOf<String, List<CreatorPlayableEpisode>>()

    /**
     * 提交当前作品内的可播放剧集，并返回播放器启动时要携带的 sourceKey。
     *
     * 调用方已经完成点击位置修正；这里再次过滤空视频，避免播放器拿到不可播 item。
     */
    fun setPlayableFeed(
        source: CreatorPlaySource,
        work: CreatorWorkInfo?,
        episodes: List<CreatorEpisodeInfo>
    ): String? {
        if (source == CreatorPlaySource.TOP_EARNERS) return null
        work ?: return null
        val sourceKey = work?.workId ?: episodes.firstOrNull()?.workId ?: return null
        val playableEpisodes = episodes.filter { !it.videoUrl.isNullOrEmpty() }
        if (playableEpisodes.isEmpty()) return null
        val playableFeed = when (source) {
            CreatorPlaySource.MY_CREATIONS -> CreatorWorkSeriesMapper.toMyCreationsEpisodes(work, playableEpisodes)
            CreatorPlaySource.WORK_SERIES -> CreatorWorkSeriesMapper.toWorkSeriesEpisodes(work, playableEpisodes)
            CreatorPlaySource.TOP_EARNERS -> emptyList()
        }
        playableFeeds[cacheKey(source, sourceKey)] = playableFeed
        return sourceKey
    }

    fun getPlayableFeed(
        source: CreatorPlaySource,
        sourceKey: String?
    ): Flow<PagingData<CreatorPlayableEpisode>> {
        val key = sourceKey?.takeIf { it.isNotEmpty() } ?: return flowOf(PagingData.empty())
        val playableFeed = playableFeeds[cacheKey(source, key)] ?: return flowOf(PagingData.empty())
        return flowOf(PagingData.from(playableFeed))
    }

    private fun cacheKey(source: CreatorPlaySource, sourceKey: String): String {
        return "${source.name}:$sourceKey"
    }
}

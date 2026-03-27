package com.kuaigeglobal.epnova.feature.play.data

import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo

/**
 * 全局单例仓库：负责数据分发、缓存管理和预加载桥接
 */
object FeedRepository {
    private var preloadedSeriesData: List<EpisodeInfo>? = null
    private var preloadedWorkId: String? = null

    // 在作品详情页点击播放时调用
    fun setPreloadedSeries(workId: String?, list: List<EpisodeInfo>?) {
        preloadedWorkId = workId
        preloadedSeriesData = list
    }

    // 在 DramasPlayPagerViewModel 中调用，取完即焚，防止内存泄漏
    fun consumePreloadedSeries(workId: String): List<EpisodeInfo>? {
        if (preloadedWorkId == workId && preloadedSeriesData != null) {
            val data = preloadedSeriesData
            preloadedSeriesData = null // 用完清空
            preloadedWorkId = null
            return data
        }
        return null
    }
}
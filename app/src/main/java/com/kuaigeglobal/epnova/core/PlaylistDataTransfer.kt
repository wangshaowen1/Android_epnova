package com.kuaigeglobal.epnova.core

import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo

/**
 * 专门用于在页面间传递播放列表数据，避免 Intent TransactionTooLargeException
 */
object PlaylistDataTransfer {
    // 暂存数据
    private var data: List<EpisodeInfo>? = null

    /**
     * 存入数据
     */
    fun put(list: List<EpisodeInfo>?) {
        data = list
    }

    /**
     * 取出数据并立即清空（阅后即焚，防止内存泄漏）
     */
    fun getAndClear(): List<EpisodeInfo>? {
        val result = data
        data = null //清空引用
        return result
    }
}
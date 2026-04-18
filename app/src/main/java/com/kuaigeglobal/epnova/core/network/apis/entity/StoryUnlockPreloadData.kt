package com.kuaigeglobal.epnova.core.network.apis.entity

import com.kuaigeglobal.epnova.core.network.apis.response.EpisodePlayData

/**
 * 播放器-剧集自动解锁响应
 */
data class StoryUnlockPreloadData(
    /** 当前集信息 */
    val currentStory: EpisodePlayData?,
    /** 本次是否触发了自动解锁（余额不足或无需解锁时为 false） */
    val autoUnlocked: Boolean?,
    /** 自动解锁后的剩余金币余额，autoUnlocked=false 时为 null */
    val remainCoinsBalance: Long?
)

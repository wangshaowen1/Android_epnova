package com.kuaigeglobal.epnova.feature.play

import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo

/**
 * 当前剧集处于锁定状态，需要介入解锁流程。
 *
 * @param isAutoUnlockFailed true  = 自动解锁已开启但余额不足，流程：充值 → 广告 → 手动解锁
 *                           false = 自动解锁未开启，直接展示手动解锁弹窗
 */
class EpisodeLockedEx(
    val episode: EpisodeInfo,
    val isAutoUnlockFailed: Boolean = false
) : Exception("Episode locked: ${episode.storyId}")


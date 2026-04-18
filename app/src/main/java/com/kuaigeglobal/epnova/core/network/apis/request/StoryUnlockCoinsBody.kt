package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 解锁弹窗-金币解锁请求体
 * @param workId 作品 ID
 * @param storyIds 待解锁剧集 ID 列表（通常传单集）
 * @param unlockType 解锁类型：1=自动解锁，2=手动解锁
 */
data class StoryUnlockCoinsBody(
    val workId: Long,
    val storyIds: List<Long>,
    val unlockType: Int
) : UserTokenBody()

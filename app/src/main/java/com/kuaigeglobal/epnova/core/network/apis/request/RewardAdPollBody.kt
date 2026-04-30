package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 解锁弹窗-广告解锁结果轮询请求体
 * @param workId 作品 ID
 * @param storyId 剧集 ID
 * @param nonce 本次广告唯一标识，由 /ad/prepare 接口返回
 */
data class StoryUnlockAdResultBody(
    val workId: String?,
    val storyId: String?,
    val nonce: String?
) : UserTokenBody()


data class RewardAdPollBody(
    val adFlowId: String?//广告流水ID（由 /reward/task/ad/preReport 返回的 nonce）
)

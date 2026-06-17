package com.kuaigeglobal.epnova.core.network.apis.response

import com.kuaigeglobal.epnova.core.network.apis.entity.AssetInfo

/**
 * 当前集资产列表
 */
data class AssetStoryListInfo(
    // 作品 ID
    val workId: String? = null,
    // 剧集 ID
    val storyId: String? = null,
    // 当前集角色资产列表
    val roles: List<AssetInfo>? = null,
    // 当前集场景资产列表
    val scenes: List<AssetInfo>? = null,
    // 当前集道具资产列表
    val props: List<AssetInfo>? = null
)

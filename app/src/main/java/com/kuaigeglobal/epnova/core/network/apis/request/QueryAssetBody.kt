package com.kuaigeglobal.epnova.core.network.apis.request

data class QueryAssetBody(
    /**
     * 资产 ID （角色、道具、场景 id）根据业务类型判断，查询单个时入参
     */
    val assetId: String?,
    /**
     * 当前资产形象ID 查询单个时入参
     */
    val appearanceId: String?,
    /**
     * 查询单个资产必须要入参业务类型，不入参默认查全部业务类型枚举 [41=角色图片 51=道具图片 61=场景图片]
     */
    val bizType: Int?,
    /**
     * 剧集 id
     */
    val storyId: String?,
    /**
     * 作品 ID
     */
    val workId: String?,
) {
    companion object {
        const val BIZ_TYPE_ROLE = 41
        const val BIZ_TYPE_PROP = 51
        const val BIZ_TYPE_SCENE = 61
    }
}

class QueryAssetPagedBody(
    /**
     * 资产 ID （角色、道具、场景 id）根据业务类型判断，查询单个时入参
     */
    val assetId: String?,
    /**
     * 当前资产形象ID 查询单个时入参
     */
    val appearanceId: String?,
    /**
     * 查询单个资产必须要入参业务类型，不入参默认查全部业务类型枚举 [41=角色图片 51=道具图片 61=场景图片]
     */
    val bizType: Int?,
    /**
     * 剧集 id
     */
    val storyId: String?,
    /**
     * 作品 ID
     */
    val workId: String?,
    pageNum: Int,
    pageSize: Int,
) : PagedBody(pageNum, pageSize)
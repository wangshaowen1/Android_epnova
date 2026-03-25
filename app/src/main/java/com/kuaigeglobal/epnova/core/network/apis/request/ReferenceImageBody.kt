package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "assetId": "资产 ID （角色、道具、场景 id）根据业务类型判断，查询单个时入参",
 *     "bizType": "查询单个资产必须要入参业务类型，不入参默认查全部业务类型枚举 [41=角色图片 51=道具图片 61=场景图片]",
 *     "workId": "作品 ID",
 *     "storyId": "剧集 id"
 * }
 */
data class ReferenceImageBody(
    //资产 ID （角色roleId、道具propId、场景sceneId）根据业务类型判断，查询单个时入参
    val assetId: String? = null,
    //当前资产形象ID 查询单个时入参
    val appearanceId: String? = null,
    //查询单个资产必须要入参业务类型，不入参默认查全部业务类型枚举 [41=角色图片 51=道具图片 61=场景图片]
    val bizType: Int? = null,
    //剧集 id
    val storyId: String? = null,
    //作品 ID
    val workId: String? = null
) : UserTokenBody() {
    companion object {
        //角色图片
        const val BIZ_TYPE_ROLE_IMAGE = 41

        //道具图片
        const val BIZ_TYPE_PROP_IMAGE = 51

        //场景图片
        const val BIZ_TYPE_SCENE_IMAGE = 61
    }
}
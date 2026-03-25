package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "storyboardId": "分镜id",
 *     "storyBoardDesc": "分镜描述",
 *     "operateType": "操作类型(1=新增 2=合成前的重绘编辑 3=合成后的重绘编辑)20260228新增保存需要传这个判断是保存是在什么操作see com.kuaigeai.common.enums.WorkOperateTypeEnum",
 *     "storyId": "剧集 id",
 *     "workId": "作品 ID"
 * }
 */
data class GenerateStoryboardBody(
    /**
     * 分镜id
     */
    val storyboardId: String? = null,
    /**
     * 分镜描述
     */
    val storyBoardDesc: String? = null,
    /**
     * 操作类型(1=新增 2=合成前的重绘编辑 3=合成后的重绘编辑)20260228新增保存需要传这个判断是保存是在什么操作see com.kuaigeai.common.enums.WorkOperateTypeEnum
     */
    val operateType: Int? = null,
    /**
     * 作品 ID
     */
    val workId: String? = null,
    /**
     * 剧集 id
     */
    val storyId: String? = null
)
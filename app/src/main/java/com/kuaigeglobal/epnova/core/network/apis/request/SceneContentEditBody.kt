package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "dramasId": "剧本id",
 *     "shotSegmentId": "短剧分镜片段id",
 *     "sceneContent": "场景内容",
 *     "userToken": "用户token，接受Long和String类型"
 * }
 */
data class SceneContentEditBody(
    val dramasId: String? = null,
    val shotSegmentId: String? = null,
    val sceneContent: String? = null,
) : UserTokenBody()


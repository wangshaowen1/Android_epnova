package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "userTimbreId": "用户声色ID 当为完善声色时必填/添加音色为空",
 *     "sourceSoundUrl": "用户声音URL",
 *     "userToken": "用户token，接受Long和String类型"
 * }
 */
data class TimbreAddOrOptimizeBody(
    val sourceSoundUrl: String? = null,
    val userTimbreId: String? = null,
) : UserTokenBody()
package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "roleImageDesc": "角色图片描述",
 *     "roleAge": "角色年龄",
 *     "roleGender": "角色性别",
 *     "roleType": "角色类型",
 *     "userToken": "用户token，接受Long和String类型"
 * }
 */
data class OptimizeRoleImageDescBody(
    val dramasId: String? = null,
    val roleImageDesc: String? = null,
    val roleAge: String? = null,
    val roleGender: String? = null,
    val roleType: String? = null,
) : UserTokenBody()
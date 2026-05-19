package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "roleId": "角色id，接受Long和String类型",
 *     "roleImageDesc": "角色图片描述",
 *     "roleTagIdList": ["泛型Long"],
 *     "referenceRoleImageUrl": "参考角色图片",
 *     "userToken": "用户token，接受Long和String类型"
 * }
 */
data class RoleImageRegenerateBody(
    val dramasId: String? = null,
    val roleId: String? = null,
    val roleImageDesc: String? = null,
    val roleTagIdList: List<String?>? = null,
    val referenceRoleImageUrl: String? = null,
    val roleName: String? = null,//角色名称，新增角色用
) : UserTokenBody()

data class RoleIdBody(
    val roleId: String? = null,
) : UserTokenBody()

data class RoleSwitchBody(
    val roleId: String? = null,
    val oldRoleId: String? = null,
) : UserTokenBody()

class RoleIdPagedBody(
    val roleId: String? = null,
    pageNum: Int,
    pageSize: Int,
) : PagedBody(pageNum, pageSize)
package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 完善用户资料接口
 * @param nickName 用户昵称
 * @param avatarUrl  头像
 * @param gender 用户性别 0-表示未知，1-表示男，2-表示女
 * @param birthday 生日
 * @param province 省
 * @param city 市
 * @param signature 个性签名
 * @param userV2EraId 年代ID
 */
data class CompleteUserDataInfoBody(
    val nickName: String? = null,
    val avatarUrl: String? = null,
    val gender: Int? = null,
    val birthday: Long? = null,
    val province: String? = null,
    val city: String? = null,
    val signature: String? = null,
    val userV2EraId: String? = null
) : UserTokenBody()
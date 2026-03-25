package com.kuaigeglobal.epnova.core.network.apis.response

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:邀请用户记录分页
 * @Data: 2025/10/15/周三  14:27
 * @Version: 1.0
 * *****************************************
 */
data class InviteUserPageInfo(
    //头像
    val avatarUrl: String? = null,
    //昵称
    val nickName: String? = null,
    //注册时间
    val registerTime: Long? = null
)
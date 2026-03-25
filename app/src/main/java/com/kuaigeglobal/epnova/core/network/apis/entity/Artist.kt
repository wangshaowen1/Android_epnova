package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce: 获取用户基础信息
 * @Data: 2024/7/8 星期一 14:53
 * @Version: 1.0
 * *****************************************
 */
@Parcelize
data class Artist(
    //用户ID，实际返回String类型
    val userToken: String? = null,
    //用户的APP ID -- 用户APP上面展示
    val userAppId: String? = null,
    //昵称
    val nickName: String? = null,
    //头像
    val avatarUrl: String? = null,
    //性别
    val gender: Int? = null) : Parcelable

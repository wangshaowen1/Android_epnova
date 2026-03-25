package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * {
 *             "roleTagId": "角色标签id",
 *             "roleTagType": "角色标签类型 [1=年龄 2=性别 3=类别]",
 *             "roleTagName": "角色标签名称"
 *         }
 */
@Parcelize
data class RoleTagInfo(
    val roleTagId: String? = null,
    val roleTagName: String? = null,
    val roleTagType: Int? = null
) : Parcelable
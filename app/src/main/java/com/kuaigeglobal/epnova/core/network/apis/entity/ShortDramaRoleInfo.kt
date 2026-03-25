package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShortDramaRoleInfo(
    /**
     * 角色id
     */
    val roleId: String?,
    /**
     * 角色名称
     */
    val roleName: String?,
    /**
     * 角色图片
     */
    val roleImageUrl: String?
) : Parcelable
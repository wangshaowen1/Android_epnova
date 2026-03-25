package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CharacterInfo(
    val genStatus: Int?,//生成状态 [0=待生成 1=生成中 2=生成成功 3=生成失败]
    val roleId: String?,//角色id
    val roleImageDesc: String?,//角色图片描述
    val roleImageUrl: String?,//角色图片
    val roleName: String?,//角色名称
    val newRoleStatus: Int?,//是否为新角色，0-否 1-是
    val roleTagList: List<RoleTagInfo?>?//角色标签集合
) : Parcelable {
    fun tags(): List<String?>? {
        return roleTagList?.map { it?.roleTagName }
    }

    fun isNewRole(): Boolean {
        return newRoleStatus == 1
    }
}
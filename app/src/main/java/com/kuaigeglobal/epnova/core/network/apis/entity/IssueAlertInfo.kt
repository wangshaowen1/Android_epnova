package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IssueAlertInfo(
    val aiLyricRemark:String?,
    val systemRemark:String?,
    val musicId:String?,
    val ratio:Int?,
    var nickName:String?,
    var musicTitle:String?,
    var musicCoverUrl:String?
) : Parcelable
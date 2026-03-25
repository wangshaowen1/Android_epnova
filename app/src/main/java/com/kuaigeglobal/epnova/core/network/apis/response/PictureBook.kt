package com.kuaigeglobal.epnova.core.network.apis.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PictureBook(
    var appearanceSource: String? = null,//绘本形象来源 [1=用户 2=系统]
    var appearanceId: String? = null,//绘本形象id
    var parentPictureBookId: String? = null,//父级绘本id
    var pictureBookContent: String? = null,//绘本内容
    var pictureBookGenStatus: Int? = null,//[枚举]绘本生成(合成)状态 [0=待生成 1=生成中 2=生成成功 3=生成失败]
    var pictureBookId: String? = null,//绘本id
    var pictureBookName: String? = null,//绘本名称
    var pictureBookRole: String? = null,//绘本角色名称
    var pictureBookTimbreId: String? = null,//绘本音色id（绘本用户形象表id/绘本系统形象表id）
    var pictureBookTimbreSource: String? = null,//绘本音色来源 [1=用户 2=系统]
    var saveStatus: Int? = null,//保存状态:(0-未保存(草稿) 1-发布)
    //绘本封面
    var coverUrl: String? = null,
    //创建时间
    val createTime: Long? = null,
    //浏览量带w
    val browseCount: String? = null,
    //年龄段
    val ageRange: String? = null,
    //绘本生成方式 [1=模板生成 2=自定义生成3=灵感生成]
    val genMethod:Int? = null,
    //作者userToken
    val userToken: String? = null,
) : Parcelable {
}
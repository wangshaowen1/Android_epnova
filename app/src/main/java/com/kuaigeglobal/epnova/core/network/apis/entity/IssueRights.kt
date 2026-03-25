package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:发行权益集合（此歌曲购买过发行权益此字段才会返回值）
 * @Data: 2025/5/6  10:27
 * @Version: 1.0
 * *****************************************
 */
@Parcelize
data class IssueRights(
    //权益id
    val rightId: String? = null,
    //权益编码<br/>伴奏和分轨文件:ACCOMPANY_AND_STRIP 音著协版权注册:MUSIC_COPYRIGHT 五大平台发行教程: ISSUE_TUTORIAL
    val rightCode: String? = null,
    val rightStatus:Int? = null,//rightCode 为MUSIC_COPYRIGHT时此字段为2 为音著协注册成功
    val certificateUrl:String?=null,
    val purchaseType:Int? = null//购买方式 0-发行套餐购买 1-单独购买

) : Parcelable
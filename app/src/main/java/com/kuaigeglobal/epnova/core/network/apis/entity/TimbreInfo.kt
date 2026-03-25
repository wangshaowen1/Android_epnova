package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TimbreInfo(
    val timbreStatus: Int? = null,//音色生成状态 [0=待处理 1=待生成 2=生成中 3=生成成功 4=生成失败 5=安全检测中（当生成中处理）]
    val timbreAvatarUrl: String? = null,//音色封面
    val timbreId: String? = null,//音色id
    val timbreModelId: String? = null,//音色模型id
    val timbreTitle: String? = null,//音色名称
    val userId: String? = null,
    val timbreSoundUrl: String? = null,//音色合成声音url
    val topStatus: Int? = null,//是否可重命名 1-不可重命名 0-可重命名
    val timbreType: Int? = null,//音色类型 1-系统 2-用户音色
    val vipStatus: Int? = null//是否是免费声色  0-免费音色
) : Parcelable {

    fun isNameModifyValid(): Boolean {
        return topStatus == 0 || timbreType == 2
    }

    fun available(): Boolean {
        return timbreStatus == 3
    }

    fun isTopTimbreForGenerate(): Boolean {
        return timbreId.isNullOrEmpty() || isForComplete()
    }

    fun isForComplete(): Boolean {
        return topStatus == 1 && timbreStatus == 0
    }

    fun isGenerating(): Boolean {
        return timbreStatus == 2 || timbreStatus == 1 || timbreStatus == 5
    }

    fun isGenerated(): Boolean {
        return timbreStatus == 3 || timbreStatus == 4
    }

    fun isGenerateFail(): Boolean {
        return timbreStatus == 4
    }

    fun getGenerateTitle(): String {
        return when (timbreStatus) {
            0, 3 -> timbreTitle ?: ""
            1, 2, 5 -> "正在克隆音色中..."
            4 -> "克隆失败"
            else -> "自定义音色"
        }
    }

    fun getSimpleGenerateTitle(): String {
        return when (timbreStatus) {
            0, 3 -> timbreTitle ?: ""
            1, 2, 5 -> "克隆中..."
            4 -> "克隆失败"
            else -> "自定义"
        }
    }

    fun getGenerateTitleDesc(): String {
        return when (timbreStatus) {
            1, 2, 5 -> "请稍等1-2分钟..."
            4 -> "可在“我的音色”中删除并重新生成"
            else -> ""
        }
    }
}
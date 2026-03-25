package com.kuaigeglobal.epnova.core.network.apis.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class DramasIdData(
    val dramasId: String?
)

/**
 * {
 *         "dramasId": "短剧id",
 *         "dramasName": "短剧名称",
 *         "scriptContent": "剧本内容",
 *         "roleList": [{
 *             "roleId": "角色id",
 *             "roleName": "角色名称"
 *         }],
 *         "genStatus": "[枚举]生成状态 [0=待生成 1=生成中 2=生成成功 3=生成失败]"
 *     }
 */
data class DramasInfoData(
    val dramasId: String? = null,
    val dramasName: String? = null,
    val genStatus: Int? = null,
    val roleList: List<Role>? = null,
    var scriptContent: String? = null
) {
    fun isGenOver(): Boolean {
        return genStatus == 2 || genStatus == 3
    }

    fun isGenSuccess(): Boolean {
        return genStatus == 2
    }

    fun isGenFailed(): Boolean {
        return genStatus == 3
    }
}

@Parcelize
data class Role(
    val roleId: String? = null,
    val roleName: String? = null
) : Parcelable

/**
 * {
 *         "dramasId": "短剧id",
 *         "shotSegmentList": [{
 *             "shotSegmentId": "短剧分镜片段id",
 *             "sequenceNumber": "序号(从1开始)",
 *             "estimatedDuration": "预估时长(秒)",
 *             "shotSegmentSceneContentList": [{
 *                 "sceneContentType": "[枚举]场景内容类型 [1=画面 2=旁边 3=动作]",
 *                 "sceneContent": "场景内容"
 *             }]
 *         }],
 *         "estimatedDuration": "预估时长(秒)",
 *         "genStatus": "[枚举]生成状态 [0=待生成 1=生成中 2=生成成功 3=生成失败]",
 *         "firstGenStatus": "是否为首次生成 [0=否 1=是]"
 *     }
 */
data class StoryboardListData(
    val dramasId: String? = null,
    val estimatedDuration: Int? = null,
    val firstGenStatus: Int? = null,//是否为首次生成 [0=否 1=是]
    val genStatus: Int? = null,//[枚举]生成状态 [0=待生成 1=生成中 2=生成成功 3=生成失败]
    val shotSegmentList: List<ShotSegmentInfo>? = null
) {
    fun isGenSuccess(): Boolean {
        return genStatus == 2
    }

    fun isGenFailed(): Boolean {
        return genStatus == 3
    }

    fun getTotalDuration(): Int {
        return shotSegmentList?.sumOf { it.estimatedDuration ?: 0 } ?: 0
    }
}

@Parcelize
data class ShotSegmentInfo(
    val estimatedDuration: Int? = null,
    var sequenceNumber: Int? = null,
    var shotSegmentId: String? = null,
    var sceneContent: String? = null
) : Parcelable

@Parcelize
data class ShotSegmentSceneContentInfo(
    val sceneContentId: String? = null,
    var sceneContent: String? = null,
    val sceneContentType: Int? = null//[枚举]场景内容类型 [1=画面 2=旁边 3=动作]
) : Parcelable

data class PointAmountData(
    val pointAmount: Int? = null//积分
)
package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import com.kuaigeglobal.epnova.common.Constants
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * {
 *     "storyboardId": "分镜主键id",
 *     "workId": "作品ID",
 *     "storyId": "剧集id",
 *     "sortNo": "序号",
 *     "creationMode": "[枚举]创作模式 [1=图生视频 2=首尾帧视频 3=融合生视频]",
 *     "storyBoardDesc": "分镜描述",
 *     "imageUrl": "当前图片url",
 *     "videoUrl": "当前视频url",
 *     "stepStatus": "[枚举]创作流程状态 [0=草稿 1=生成中 2=已完成 3=生成失败 4=已合成]",
 *     "duration": "视频时长(毫秒)",
 *     "taskId": "任务ID (雪花算法)",
 *     "taskType": "[枚举]任务类型 [1=文生文 2=文生图 3=图生图 4=文生视频 5图生视频 6-视频生视频]",
 *     "genStatusImage": "[枚举]图片生成状态 0=待生成 1=生成中 2=成功 3=失败 99=无需生成",
 *     "genStatusVideo": "[枚举]视频生成状态 0=待生成 1=生成中 2=成功 3=失败"
 * }
 */
@Parcelize
data class SimpleStoryboardInfo(
    /**
     * 分镜主键id
     */
    val storyboardId: String? = null,
    /**
     * 作品ID
     */
    val workId: String? = null,
    /**
     * 剧集id
     */
    val storyId: String? = null,
    /**
     * 序号
     */
    val sortNo: Int? = null,
    /**
     * 分镜描述
     */
    val storyBoardDesc: String? = null,
    /**
     * 当前图片url
     */
    val imageUrl: String? = null,
    /**
     * 当前视频url
     */
    var videoUrl: String? = null,
    /**
     * [枚举]创作流程状态 [0=草稿 1=生成中 2=已完成 3=生成失败 4=已合成]
     */
    private val stepStatus: Int? = null,
    /**
     * 视频时长(秒)
     */
    val duration: Int? = null,
    /**
     * 任务ID (雪花算法)
     */
    var taskId: String? = null,
    /**
     * [枚举]任务类型 [1=文生文 2=文生图 3=图生图 4=文生视频 5图生视频 6-视频生视频]
     */
    private val taskType: Int? = null,
    /**
     * [枚举]视频生成状态 0=待生成 1=生成中 2=成功 3=失败 5=重绘中 6=重绘完成
     */
    var genStatusVideo: Int? = null,
    /**
     * 分镜创建时间
     */
    val createTime : String? = null,
    /**
     * 视频是否含有水印：true=含水印（v1.4上线前生成），false=无水印，null=无法判断
     */
    val hasWatermark : Boolean? = null,
    /**
     * 漫剧类别 [storyboard_dynamic=动态漫 storyboard_narration=解说漫]
     */
    val comicType: String? = null,
) : Parcelable {

    val videoGenerating get() = genStatusVideo == 1 || genStatusVideo == 5

    val videoGenerated get() = genStatusVideo == 2 || genStatusVideo == 6

    val videoGenerateFailed get() = genStatusVideo == 3

    @IgnoredOnParcel
    var subtitle: SubtitleTaskStatusInfo? = null

    val subtitleGenerating get() = subtitle?.generating == true

    val subtitleGenerated get() = subtitle?.generated == true

    val subtitleGenerateFailed get() = subtitle?.generateFailed == true

    /**
     * 是解说漫
     */
    fun isNarration(): Boolean {
        return comicType == Constants.Model.STORYBOARD_NARRATION
    }

}
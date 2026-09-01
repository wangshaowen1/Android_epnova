package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.getResString
import kotlinx.parcelize.Parcelize

/**
 * 创作体系剧集模型。
 *
 * 该模型用于 TopEarners、MyCreations 和作品详情剧集列表。
 * 不要加入内容消费字段，例如点赞、收藏、锁集、解锁金币。
 */
@Parcelize
data class CreatorEpisodeInfo(
    /**所属作品 ID。*/
    var workId: String? = null,

    /**剧集 ID。*/
    val storyId: String? = null,

    /**剧集标题。*/
    val storyTitle: String? = null,

    /**剧集梗概。*/
    val storySummary: String? = null,

    /**剧集剧本内容。*/
    val storyContent: String? = null,

    /**剧集排序号。*/
    val sortNo: Int? = null,

    /**创作流程进度。*/
    val stepProgress: Int? = null,

    /**创作流程状态。*/
    val stepStatus: Int? = null,

    /**分镜数量。*/
    val storyboardTotal: Int? = null,

    /**当前剧集视频 URL。*/
    var videoUrl: String? = null,

    /**视频时长，当前创作接口按秒处理。*/
    var duration: Long? = null,

    /**剧集视频比例；为空时可以使用作品比例兜底。*/
    val workRatio: String? = null,

    /**剧集播放量。*/
    val playCount: Int? = null,

    /**续创剧集 ID，灵感续创保存剧本时使用。*/
    val sequelStoryId: String? = null,

    /**剧集或作品封面。*/
    val workCoverUrl: String? = null,

    /**剧集生成任务结果。*/
    val taskGenResultStoryVO: TaskGenResultStoryVO? = null,

    /**是否首次创建；1 表示首次创建。*/
    val firstCreationFlag: Int? = null,

    /**是否带水印，预留给 FastFilm 风格数据。*/
    val hasWatermark: Boolean? = null,

    /**生成失败标记，预留给 FastFilm 风格数据。*/
    val generationFailedFlag: Int? = null,

    /**生成进度详情，预留给 FastFilm 风格数据。*/
    val stepProgressDetail: Int? = null,

    /**生成进度副标题。*/
    val stepProgressSubTitle: String? = null,

    /**剧集发布审核状态。*/
    val publishStatus: Int? = null,

    /**剧集审核失败或驳回原因。*/
    val rejectReason: String? = null,

    /** 20260828 1.3.8新增。创作阶段展示状态：0=不展示，1=Creating，2=Completed，3=Failed。 */
    val creationStageStatus: Int? = null,

    /** 20260828 1.3.8新增。创作阶段英文展示文案，由后端聚合返回，例如 Script Creating、Asset Failed、Ready to Generate。 */
    val creationStageText: String? = null,

    /**剧集封面角标类型。*/
    val coverBadgeType: Int? = null
) : Parcelable {

    /**剧集生成任务结果，查询单集时可能返回资产或分镜生成结果。*/
    @Parcelize
    data class TaskGenResultStoryVO(
        /**业务类型，例如生成资产描述或分镜描述。*/
        val bizType: Int? = null,

        /**生成任务 ID。*/
        val taskId: String? = null,

        /**生成状态：待生成、生成中、成功、失败。*/
        val genStatus: Int? = null,

        /**任务结果，可能是资产、分镜图片 URL 等 JSON 字符串。*/
        val result: String? = null
    ) : Parcelable

    companion object {
        /**剧集进度：待创建。*/
        const val NOT_CREATED = 0

        /**剧集进度：剧本梗概生成。*/
        const val PROGRESS_SUMMARY_GENERATING = 10

        /**剧集进度：剧本生成中。*/
        const val PROGRESS_STORY_GENERATING = 15

        /**剧集进度：资产生成中。*/
        const val PROGRESS_ASSET_GENERATING = 20

        /**剧集进度：分镜描述生成中。*/
        const val PROGRESS_STORYBOARD_GENERATING_ASSET = 30

        /**剧集进度：分镜视频生成中。*/
        const val PROGRESS_STORYBOARD_GENERATING = 32

        /**剧集进度：待合成。*/
        const val PROGRESS_WAITING_COMPOSING = 35

        /**剧集进度：已完成。*/
        const val PROGRESS_COMPLETED = 40

        /**剧集进度：分镜重绘中。*/
        const val EPISODE_REPAINT_STATE = 45

        /**剧集进度：重绘已完成。*/
        const val EPISODE_REPAINT_COMPLETED = 50
    }

    /**剧集已发布或审核中时，剧集列表删除需要限制。*/
    fun isPublishedOrUnderReview(): Boolean {
        return publishStatus in listOf(CreatorPublishStatus.REVIEWING, CreatorPublishStatus.PASSED)
    }

    /**剧集发布角标文案。*/
    fun publishBadgeText(): String? {
        return when {
//            isDraft() -> R.string.drama_detail_draft.getResString()
            publishStatus == CreatorPublishStatus.UNPUBLISHED -> R.string.drama_detail_unpublished.getResString()
            publishStatus == CreatorPublishStatus.REVIEWING -> R.string.drama_detail_in_review.getResString()
            publishStatus == CreatorPublishStatus.PASSED -> R.string.drama_detail_live.getResString()
            publishStatus == CreatorPublishStatus.FAILED -> R.string.drama_detail_review_failed.getResString()
            else -> null
        }
    }

    /**剧集发布角标文字颜色。*/
    @ColorRes
    fun publishBadgeTextColor(): Int {
        return when {
//            isDraft() -> R.color.fixed_black_1
            publishStatus == CreatorPublishStatus.REVIEWING -> R.color.func_warning_dark
            publishStatus == CreatorPublishStatus.PASSED -> R.color.func_success_dark
            publishStatus == CreatorPublishStatus.FAILED -> R.color.func_failure_dark
            publishStatus == CreatorPublishStatus.UNPUBLISHED -> R.color.func_special_dark
            else -> R.color.fixed_white_1
        }
    }

    @ColorRes
    fun episodeStatusTextColor(): Int {
        return when (creationStageStatus) {
            SeriesEpisodeStatus.CREATING -> R.color.color_ffce2c
            SeriesEpisodeStatus.COMPLETED -> R.color.color_ffce2c
            SeriesEpisodeStatus.FAILED -> R.color.color_ef4444
            else -> R.color.fixed_white_1
        }
    }

    fun episodeShowStatus(): Boolean = creationStageStatus == SeriesEpisodeStatus.NONE || creationStageStatus == SeriesEpisodeStatus.CREATING

    fun episodeIsCreating(): Boolean = creationStageStatus == SeriesEpisodeStatus.CREATING

    /**剧集发布角标背景；草稿态使用浅底，其它发布态使用深色半透明底。*/
    @DrawableRes
    fun publishBadgeBackground(): Int {
        return if (isDraft()) {
            R.drawable.bg_fixed_white_1_round
        } else {
            R.drawable.bg_60_000000_round
        }
    }

    /**分镜视频生成或分镜重绘中，需要展示生成中状态。*/
    fun isGenerating(): Boolean {
        return stepProgress in listOf(PROGRESS_STORYBOARD_GENERATING, EPISODE_REPAINT_STATE)
    }

    /**生成态状态文案。*/
    fun statusCopy(): String {
        return when (stepProgress) {
            PROGRESS_STORYBOARD_GENERATING -> R.string.storyboard_generating.getResString()
            EPISODE_REPAINT_STATE -> R.string.video_redrawing.getResString()
            else -> R.string.video_synthesizing.getResString()
        }
    }

    /**待合成和重绘完成都属于可继续等待合成的状态。*/
    fun isWaitingComposing(): Boolean {
        return stepProgress in listOf(PROGRESS_WAITING_COMPOSING, EPISODE_REPAINT_COMPLETED)
    }

    /**剧集视频已完成。*/
    fun isCompleted(): Boolean {
        return stepProgress == PROGRESS_COMPLETED
    }

    /**剧本、资产、分镜描述生成阶段在 UI 上按草稿态展示。*/
    fun isDraft(): Boolean {
        return false
    }

    /**是否首次创建。*/
    fun isFirstCreated(): Boolean {
        return firstCreationFlag == 1
    }

    /**未创建包含真正未创建和梗概生成阶段。*/
    fun isNotCreated(): Boolean {
        return stepProgress == NOT_CREATED || stepProgress == PROGRESS_SUMMARY_GENERATING
    }

    fun isPassed(): Boolean {
        return publishStatus == CreatorPublishStatus.PASSED
    }

    /**已合成或重绘相关状态，旧 UI 用于判断是否已有可展示视频。*/
    val isSynthesised: Boolean
        get() = stepProgress in arrayOf(PROGRESS_COMPLETED, EPISODE_REPAINT_STATE, EPISODE_REPAINT_COMPLETED)
}

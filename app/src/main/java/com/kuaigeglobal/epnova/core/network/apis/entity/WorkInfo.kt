package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import androidx.room.TypeConverters
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.toUnitText
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.db.convert.Converters
import com.nhsd.basic.ext.getResString
import kotlinx.parcelize.Parcelize

@Parcelize
@TypeConverters(Converters::class)
data class WorkInfo(
    /**作品ID*/
    val workId: String? = null,
    /**作品名称*/
    val workTitle: String? = null,
    /**作品简介*/
    val workDesc: String? = null,
    /**作品风格code*/
    val workStyleCode: String? = null,
    /**视频比例：16:9、4:3、1:1、3:4、9:16、21:9、adaptive(自动)*/
    val workRatio: String? = null,
    /**作品灵感*/
    val workInspiration: String? = null,
    /**作品封面图URL（AI生成或默认图）*/
    val workCoverUrl: String? = null,
    /**作品类型：1-灵感创作;2-小说改编;3-小说上传*/
    val workType: Int? = null,
    /**[枚举]作品状态：0=未创建剧集 5-开始草稿 32=分镜生成中 35待合成 45=视频重绘中 50=视频重绘已完成 60=已完成 70=失败*/
    val workStatus: Int? = null,
    /**总集数（规划集数）*/
    val totalEpisodeCnt: Int? = null,
    /**已完成集数*/
    val finishedEpisodeCnt: Int? = null,
    /**最近生成的集数编号*/
    val lastEpisodeNo: Int? = null,
    /**最近一次生成时间*/
    val lastGenerateTime: String? = null,
    /**播放量(初始化1000-10000)*/
    val playCount: Int? = null,
    /**作者用户ID*/
    var userId: String? = null,
    /**剧集*/
    var workStoryItemList: List<EpisodeInfo>? = null,
    /**[枚举]签约状态(0-未申请 1-审核中 2-已通过 3-已驳回)*/
    val contractStatus: Int? = null,
    /**20260520 Epnova更新注释[枚举]发布状态(1=审核中 2=通过 3=审核失败（驳回/不通过）和最新的审核批次的状态一致（前端可用于剧集驳回原因取值判断）*/
    val publishStatus: Int? = null,
    /**20260520 Epnova驳回原因继续用这个（作品/剧集的角标查看驳回原因也是取这个字段，审核失败的统一展示一样的原因）驳回原因（审核失败）*/
    val rejectReason: String? = null,
    /**签约评级（S/A/B/C等）*/
    val contractLevel: String? = null,
    /**[枚举]小说上传模式下-第一集是否有剧本(0-没有 1-有)*/
    val novelUploadIsOneStoryContentStatus: Int? = null,
    /**发行平台图片 list*/
    val publishPlatformImageList: List<String>? = null,
    /**作品-自己发行流程Url*/
    val workSelfPublishProcessUrl: String? = null,
    /**20260520 作品-发行页面完整 Url（用于跳转 H5）*/
    val workContractPublishPageUrl: String? = null,
    /**20260520新增用于全部发行成功后置灰是否展示「去发行」按钮（0-不展示 1-展示，全部剧集已完结且全部发行置灰）*/
    val showPublishButton: Int? = null,
    /**20260520新增[枚举]全局发布状态：0=未发布, 1=审核中, 2=已发布（通过/已上架）, 3=审核失败（不通过）*/
    val globalPublishStatus: Int? = null,
    /**20260520新增已发布（审核通过）的剧集数量*/
    val publishedEpisodeCnt: Int? = null,
    /**20260520新增My List-My Creations 封面右上角角标类型：0=None,10=Draft,20=Unpublished,30=In Review,40=Review Failed,50=Published*/
    val coverBadgeType: Int? = null,
) : Parcelable {

    companion object {
        /**作品类型：1-灵感创作*/
        const val INSPIRATION_CREATION = 1

        /**作品类型：2-小说创作*/
        const val NOVEL_CREATION = 2

        /**作品类型：3-小说上传*/
        const val NOVEL_UPLOAD = 3

        /**作品状态 0=剧集未生成*/
        const val NOT_CREATED_EPISODE = 0

        /**作品状态 0=草稿*/
        const val DRAFT = 5

        /**作品状态 32=分镜生成中*/
        const val MIRROR_GENERATION_IN_PROGRESS = 32

        /**作品状态 35=待合成*/
        const val WAIT_STATE = 35

        /**作品状态 45=视频重绘中*/
        const val VIDEO_REDRAWING_IN_PROGRESS = 45

        /**作品状态 50=视频重绘已完成*/
        const val VIDEO_REDRAW_COMPLETED = 50

        /**已完成状态*/
        const val COMPLETED_STATE = 60

        /**失败状态*/
        const val FAILED_STATE = 70

        /**发布审核中*/
        const val PUBLISH_STATUS_REVIEWING = 1

        /**未发布*/
        const val PUBLISH_STATUS_UNPUBLISHED = 0

        /**发布审核通过*/
        const val PUBLISH_STATUS_PASSED = 2

        /**发布审核失败*/
        const val PUBLISH_STATUS_FAILED = 3

        /**全局发布状态：未发布*/
        const val GLOBAL_PUBLISH_STATUS_UNPUBLISHED = 0

        /**全局发布状态：审核中*/
        const val GLOBAL_PUBLISH_STATUS_REVIEWING = 1

        /**全局发布状态：已发布*/
        const val GLOBAL_PUBLISH_STATUS_PUBLISHED = 2

        /**全局发布状态：审核失败*/
        const val GLOBAL_PUBLISH_STATUS_FAILED = 3

        /**封面角标：无*/
        const val COVER_BADGE_NONE = 0

        /**封面角标：草稿*/
        const val COVER_BADGE_DRAFT = 10

        /**封面角标：未发布*/
        const val COVER_BADGE_UNPUBLISHED = 20

        /**封面角标：审核中*/
        const val COVER_BADGE_IN_REVIEW = 30

        /**封面角标：审核失败*/
        const val COVER_BADGE_REVIEW_FAILED = 40

        /**封面角标：已发布*/
        const val COVER_BADGE_PUBLISHED = 50

    }


    /**
     * 小说上传模式下-第一集是否有剧本
     * @return Boolean
     */
    fun isInspirationUploadMode(): Boolean {
        return novelUploadIsOneStoryContentStatus == 1
    }


    /**是否是分镜生成中，视频合成中，视频重绘中*/
    fun isWaitingComposing(): Boolean {
        return workStatus in listOf(MIRROR_GENERATION_IN_PROGRESS, VIDEO_REDRAWING_IN_PROGRESS)
    }

    fun statusCopy(): String {
        return when (workStatus) {
            MIRROR_GENERATION_IN_PROGRESS -> R.string.storyboard_generating.getResString()
            VIDEO_REDRAWING_IN_PROGRESS -> R.string.video_redrawing.getResString()
            else -> R.string.video_synthesizing.getResString()
        }
    }
    fun isNotCreatedEpisode(): Boolean {
        return workStatus == NOT_CREATED_EPISODE
    }

    //草稿状态
    fun isDraftStatus(): Boolean {
        return workStatus == DRAFT || workStatus == NOT_CREATED_EPISODE
    }

    fun seriesIntroduction(index: Int = 0): String? {
        return workStoryItemList?.getOrNull(index)?.storySummary
    }


    fun isInspirationGenerated(): Boolean {
        return workType == 1
    }


    fun isMine(): Boolean {
        return User.get().isSameId(userId)
    }

    fun isPublishedOrUnderReview(): Boolean {
        return publishStatus in listOf(PUBLISH_STATUS_REVIEWING, PUBLISH_STATUS_PASSED) ||
                globalPublishStatus in listOf(GLOBAL_PUBLISH_STATUS_REVIEWING, GLOBAL_PUBLISH_STATUS_PUBLISHED) ||
                coverBadgeType in listOf(COVER_BADGE_IN_REVIEW, COVER_BADGE_PUBLISHED)
    }

    fun hasPublishedOrUnderReviewEpisode(): Boolean {
        return isPublishedOrUnderReview() ||
                (publishedEpisodeCnt ?: 0) > 0 ||
                workStoryItemList.orEmpty().any { it.isPublishedOrUnderReview() }
    }

    fun showPublishButton(): Boolean {
        return showPublishButton == 1
    }

    fun isAllEpisodesPublished(): Boolean {
        val total = totalEpisodeCnt ?: return false
        if (total <= 0) return false
        return (publishedEpisodeCnt ?: 0) >= total
    }

    fun isPublishButtonEnabled(): Boolean {
        return showPublishButton() && !isAllEpisodesPublished()
    }

    fun isPublishReviewFailed(): Boolean {
        return coverBadgeType == COVER_BADGE_REVIEW_FAILED ||
                globalPublishStatus == GLOBAL_PUBLISH_STATUS_FAILED ||
                publishStatus == PUBLISH_STATUS_FAILED
    }

    fun publishBadgeText(): String? {
        return when {
            isPublishReviewFailed() -> R.string.drama_detail_review_failed.getResString()
            coverBadgeType == COVER_BADGE_DRAFT -> R.string.drama_detail_draft.getResString()
            coverBadgeType == COVER_BADGE_UNPUBLISHED -> R.string.drama_detail_unpublished.getResString()
            isDraftStatus() -> R.string.drama_detail_draft.getResString()
            globalPublishStatus == GLOBAL_PUBLISH_STATUS_UNPUBLISHED -> R.string.drama_detail_unpublished.getResString()
            coverBadgeType == COVER_BADGE_IN_REVIEW || globalPublishStatus == GLOBAL_PUBLISH_STATUS_REVIEWING ||
                    publishStatus == PUBLISH_STATUS_REVIEWING -> R.string.drama_detail_in_review.getResString()
            coverBadgeType == COVER_BADGE_PUBLISHED || globalPublishStatus == GLOBAL_PUBLISH_STATUS_PUBLISHED ||
                    publishStatus == PUBLISH_STATUS_PASSED || (publishedEpisodeCnt ?: 0) > 0 -> publishPublishedText()
            else -> null
        }
    }

    private fun publishPublishedText(): String {
        val prefix = R.string.drama_detail_published.getResString()
        val suffix = when {
            isAllEpisodesPublished() -> R.string.drama_detail_all_eps.getResString()
            (publishedEpisodeCnt ?: 0) > 0 -> {
                val count = publishedEpisodeCnt ?: 0
                val resId = if (count == 1) R.string.drama_detail_ep_1 else R.string.drama_detail_ep_2
                resId.getResString(count.toString())
            }
            else -> return prefix
        }
        return "$prefix · $suffix"
    }

    fun publishBadgeTextColor(): Int {
        return when {
            isPublishReviewFailed() -> R.color.func_failure_dark
            coverBadgeType == COVER_BADGE_DRAFT || isDraftStatus() -> R.color.fixed_black_1
            coverBadgeType == COVER_BADGE_IN_REVIEW || globalPublishStatus == GLOBAL_PUBLISH_STATUS_REVIEWING ||
                    publishStatus == PUBLISH_STATUS_REVIEWING -> R.color.func_warning_dark
            coverBadgeType == COVER_BADGE_PUBLISHED || globalPublishStatus == GLOBAL_PUBLISH_STATUS_PUBLISHED ||
                    publishStatus == PUBLISH_STATUS_PASSED || (publishedEpisodeCnt ?: 0) > 0 -> R.color.func_success_dark
            else -> R.color.fixed_white_1
        }
    }

    fun publishBadgeBackground(): Int {
        return if (coverBadgeType == COVER_BADGE_DRAFT || isDraftStatus()) {
            R.drawable.bg_fixed_white_1_round
        } else {
            R.drawable.bg_60_000000_round
        }
    }

    val stripedTrickTitle
        get() = workTitle?.replace("\n", "")?.replace("\r", "")

    fun getDuring(index: Int = 0): Long {
        return (workStoryItemList?.getOrNull(index)?.duration ?: 0L) * 1000L
    }

    fun getVideoCoverUrl(): String? {
        return workCoverUrl
    }

    fun getPlayUrl(index: Int = 0): String? {
        return workStoryItemList?.getOrNull(index)?.videoUrl
    }

    fun getFormatPlaycount(): String? {
        return playCount?.toUnitText()
    }

    fun getEpisodeId(index: Int = 0): String? {
        return workStoryItemList?.getOrNull(index)?.storyId
    }

    override fun toString(): String {
        return "DramaInfo(id=$workId, name=$workTitle, playUrl=${getPlayUrl()} })"
    }

    fun equalsWithMedias(other: WorkInfo?): Boolean {
        if (this != other) return false
        if (workId != other.workId) return false

        return true

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WorkInfo

        if (workType != other.workType) return false
        if (workStatus != other.workStatus) return false
        if (totalEpisodeCnt != other.totalEpisodeCnt) return false
        if (finishedEpisodeCnt != other.finishedEpisodeCnt) return false
        if (lastEpisodeNo != other.lastEpisodeNo) return false
        if (lastGenerateTime != other.lastGenerateTime) return false
        if (playCount != other.playCount) return false
        if (workId != other.workId) return false
        if (workTitle != other.workTitle) return false
        if (workDesc != other.workDesc) return false
        if (workStyleCode != other.workStyleCode) return false
        if (workRatio != other.workRatio) return false
        if (workInspiration != other.workInspiration) return false
        if (workCoverUrl != other.workCoverUrl) return false
        if (publishStatus != other.publishStatus) return false
        if (rejectReason != other.rejectReason) return false
        if (workContractPublishPageUrl != other.workContractPublishPageUrl) return false
        if (showPublishButton != other.showPublishButton) return false
        if (globalPublishStatus != other.globalPublishStatus) return false
        if (publishedEpisodeCnt != other.publishedEpisodeCnt) return false
        if (coverBadgeType != other.coverBadgeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = workType ?: 0
        result = 31 * result + (workStatus ?: 0)
        result = 31 * result + (totalEpisodeCnt ?: 0)
        result = 31 * result + (finishedEpisodeCnt ?: 0)
        result = 31 * result + (playCount ?: 0)
        result = 31 * result + (workId?.hashCode() ?: 0)
        result = 31 * result + (workTitle?.hashCode() ?: 0)
        result = 31 * result + (workDesc?.hashCode() ?: 0)
        result = 31 * result + (workStyleCode?.hashCode() ?: 0)
        result = 31 * result + (workRatio?.hashCode() ?: 0)
        result = 31 * result + (workInspiration?.hashCode() ?: 0)
        result = 31 * result + (workCoverUrl?.hashCode() ?: 0)
        result = 31 * result + (publishStatus ?: 0)
        result = 31 * result + (rejectReason?.hashCode() ?: 0)
        result = 31 * result + (workContractPublishPageUrl?.hashCode() ?: 0)
        result = 31 * result + (showPublishButton ?: 0)
        result = 31 * result + (globalPublishStatus ?: 0)
        result = 31 * result + (publishedEpisodeCnt ?: 0)
        result = 31 * result + (coverBadgeType ?: 0)
        return result
    }
}

/**
 *  {
 *                         "id": "所属剧集ID",
 *                         "playCount": "播放量(初始化1000-10000)",
 *                         "videoUrl": "当前视频url",
 *                         "duration": "视频时长(毫秒)",
 *                         "workRatio": "视频比例：16:9、4:3、1:1、3:4、9:16、21:9、adaptive(自动)"
 *                     }
 *                     {
 *                 "storyId": "剧集id",
 *                 "storyTitle": "剧集标题",
 *                 "storySummary": "剧集梗概",
 *                 "storyContent": "剧集剧本内容",
 *                 "sortNo": "序号",
 *                 "stepProgress": "[枚举]创作流程进度 [10=剧本输入 20=资产生成中 30=分镜生成中 40=已完成  35=待合成]",
 *                 "stepStatus": "[枚举]创作流程状态 [0=草稿 1=生成中 2=已完成 3=生成失败]",
 *                 "storyboardTotal": "分镜数",
 *                 "videoUrl": "当前视频url",
 *                 "duration": "视频时长(毫秒)",
 *                 "workRatio": "视频比例：16:9、4:3、1:1、3:4、9:16、21:9、adaptive(自动)",
 *                 "playCount": "播放量(初始化1000-10000)"
 *             }
 */
@Parcelize
data class EpisodeInfo(
    /**作品ID*/
    var workId: String? = null,
    /**剧集id*/
    val storyId: String? = null,
    /**剧集标题*/
    val storyTitle: String? = null,
    /**剧集梗概*/
    val storySummary: String? = null,
    /**剧集剧本内容*/
    val storyContent: String? = null,
    /**序号*/
    val sortNo: Int? = null,
    /**[枚举]创作流程进度 [0=待创建 10=剧本输入 15=剧本生成中 20=资产生成中 30=分镜描述生成中 32=分镜视频生成中35待合成 40=已完成 45=分镜重绘中 50=重绘已完成]20260311加0=待创建的状态，逻辑变更返回所有剧集*/
    val stepProgress: Int? = null,
    /**[枚举]创作流程状态 [0=草稿 1=生成中 2=已完成 3=生成失败]*/
    val stepStatus: Int? = null,
    /**分镜数*/
    val storyboardTotal: Int? = null,
    /**当前视频url*/
    var videoUrl: String? = null,
    /**视频时长(秒)*/
    var duration: Long? = null,
    /**视频比例：16:9、4:3、1:1、3:4、9:16、21:9、adaptive(自动)*/
    val workRatio: String? = null,
    /**播放量(初始化1000-10000)*/
    val playCount: Int? = null,
    /**续创剧集id（灵感续创才有的，小说没有，保存剧本需要带回来）*/
    val sequelStoryId: String? = null,
    /**封面*/
    val workCoverUrl: String? = null,
    /**剧集生成任务结果*/
    val taskGenResultStoryVO: TaskGenResultStoryVO? = null,
    /**是否首次创建（0=否删除过，1=是首次创建）*/
    val firstCreationFlag: Int? = null,
    /**20260520新增[枚举]剧集发布状态：0=未发布, 1=审核中, 2=已发布（通过/已上架）*/
    val publishStatus: Int? = null,
    /**20260520 驳回原因（审核失败）*/
    val rejectReason: String? = null,
    /**20260520 剧集封面角标：0=None,10=Draft,20=Unpublished,30=In Review,40=Review Failed,50=Published*/
    val coverBadgeType: Int? = null,

    val liked: Boolean? = null,
    val likeCount: Int? = null,
    /**锁定状态（0=免费集 1=已解锁 2=未解锁付费集）未登录用户，付费集统一返回 2*/
    val lockStatus: Int? = null,
    /**解锁费用（金币数），免费集为 0*/
    val costNums: Int? = null,
) : Parcelable {

    @Parcelize
    data class TaskGenResultStoryVO(
        /**业务类型 40=生成所有资产描述 70=生成所有分镜描述*/
        val bizType: Int? = null,
        /**任务id*/
        val taskId: String? = null,
        /**[枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败]*/
        val genStatus: Int? = null,
        /**查询单个是直接返回结果,比如资产、分镜图片url等*/
        val result: String? = null
    ) : Parcelable

    companion object {
        /**待创建*/
        const val NOT_CREATED = 0

        /**剧本梗概生成*/
        const val PROGRESS_SUMMARY_GENERATING = 10

        /**剧本生成中*/
        const val PROGRESS_STORY_GENERATING = 15

        /**资产生成中*/
        const val PROGRESS_ASSET_GENERATING = 20

        /**分镜生成中*/
        const val PROGRESS_STORYBOARD_GENERATING_ASSET = 30

        /**分镜视频生成中*/
        const val PROGRESS_STORYBOARD_GENERATING = 32

        /**待合成*/
        const val PROGRESS_WAITING_COMPOSING = 35

        /**已完成*/
        const val PROGRESS_COMPLETED = 40

        /**分镜重绘中*/
        const val EPISODE_REPAINT_STATE = 45

        /**重绘已完成*/
        const val EPISODE_REPAINT_COMPLETED = 50
    }

    fun formatEpisodeNo(): String {
        return R.string.episode_number_format.getResString(sortNo ?: 0)
    }

    fun isLocked(): Boolean {
        return lockStatus == 2
    }

    fun isPublishedOrUnderReview(): Boolean {
        return publishStatus in listOf(WorkInfo.PUBLISH_STATUS_REVIEWING, WorkInfo.PUBLISH_STATUS_PASSED) ||
                coverBadgeType in listOf(WorkInfo.COVER_BADGE_IN_REVIEW, WorkInfo.COVER_BADGE_PUBLISHED)
    }

    fun publishBadgeText(): String? {
        return when {
            coverBadgeType == WorkInfo.COVER_BADGE_DRAFT || isDraft() -> R.string.drama_detail_draft.getResString()
            coverBadgeType in listOf(WorkInfo.COVER_BADGE_UNPUBLISHED, WorkInfo.COVER_BADGE_REVIEW_FAILED) ||
                    (isCompleted() && publishStatus in listOf(WorkInfo.PUBLISH_STATUS_UNPUBLISHED, WorkInfo.PUBLISH_STATUS_FAILED)) -> {
                R.string.drama_detail_unpublished.getResString()
            }
            coverBadgeType == WorkInfo.COVER_BADGE_IN_REVIEW ||
                    publishStatus == WorkInfo.PUBLISH_STATUS_REVIEWING -> R.string.drama_detail_in_review.getResString()
            coverBadgeType == WorkInfo.COVER_BADGE_PUBLISHED ||
                    publishStatus == WorkInfo.PUBLISH_STATUS_PASSED -> R.string.drama_detail_published.getResString()
            else -> null
        }
    }

    fun publishBadgeTextColor(): Int {
        return when {
            coverBadgeType == WorkInfo.COVER_BADGE_DRAFT || isDraft() -> R.color.fixed_black_1
            coverBadgeType == WorkInfo.COVER_BADGE_IN_REVIEW ||
                    publishStatus == WorkInfo.PUBLISH_STATUS_REVIEWING -> R.color.func_warning_dark
            coverBadgeType == WorkInfo.COVER_BADGE_PUBLISHED ||
                    publishStatus == WorkInfo.PUBLISH_STATUS_PASSED -> R.color.func_success_dark
            else -> R.color.fixed_white_1
        }
    }

    fun publishBadgeBackground(): Int {
        return if (coverBadgeType == WorkInfo.COVER_BADGE_DRAFT || isDraft()) {
            R.drawable.bg_fixed_white_1_round
        } else {
            R.drawable.bg_60_000000_round
        }
    }


    /**
     * 是否是正在生成中
     * @return Boolean
     */
    fun isGenerating(): Boolean {
        return stepProgress in listOf(PROGRESS_STORYBOARD_GENERATING, EPISODE_REPAINT_STATE)
    }

    fun statusCopy(): String {
        return when (stepProgress) {
            PROGRESS_STORYBOARD_GENERATING -> R.string.storyboard_generating.getResString()
            EPISODE_REPAINT_STATE -> R.string.video_redrawing.getResString()
            else -> R.string.video_synthesizing.getResString()
        }
    }

    /**
     * 等待合成
     * @return Boolean
     */
    fun isWaitingComposing(): Boolean {
        return stepProgress in listOf(PROGRESS_WAITING_COMPOSING, EPISODE_REPAINT_COMPLETED)
    }

    /**
     * 已完成
     * @return Boolean
     */
    fun isCompleted(): Boolean {
        return stepProgress == PROGRESS_COMPLETED
    }

    /**
     * 是否是草稿
     * @return Boolean
     */
    fun isDraft(): Boolean {
        return stepProgress in listOf(
            PROGRESS_STORY_GENERATING,
            PROGRESS_ASSET_GENERATING,
            PROGRESS_STORYBOARD_GENERATING_ASSET
        )
    }

    /**
     * 是否是首次创建
     * @return Boolean
     */
    fun isFirstCreated(): Boolean {
        return firstCreationFlag == 1
    }

    /**
     * 是否是未创建
     * @return Boolean
     */
    fun isNotCreated(): Boolean {
        return stepProgress == NOT_CREATED || stepProgress == PROGRESS_SUMMARY_GENERATING
    }

    val isSynthesised get() = stepProgress in arrayOf(PROGRESS_COMPLETED, EPISODE_REPAINT_STATE, EPISODE_REPAINT_COMPLETED)
}

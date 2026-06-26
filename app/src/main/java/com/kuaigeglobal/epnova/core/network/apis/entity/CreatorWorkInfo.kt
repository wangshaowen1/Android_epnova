package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.room.TypeConverters
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.toUnitText
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.db.convert.CreatorConverters
import com.nhsd.basic.ext.getResString
import kotlinx.parcelize.Parcelize

/**
 * 创作体系作品模型。
 *
 * 该模型覆盖 TopEarners、MyCreations 和作品详情的未发行创作业务。
 * 不要放内容消费字段，例如锁集、金币解锁、点赞收藏等。
 */
@Parcelize
@TypeConverters(CreatorConverters::class)
data class CreatorWorkInfo(
    /**作品 ID。*/
    val workId: String? = null,

    /**作品标题。*/
    val workTitle: String? = null,

    /**作品简介。*/
    val workDesc: String? = null,

    /**作品风格 code。*/
    val workStyleCode: String? = null,

    /**作品默认视频比例，例如 16:9、3:4、9:16、adaptive。*/
    val workRatio: String? = null,

    /**作品灵感，创作流程中用于继续编辑或详情展示。*/
    val workInspiration: String? = null,

    /**作品封面图 URL。*/
    val workCoverUrl: String? = null,

    /**作品类型：灵感创作、小说改编、小说上传等。*/
    val workType: Int? = null,

    /**作品创作状态，例如草稿、生成中、已完成、失败。*/
    val workStatus: Int? = null,

    /**规划或总集数。*/
    val totalEpisodeCnt: Int? = null,

    /**已完成集数。*/
    val finishedEpisodeCnt: Int? = null,

    /**最近生成的集数编号。*/
    val lastEpisodeNo: Int? = null,

    /**最近一次生成时间。*/
    val lastGenerateTime: String? = null,

    /**播放量，TopEarners 和作品列表可展示。*/
    val playCount: Int? = null,

    /**作者用户 ID。*/
    var userId: String? = null,

    /**作品下的剧集列表；TopEarners 当前通常只有一集。*/
    var workStoryItemList: List<CreatorEpisodeInfo>? = null,

    /**签约状态，服务创作和发布流程。*/
    val contractStatus: Int? = null,

    /**作品最新审核批次发布状态。*/
    val publishStatus: Int? = null,

    /**审核失败或驳回原因，作品和剧集角标点击时展示。*/
    val rejectReason: String? = null,

    /**签约评级，例如 S/A/B/C。*/
    val contractLevel: String? = null,

    /**小说上传模式下第一集是否已有剧本。*/
    val novelUploadIsOneStoryContentStatus: Int? = null,

    /**发行平台图片列表，作品详情发布区域使用。*/
    val publishPlatformImageList: List<String>? = null,

    /**作品自发行流程 URL。*/
    val workSelfPublishProcessUrl: String? = null,

    /**作品发行页面完整 URL，用于跳转 H5。*/
    val workContractPublishPageUrl: String? = null,

    /**是否展示去发行按钮；1 表示展示。*/
    val showPublishButton: Int? = null,

    /**作品全局发布状态，聚合未发布、审核中、已发布、审核失败。*/
    val globalPublishStatus: Int? = null,

    /**已发布或审核通过的剧集数量。*/
    val publishedEpisodeCnt: Int? = null,

    /**MyCreations 封面右上角发布角标类型。*/
    val coverBadgeType: Int? = null,

    /**TopEarners/FastFilm 风格平台链接。*/
    val platformLinks: List<CreatorPlatformInfo>? = null,

    /**TopEarners 累计收益展示字段。*/
    val totalEarnings: String? = null,

    /**上架时间，预留给 FastFilm 风格数据。*/
    val shelfTime: Long? = null,

    /**生成失败标记，预留给 FastFilm 风格数据。*/
    val generationFailedFlag: Int? = null,

    /**生成状态标记，预留给 FastFilm 风格数据。*/
    val generationStatusFlag: Int? = null,

    /**生成进度副标题。*/
    val stepProgressSubTitle: String? = null
) : Parcelable {

    companion object {
        /**作品类型：灵感创作。*/
        const val INSPIRATION_CREATION = 1

        /**作品类型：小说创作。*/
        const val NOVEL_CREATION = 2

        /**作品类型：小说上传。*/
        const val NOVEL_UPLOAD = 3

        /**作品状态：未创建剧集。*/
        const val NOT_CREATED_EPISODE = 0

        /**作品状态：草稿。*/
        const val DRAFT = 5

        /**作品状态：分镜生成中。*/
        const val MIRROR_GENERATION_IN_PROGRESS = 32

        /**作品状态：待合成。*/
        const val WAIT_STATE = 35

        /**作品状态：视频重绘中。*/
        const val VIDEO_REDRAWING_IN_PROGRESS = 45

        /**作品状态：视频重绘已完成。*/
        const val VIDEO_REDRAW_COMPLETED = 50

        /**作品状态：已完成。*/
        const val COMPLETED_STATE = 60

        /**作品状态：失败。*/
        const val FAILED_STATE = 70
    }

    /**小说上传模式下，第一集已有剧本时才能直接进入后续创作流程。*/
    fun isInspirationUploadMode(): Boolean {
        return novelUploadIsOneStoryContentStatus == 1
    }

    /**作品正在分镜生成或视频重绘时，列表需要展示生成态文案。*/
    fun isWaitingComposing(): Boolean {
        return workStatus in listOf(MIRROR_GENERATION_IN_PROGRESS, VIDEO_REDRAWING_IN_PROGRESS)
    }

    /**生成态状态文案。*/
    fun statusCopy(): String {
        return when (workStatus) {
            MIRROR_GENERATION_IN_PROGRESS -> R.string.storyboard_generating.getResString()
            VIDEO_REDRAWING_IN_PROGRESS -> R.string.video_redrawing.getResString()
            else -> R.string.video_synthesizing.getResString()
        }
    }

    /**作品还没有创建任何剧集。*/
    fun isNotCreatedEpisode(): Boolean {
        return workStatus == NOT_CREATED_EPISODE
    }

    /**草稿态包含真正草稿和未创建剧集两类状态。*/
    fun isDraftStatus(): Boolean {
        return workStatus == DRAFT || workStatus == NOT_CREATED_EPISODE
    }

    /**取指定剧集简介，作品详情默认展示第一集简介。*/
    fun seriesIntroduction(index: Int = 0): String? {
        return workStoryItemList?.getOrNull(index)?.storySummary
    }

    /**是否为灵感创作作品。*/
    fun isInspirationGenerated(): Boolean {
        return workType == INSPIRATION_CREATION
    }

    /**是否当前登录用户自己的创作作品。*/
    fun isMine(): Boolean {
        return User.get().isSameId(userId)
    }

    /**
     * 作品自身是否已经发布通过或处于审核中。
     *
     * 这里不看已发布集数；如果需要判断“作品或任一剧集不可删”，使用 hasPublishedOrUnderReviewEpisode。
     */
    fun isPublishedOrUnderReview(): Boolean {
        return publishStatus in listOf(CreatorPublishStatus.REVIEWING, CreatorPublishStatus.PASSED) ||
                globalPublishStatus in listOf(CreatorPublishStatus.REVIEWING, CreatorPublishStatus.PUBLISHED) ||
                coverBadgeType in listOf(CreatorCoverBadgeType.IN_REVIEW, CreatorCoverBadgeType.PUBLISHED)
    }

    /**作品本身或任一剧集已发布/审核中时，删除和批量管理需要限制。*/
    fun hasPublishedOrUnderReviewEpisode(): Boolean {
        return isPublishedOrUnderReview() ||
                (publishedEpisodeCnt ?: 0) > 0 ||
                workStoryItemList.orEmpty().any { it.isPublishedOrUnderReview() }
    }

    /**后端控制是否展示去发行按钮。*/
    fun showPublishButton(): Boolean {
        return showPublishButton == 1
    }

    /**规划集数都已经发布时，发行按钮需要置灰。*/
    fun isAllEpisodesPublished(): Boolean {
        val total = totalEpisodeCnt ?: return false
        if (total <= 0) return false
        return (publishedEpisodeCnt ?: 0) >= total
    }

    /**去发行按钮既要后端允许展示，也不能是全部剧集已发布。*/
    fun isPublishButtonEnabled(): Boolean {
        return showPublishButton() && !isAllEpisodesPublished()
    }

    /**作品详情/MyCreations 审核失败角标，用于颜色、文案和驳回原因入口。*/
    fun isPublishReviewFailed(): Boolean {
        return coverBadgeType == CreatorCoverBadgeType.REVIEW_FAILED
    }

    /**发布角标文案，作品详情和 MyCreations 按 coverBadgeType 展示。*/
    fun publishBadgeText(): String? {
        return when (coverBadgeType) {
            CreatorCoverBadgeType.DRAFT -> R.string.drama_detail_draft.getResString()
            CreatorCoverBadgeType.UNPUBLISHED -> R.string.drama_detail_unpublished.getResString()
            CreatorCoverBadgeType.IN_REVIEW -> R.string.drama_detail_in_review.getResString()
            CreatorCoverBadgeType.REVIEW_FAILED -> R.string.drama_detail_review_failed.getResString()
            CreatorCoverBadgeType.PUBLISHED -> R.string.drama_detail_published.getResString()
            else -> null
        }
    }

    fun publishSeriesStatusText(): String? {
        return when (coverBadgeType) {
            CreatorCoverBadgeType.DRAFT -> R.string.drama_detail_draft.getResString()
            CreatorCoverBadgeType.UNPUBLISHED -> R.string.drama_detail_unpublished.getResString()
            CreatorCoverBadgeType.IN_REVIEW -> R.string.drama_detail_in_review.getResString()
            CreatorCoverBadgeType.REVIEW_FAILED -> R.string.drama_detail_review_failed.getResString()
            CreatorCoverBadgeType.PUBLISHED -> {
                if (totalEpisodeCnt == publishedEpisodeCnt) {
                    "${R.string.drama_detail_published.getResString()} · ${R.string.drama_detail_all_eps.getResString()}"
                } else {
                    "${R.string.drama_detail_published.getResString()} · ${R.string.drama_detail_ep_2.getResString(publishedEpisodeCnt ?: 0)}"
                }
            }
            else -> null
        }
    }

    /**发布角标文字颜色。*/
    @ColorRes
    fun publishBadgeTextColor(): Int {
        return when (coverBadgeType) {
            CreatorCoverBadgeType.DRAFT -> R.color.fixed_black_1
            CreatorCoverBadgeType.UNPUBLISHED -> R.color.func_special_dark
            CreatorCoverBadgeType.IN_REVIEW -> R.color.func_warning_dark
            CreatorCoverBadgeType.REVIEW_FAILED -> R.color.func_failure_dark
            CreatorCoverBadgeType.PUBLISHED -> R.color.func_success_dark
            else -> R.color.fixed_white_1
        }
    }

    /**发布角标背景；草稿态使用浅底，其它发布态使用深色半透明底。*/
    @DrawableRes
    fun publishBadgeBackground(): Int {
        return if (coverBadgeType == CreatorCoverBadgeType.DRAFT) {
            R.drawable.bg_fixed_white_1_round
        } else {
            R.drawable.bg_60_000000_round
        }
    }

    fun getDoneProgressFormat(): String {
        return if (totalEpisodeCnt == 1) {
            R.string.single_episode_short.getResString()
        } else {
            R.string.my_list_ep_done_ratio.getResString(finishedEpisodeCnt, totalEpisodeCnt)
        }
    }

    fun getCreationFormat(): String {
        val finish = finishedEpisodeCnt ?: 0
        val total = totalEpisodeCnt ?: 0

        val n = if (finish < 2) {
            R.string.drama_created_ep_singular.getResString(finish)
        } else {
            R.string.drama_created_ep_plural.getResString(finish)
        }

        val m = if (total < 2) {
            R.string.drama_total_ep_singular.getResString(total)
        } else {
            R.string.drama_total_ep_plural.getResString(total)
        }
        return "$n / $m"
    }

    /**去除标题中的换行，适配列表单行或双行展示。*/
    val stripedTrickTitle: String?
        get() = workTitle?.replace("\n", "")?.replace("\r", "")

    /**取指定剧集时长并转毫秒；旧接口 duration 按秒返回。*/
    fun getDuring(index: Int = 0): Long {
        return (workStoryItemList?.getOrNull(index)?.duration ?: 0L) * 1000L
    }

    /**播放器或列表封面兜底图。*/
    fun getVideoCoverUrl(): String? {
        return workCoverUrl
    }

    /**取指定剧集播放地址。*/
    fun getPlayUrl(index: Int = 0): String? {
        return workStoryItemList?.getOrNull(index)?.videoUrl
    }

    /**格式化播放量，例如 7.6K。*/
    fun getFormatPlaycount(): String? {
        return playCount?.toUnitText()
    }

    /**TopEarners 收益展示，后端已格式化成字符串。*/
    fun getTotalEarningsText(): String {
        return totalEarnings.orEmpty()
    }

    /**取指定剧集 ID。*/
    fun getEpisodeId(index: Int = 0): String? {
        return workStoryItemList?.getOrNull(index)?.storyId
    }

    fun isPassed(): Boolean {
        return publishStatus == CreatorPublishStatus.PASSED ||
                globalPublishStatus == CreatorPublishStatus.PUBLISHED ||
                coverBadgeType == CreatorPublishStatus.PUBLISHED
    }

}

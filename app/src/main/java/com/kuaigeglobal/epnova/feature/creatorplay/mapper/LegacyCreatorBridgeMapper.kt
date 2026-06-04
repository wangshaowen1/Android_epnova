package com.kuaigeglobal.epnova.feature.creatorplay.mapper

import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorEpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo

/**
 * 旧 WorkInfo/EpisodeInfo 到 Creator 模型的临时桥接。
 *
 * 只用于接口和页面尚未完全切到 Creator DTO 的过渡阶段。
 * 旧 DTO 不允许越过这个 mapper 继续传到新播放器、Adapter 或 PlayerManager。
 */
object LegacyCreatorBridgeMapper {

    /**桥接旧作品模型，保留创作和发布审核字段，过滤掉内容消费语义。*/
    fun toCreatorWorkInfo(work: WorkInfo?): CreatorWorkInfo? {
        work ?: return null
        return CreatorWorkInfo(
            workId = work.workId,
            workTitle = work.workTitle,
            workDesc = work.workDesc,
            workStyleCode = work.workStyleCode,
            workRatio = work.workRatio,
            workInspiration = work.workInspiration,
            workCoverUrl = work.workCoverUrl,
            workType = work.workType,
            workStatus = work.workStatus,
            totalEpisodeCnt = work.totalEpisodeCnt,
            finishedEpisodeCnt = work.finishedEpisodeCnt,
            lastEpisodeNo = work.lastEpisodeNo,
            lastGenerateTime = work.lastGenerateTime,
            playCount = work.playCount,
            userId = work.userId,
            workStoryItemList = work.workStoryItemList.orEmpty().mapNotNull { episode ->
                toCreatorEpisodeInfo(episode, work.workId)
            },
            contractStatus = work.contractStatus,
            publishStatus = work.publishStatus,
            rejectReason = work.rejectReason,
            contractLevel = work.contractLevel,
            novelUploadIsOneStoryContentStatus = work.novelUploadIsOneStoryContentStatus,
            publishPlatformImageList = work.publishPlatformImageList,
            workSelfPublishProcessUrl = work.workSelfPublishProcessUrl,
            workContractPublishPageUrl = work.workContractPublishPageUrl,
            showPublishButton = work.showPublishButton,
            globalPublishStatus = work.globalPublishStatus,
            publishedEpisodeCnt = work.publishedEpisodeCnt,
            coverBadgeType = work.coverBadgeType
        )
    }

    /**桥接旧剧集模型，只复制创作流程字段，不复制内容消费字段。*/
    fun toCreatorEpisodeInfo(episode: EpisodeInfo?, workIdFallback: String? = null): CreatorEpisodeInfo? {
        episode ?: return null
        return CreatorEpisodeInfo(
            workId = episode.workId ?: workIdFallback,
            storyId = episode.storyId,
            storyTitle = episode.storyTitle,
            storySummary = episode.storySummary,
            storyContent = episode.storyContent,
            sortNo = episode.sortNo,
            stepProgress = episode.stepProgress,
            stepStatus = episode.stepStatus,
            storyboardTotal = episode.storyboardTotal,
            videoUrl = episode.videoUrl,
            duration = episode.duration,
            workRatio = episode.workRatio,
            playCount = episode.playCount,
            sequelStoryId = episode.sequelStoryId,
            workCoverUrl = episode.workCoverUrl,
            taskGenResultStoryVO = episode.taskGenResultStoryVO?.toCreatorTaskGenResultStoryVO(),
            firstCreationFlag = episode.firstCreationFlag,
            publishStatus = episode.publishStatus,
            rejectReason = episode.rejectReason,
            coverBadgeType = episode.coverBadgeType
        )
    }

    /**批量桥接旧剧集列表，常用于作品详情 episodeList 接口迁移期。*/
    fun toCreatorEpisodeInfos(
        episodes: List<EpisodeInfo>?,
        workIdFallback: String? = null
    ): List<CreatorEpisodeInfo> {
        return episodes.orEmpty().mapNotNull { toCreatorEpisodeInfo(it, workIdFallback) }
    }

    /**
     * Creator 作品到旧编辑页参数的显式桥接。
     *
     * 仅用于 AppRouter、作品信息编辑等尚未切 Creator 的旧入口；不要把转换结果交给新播放器。
     */
    fun toLegacyWorkInfo(work: CreatorWorkInfo?): WorkInfo? {
        work ?: return null
        return WorkInfo(
            workId = work.workId,
            workTitle = work.workTitle,
            workDesc = work.workDesc,
            workStyleCode = work.workStyleCode,
            workRatio = work.workRatio,
            workInspiration = work.workInspiration,
            workCoverUrl = work.workCoverUrl,
            workType = work.workType,
            workStatus = work.workStatus,
            totalEpisodeCnt = work.totalEpisodeCnt,
            finishedEpisodeCnt = work.finishedEpisodeCnt,
            lastEpisodeNo = work.lastEpisodeNo,
            lastGenerateTime = work.lastGenerateTime,
            playCount = work.playCount,
            userId = work.userId,
            workStoryItemList = toLegacyEpisodeInfos(work.workStoryItemList, work.workId),
            contractStatus = work.contractStatus,
            publishStatus = work.publishStatus,
            rejectReason = work.rejectReason,
            contractLevel = work.contractLevel,
            novelUploadIsOneStoryContentStatus = work.novelUploadIsOneStoryContentStatus,
            publishPlatformImageList = work.publishPlatformImageList,
            workSelfPublishProcessUrl = work.workSelfPublishProcessUrl,
            workContractPublishPageUrl = work.workContractPublishPageUrl,
            showPublishButton = work.showPublishButton,
            globalPublishStatus = work.globalPublishStatus,
            publishedEpisodeCnt = work.publishedEpisodeCnt,
            coverBadgeType = work.coverBadgeType
        )
    }

    /**Creator 剧集到旧编辑页参数的显式桥接，不补旧播放器内容消费字段。*/
    fun toLegacyEpisodeInfo(episode: CreatorEpisodeInfo?, workIdFallback: String? = null): EpisodeInfo? {
        episode ?: return null
        return EpisodeInfo(
            workId = episode.workId ?: workIdFallback,
            storyId = episode.storyId,
            storyTitle = episode.storyTitle,
            storySummary = episode.storySummary,
            storyContent = episode.storyContent,
            sortNo = episode.sortNo,
            stepProgress = episode.stepProgress,
            stepStatus = episode.stepStatus,
            storyboardTotal = episode.storyboardTotal,
            videoUrl = episode.videoUrl,
            duration = episode.duration,
            workRatio = episode.workRatio,
            playCount = episode.playCount,
            sequelStoryId = episode.sequelStoryId,
            workCoverUrl = episode.workCoverUrl,
            taskGenResultStoryVO = episode.taskGenResultStoryVO?.toLegacyTaskGenResultStoryVO(),
            firstCreationFlag = episode.firstCreationFlag,
            publishStatus = episode.publishStatus,
            rejectReason = episode.rejectReason,
            coverBadgeType = episode.coverBadgeType
        )
    }

    /**批量桥接 Creator 剧集列表到旧编辑页参数。*/
    fun toLegacyEpisodeInfos(
        episodes: List<CreatorEpisodeInfo>?,
        workIdFallback: String? = null
    ): List<EpisodeInfo> {
        return episodes.orEmpty().mapNotNull { toLegacyEpisodeInfo(it, workIdFallback) }
    }

    /**生成任务结果只复制创作流程字段，不带任何内容消费字段。*/
    private fun EpisodeInfo.TaskGenResultStoryVO.toCreatorTaskGenResultStoryVO(): CreatorEpisodeInfo.TaskGenResultStoryVO {
        return CreatorEpisodeInfo.TaskGenResultStoryVO(
            bizType = bizType,
            taskId = taskId,
            genStatus = genStatus,
            result = result
        )
    }

    /**生成任务结果回传旧编辑页，只复制创作流程字段。*/
    private fun CreatorEpisodeInfo.TaskGenResultStoryVO.toLegacyTaskGenResultStoryVO(): EpisodeInfo.TaskGenResultStoryVO {
        return EpisodeInfo.TaskGenResultStoryVO(
            bizType = bizType,
            taskId = taskId,
            genStatus = genStatus,
            result = result
        )
    }
}

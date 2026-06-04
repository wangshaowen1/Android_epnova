package com.kuaigeglobal.epnova.feature.editor

import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorEpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlaySource
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlayableEpisode
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorActivity

/**
 * Creator 播放器到现有分镜视频编辑页的隔离入口。
 *
 * EditStoryboardVideoActivity 当前仍消费旧 EpisodeInfo，这里集中做一次最小字段桥接，
 * 避免 feature/creatorplay 直接 import 旧播放/内容消费模型。
 */
object CreatorStoryboardEditorLauncher {

    /**进入分镜视频列表页；真正重绘某个分镜仍由 EditStoryboardVideoActivity 内部处理。*/
    fun start(item: CreatorPlayableEpisode) {
        val workId = item.workContext.workId ?: item.episode.workId
        EditStoryboardVideoActivity.start(
            storyTitle = item.workContext.workTitle ?: item.episode.storyTitle,
            workId = workId,
            storyId = item.episode.storyId,
            workRatio = item.episode.workRatio ?: item.workContext.workRatio,
            story = item.episode.toLegacyEpisodeInfo(workId),
            from = item.editorFromPage(),
            model = item.editorModel()
        )
    }

    /**编辑页返回逻辑依赖来源标记；MyCreations 和作品详情分别回到自己的列表边界。*/
    private fun CreatorPlayableEpisode.editorFromPage(): String? {
        return when (source) {
            CreatorPlaySource.MY_CREATIONS -> StoryEditorActivity.FROM_MY_WORKS
            CreatorPlaySource.WORK_SERIES -> StoryEditorActivity.FROM_SERIES
            CreatorPlaySource.TOP_EARNERS -> null
        }
    }

    /**MyCreations 单集作品按单集模式回退；作品详情剧集列表沿用编辑页默认剧集模式。*/
    private fun CreatorPlayableEpisode.editorModel(): Int? {
        return when (source) {
            CreatorPlaySource.MY_CREATIONS -> DramaGenerateConstants.MODEL_STORY_SINGLE
            CreatorPlaySource.TOP_EARNERS,
            CreatorPlaySource.WORK_SERIES -> null
        }
    }

    /**只桥接编辑页实际需要的 Creator episode 字段，不带旧播放器的点赞、锁集或解锁字段。*/
    private fun CreatorEpisodeInfo.toLegacyEpisodeInfo(workId: String?): EpisodeInfo {
        return EpisodeInfo(
            workId = workId,
            storyId = storyId,
            storyTitle = storyTitle,
            storySummary = storySummary,
            storyContent = storyContent,
            sortNo = sortNo,
            stepProgress = stepProgress,
            stepStatus = stepStatus,
            storyboardTotal = storyboardTotal,
            videoUrl = videoUrl,
            duration = duration,
            workRatio = workRatio,
            playCount = playCount,
            sequelStoryId = sequelStoryId,
            workCoverUrl = workCoverUrl,
            taskGenResultStoryVO = taskGenResultStoryVO?.toLegacyTaskResult(),
            firstCreationFlag = firstCreationFlag,
            publishStatus = publishStatus,
            rejectReason = rejectReason,
            coverBadgeType = coverBadgeType
        )
    }

    /**编辑页只需要任务状态和结果，Creator 预留字段按同名结构桥接。*/
    private fun CreatorEpisodeInfo.TaskGenResultStoryVO.toLegacyTaskResult(): EpisodeInfo.TaskGenResultStoryVO {
        return EpisodeInfo.TaskGenResultStoryVO(
            bizType = bizType,
            taskId = taskId,
            genStatus = genStatus,
            result = result
        )
    }
}

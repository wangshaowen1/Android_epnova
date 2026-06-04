package com.kuaigeglobal.epnova.feature.creatorplay

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlayableEpisode

/**
 * 新创作播放器 item VM。
 *
 * XML 只绑定这个 VM，不直接依赖 CreatorPlayableEpisode，后续 UI 字段调整集中在这里处理。
 */
class ItemCreatorVideoFeedVm(data: CreatorPlayableEpisode?) : AppItemViewModel<CreatorPlayableEpisode>(data) {

    /**中心播放按钮可见性，只在暂停态显示。*/
    val showPauseButton = ObservableBoolean(false)

    /**loading 可见性，只表达播放器缓冲或 seek 中。*/
    val showBuffering = ObservableBoolean(false)

    /**错误文案可见性，播放器无法播放当前视频时显示。*/
    val showError = ObservableBoolean(false)

    /**错误文案，由 PlayerManager 提供轻量错误描述。*/
    val errorText = ObservableField("")

    /**标题优先取剧集标题，缺失时使用作品标题兜底。*/
    fun title(): String? {
        val item = data.get()
        return item?.episode?.storyTitle ?: item?.workContext?.workTitle
    }

    /**简介优先取剧集梗概，缺失时使用作品简介兜底，保持 FastFilm 播放器的信息层级。*/
    fun description(): String? {
        val item = data.get()
        return item?.episode?.storySummary ?: item?.workContext?.workDesc
    }

    /**简介为空时隐藏描述区域，避免播放器底部留下无意义空白。*/
    fun hasDescription(): Boolean {
        return description().isNullOrBlank().not()
    }

    /**视频时长毫秒值；接口模型里的 duration 按秒保存。*/
    fun durationMs(): Long {
        return (data.get()?.episode?.duration ?: 0L) * 1000L
    }

    /**ViewHolder 绑定新数据时重置播放器 UI 状态，避免 RecyclerView 复用残留。*/
    fun resetPlayerUi() {
        showPauseButton.set(false)
        showBuffering.set(false)
        showError.set(false)
        errorText.set("")
    }

    /**把播放器状态收敛成 item UI 字段，Adapter 不直接散落状态判断。*/
    fun applyPlayerState(state: CreatorFeedPlayerManager.CreatorPlayerState) {
        showError.set(state is CreatorFeedPlayerManager.CreatorPlayerState.Error)
        showBuffering.set(state is CreatorFeedPlayerManager.CreatorPlayerState.Buffering)
        showPauseButton.set(state is CreatorFeedPlayerManager.CreatorPlayerState.Pause)

        if (state is CreatorFeedPlayerManager.CreatorPlayerState.Error) {
            errorText.set(state.msg)
        } else {
            errorText.set("")
        }
    }

    companion object {
        /**播放 feed 的稳定 key，优先用 feedKey，缺失时降级到 storyId/workId/feedIndex。*/
        fun stableKey(item: CreatorPlayableEpisode?): String? {
            item ?: return null
            return item.feedKey ?: item.episode.storyId ?: item.workContext.workId ?: item.feedIndex.toString()
        }
    }
}

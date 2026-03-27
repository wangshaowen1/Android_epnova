package com.kuaigeglobal.epnova.feature.play

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo // 注意导入正确的作品实体

class ItemVideoFeedVm(info: EpisodeInfo?) : AppItemViewModel<EpisodeInfo>(info) {

    // --- 作品级全局状态 ---
    val workInfo = ObservableField<WorkVOInfo>()

    // --- 互动区状态 (单集) ---
    val isLiked = ObservableBoolean(false)
    val likeCount = ObservableField("0")

    // --- 互动区状态 (作品) ---
    val isCollected = ObservableBoolean(false)
    val collectCount = ObservableField("0")

    // --- 播放器 UI 状态 ---
    val showBuffering = ObservableBoolean(false)
    val showPauseBtn = ObservableBoolean(false)

    // --- 交互桥梁 ---
    var actionListener: VideoFeedAdapter.InteractionListener? = null
    // 使用高阶函数动态获取准确的 position，防止 RecyclerView 复用导致错位
    var positionProvider: () -> Int = { -1 }

    /** 同步剧集点赞状态 */
    fun syncLike(item: EpisodeInfo?) {
        isLiked.set(item?.liked == true)
        likeCount.set(item?.likeCount?.toString() ?: "0")
    }

    /** 同步全局作品状态 */
    fun syncWorkInfo(work: WorkVOInfo?) {
        workInfo.set(work)
        isCollected.set(work?.collected == true)
        collectCount.set(work?.collectCount?.toString() ?: "0")
    }

    // --- 供 XML 直接绑定的点击事件 ---
    fun onClickPlay() = actionListener?.onTogglePlay(positionProvider())
    fun onClickCollect() = actionListener?.onToggleCollect(positionProvider())
    fun onClickLike() = actionListener?.onToggleLike(positionProvider())
    fun onClickEpisodeSelect() = actionListener?.showEpisodeSelectDialog(positionProvider())
}
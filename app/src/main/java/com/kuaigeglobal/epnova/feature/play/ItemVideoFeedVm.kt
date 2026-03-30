package com.kuaigeglobal.epnova.feature.play

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.common.ext.toUnitText
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo // 注意导入正确的作品实体
import com.nhsd.basic.utils.Logger

class ItemVideoFeedVm(info: EpisodeInfo?) : AppItemViewModel<EpisodeInfo>(info) {

    // --- 作品级全局状态 ---
    val workInfo = ObservableField<WorkVOInfo>()

    val isHideViews = ObservableBoolean(false)

    // --- 互动区状态 (单集) ---
    val isLiked = ObservableBoolean(false)
    val likeCount = ObservableField("0")

    // --- 互动区状态 (作品) ---
    val isCollected = ObservableBoolean(false)
    val collectCount = ObservableField("0")

    // --- 播放器 UI 状态 ---
    val showBuffering = ObservableBoolean(false)

    // 【新增】是否正在播放（用于切换中心按钮的 Icon：播放/暂停）
    val isPlaying = ObservableBoolean(true)

    // 【新增】控制中心播放/暂停按钮的显隐（规则1：首次进入不展示；规则3：点击空白区展示）
    val showCenterPlayBtn = ObservableBoolean(false)

    // --- 交互桥梁 ---
    var actionListener: VideoFeedAdapter.InteractionListener? = null

    // 使用高阶函数动态获取准确的 position，防止 RecyclerView 复用导致错位
    var positionProvider: () -> Int = { -1 }

    // 【新增】暴露给 Adapter/ViewHolder 内部处理特殊 UI 逻辑的回调
    var onBlankAreaClicked: (() -> Unit)? = null

    /** 同步剧集点赞状态 */
    fun syncLike(item: EpisodeInfo?) {
        Logger.d("更新点赞状态", "item.like=${item?.liked}, item.likeCount=${item?.likeCount}")
        item?.liked?.let { isLiked.set(it) }
        item?.likeCount?.let { count ->
            likeCount.set(count.toUnitText())
        }
    }

    /** 同步全局作品状态 */
    fun syncWorkInfo(work: WorkVOInfo?) {
        workInfo.set(work)
        isCollected.set(work?.collected == true)
        collectCount.set(work?.collectCount?.toUnitText())
    }

    fun onClickBlankArea() {
//        onBlankAreaClicked?.invoke()
        actionListener?.onTogglePlay(positionProvider())
    }

    // --- 供 XML 直接绑定的点击事件 ---
    fun onClickPlay() = actionListener?.onTogglePlay(positionProvider())
    fun onClickCollect() = actionListener?.onToggleCollect(positionProvider())
    fun onClickLike() = actionListener?.onToggleLike(positionProvider())
    fun onClickEpisodeSelect() = actionListener?.showEpisodeSelectDialog(positionProvider())
}
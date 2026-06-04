package com.kuaigeglobal.epnova.feature.creatorplay

import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlaySource
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlayableEpisode

/**
 * 新创作播放器 VM。
 *
 * 这里只保存播放器入口和当前播放态，不承载旧播放器的解锁、点赞、收藏或选集状态。
 */
class CreatorPlayerViewModel : AppRepositoryViewModel<AppRepository>() {

    /**新播放器复用项目通用 toolbar，只保留返回按钮，不展示标题和菜单。*/
    val toolbar = ToolbarViewModel(
        navigationText = null,
        navigationAction = { back() },
        title = null
    )

    /**播放来源决定切换边界：TopEarners 可跨作品，MyCreations/作品详情只在当前作品内播放。*/
    var playSource: CreatorPlaySource = CreatorPlaySource.TOP_EARNERS

    /**入口传入的初始位置，只用于播放器首次渲染后定位。*/
    var initialPosition: Int = 0

    /**当前正在播放的 Creator 播放单元，供后续标题、埋点或调试状态使用。*/
    val currentPlayableEpisode = ObservableField<CreatorPlayableEpisode?>()
}

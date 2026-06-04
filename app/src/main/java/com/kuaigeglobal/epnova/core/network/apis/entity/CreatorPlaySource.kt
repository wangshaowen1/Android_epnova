package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 创作播放器的数据来源。
 *
 * 这里只描述创作体系 source。首页内容区不允许放进这个枚举，避免新旧播放器再次耦合。
 */
enum class CreatorPlaySource {
    /**TopEarners 推荐作品，唯一允许跨作品上下切换的 source。*/
    TOP_EARNERS,

    /**MyCreations 入口，只播放当前作品自己的剧集，不跨作品切换。*/
    MY_CREATIONS,

    /**作品详情剧集列表，只在当前 workId 下的剧集内切换。*/
    WORK_SERIES
}

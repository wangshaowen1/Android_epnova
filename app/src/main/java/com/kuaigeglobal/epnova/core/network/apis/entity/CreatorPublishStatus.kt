package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 创作体系发布状态。
 *
 * 这组常量只服务未发行创作、作品详情和发布审核链路；不要和内容消费播放器的锁集状态混用。
 */
object CreatorPublishStatus {
    /**未发布，作品或剧集仍在创作体系内。*/
    const val UNPUBLISHED = 0

    /**审核中，删除、编辑和发行入口需要按审核状态限制。*/
    const val REVIEWING = 1

    /**剧集级发布审核通过。*/
    const val PASSED = 2

    /**作品全局发布状态里的已发布，数值和 PASSED 一致，命名上区分作品语义。*/
    const val PUBLISHED = PASSED

    /**审核失败或驳回，需要展示驳回原因。*/
    const val FAILED = 3
}

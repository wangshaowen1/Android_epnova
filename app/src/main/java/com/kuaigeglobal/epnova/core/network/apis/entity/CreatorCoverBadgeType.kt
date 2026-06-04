package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 创作体系封面角标类型。
 *
 * 用于 MyCreations 和作品详情的封面/剧集角标展示，不参与播放器解锁或播放权限判断。
 */
object CreatorCoverBadgeType {
    /**无角标。*/
    const val NONE = 0

    /**草稿。*/
    const val DRAFT = 10

    /**未发布。*/
    const val UNPUBLISHED = 20

    /**审核中。*/
    const val IN_REVIEW = 30

    /**审核失败。*/
    const val REVIEW_FAILED = 40

    /**已发布。*/
    const val PUBLISHED = 50
}

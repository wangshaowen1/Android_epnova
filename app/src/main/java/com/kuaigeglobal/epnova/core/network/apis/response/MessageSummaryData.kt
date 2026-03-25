package com.kuaigeglobal.epnova.core.network.apis.response

/**
 * 消息概览
 */
data class MessageSummaryData(
    /**
     * 消息未读数量总数
     */
    val unreadNumTotal: Int? = 0,
    /**
     * 喜欢消息
     */
    val likesMessage: Message? = null,
    /**
     * 追剧(短剧剧集)消息
     */
    val watchingMessage: Message? = null,
    /**
     * 系统消息
     */
    val systemMessages: Message? = null
) {
    data class Message(
        /**
         * 消息未读数量
         */
        val unreadNum: Int? = 0,
        /**
         * 最新一条消息
         */
        val message: String? = null,
        /**
         * 创建时间-返回时间戳(没有消息时返回null)
         */
        val createTime: Long? = null
    )
}


package com.kuaigeglobal.epnova.core.network.apis.entity

/** 消息中心分页项。 */
data class MessagePageInfo(
    /** 用户消息记录 ID。 */
    val messageId: String? = null,
    /** 消息标题。 */
    val title: String? = null,
    /** 完整消息正文。 */
    val content: String? = null,
    /** 消息图片地址。 */
    val imageUrl: String? = null,
    /** 关联作品名称。 */
    val workTitle: String? = null,
    /** 消息发送时间，Unix 毫秒时间戳。 */
    val sendTime: Long? = null,
    /** 是否支持点击跳转：0=否，1=是。 */
    val canJump: Int? = null,
    /** 消息跳转类型：1、2、3。 */
    val jumpType: Int? = null,
    /** 消息内的实际跳转地址。 */
    val jumpUri: String? = null,
)

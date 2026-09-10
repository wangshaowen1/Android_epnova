package com.kuaigeglobal.epnova.core.network.apis.response

/** 查询问题反馈详情接口返回的数据。 */
data class FeedbackDetailData(
    /** 反馈雪花业务 ID，以字符串返回。 */
    val feedbackId: String? = null,
    /** 反馈问题类型，接口枚举值为 1、2、3。 */
    val problemType: Int? = null,
    /** 用户提交的完整反馈描述。 */
    val problemDesc: String? = null,
    /** 用户提交的反馈图片 URL，最多 4 张。 */
    val imagesUrl: List<String>? = null,
    /** 用户提交反馈时填写的联系邮箱。 */
    val contactEmail: String? = null,
    /**
     * 反馈提交时间。
     * 接口 Schema 声明为 string/date，字段说明约定内容为 Unix 毫秒时间戳。
     */
    val createTime: String? = null,
    /** 反馈处理状态，接口枚举值为 1、2。 */
    val feedbackStatus: Int? = null,
    /** 当前回复版本；0 表示尚未回复，首次回复为 1，修改后递增。 */
    val replyVersion: Int? = null,
    /** 当前客服回复；尚未回复时为 null。 */
    val replyContent: FeedbackReplyContentData? = null,
    /**
     * 当前回复提交或最近修改时间。
     * 接口 Schema 声明为 string/date，字段说明约定内容为 Unix 毫秒时间戳。
     */
    val replyTime: String? = null,
    /** 当前回复阅读状态：0=未读，1=已读。 */
    val replyReadStatus: Int? = null,
    /**
     * 当前回复首次确认已读的服务端时间；未读或未回复时为 null。
     * 接口 Schema 声明为 string/date，字段说明约定内容为 Unix 毫秒时间戳。
     */
    val replyReadTime: String? = null,
    /** 是否仍有角标提醒：0=否，1=是。查询详情不会修改该状态。 */
    val noticePending: Int? = null,
)

/** 反馈详情中的客服回复内容。 */
data class FeedbackReplyContentData(
    /** 客服回复正文。 */
    val content: String? = null,
    /** 客服回复图片 URL，最多 4 张。 */
    val imagesUrl: List<String>? = null,
)

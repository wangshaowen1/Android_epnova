package com.kuaigeglobal.epnova.core.network.apis.entity

/** 查询问题反馈记录分页接口返回的单条记录。 */
data class FeedbackPageInfo(
    /** 反馈雪花业务 ID，供详情查询使用。 */
    val feedbackId: String? = null,
    /** 反馈问题类型，接口枚举值为 1、2、3。 */
    val problemType: Int? = null,
    /** 反馈内容摘要，最多 120 个字符。 */
    val problemDescSummary: String? = null,
    /** 用户提交的反馈图片 URL，最多 4 张，按提交顺序返回。 */
    val imagesUrl: List<String>? = null,
    /**
     * 反馈提交时间。
     * 接口 Schema 声明为 string/date，字段说明约定内容为 Unix 毫秒时间戳。
     */
    val createTime: String? = null,
    /** 反馈处理状态，接口枚举值为 1、2。 */
    val feedbackStatus: Int? = null,
    /** 当前回复版本；0 表示尚未回复，首次回复为 1，修改后递增。 */
    val replyVersion: Int? = null,
    /** 当前回复阅读状态：0=未读，1=已读。 */
    val replyReadStatus: Int? = null,
    /** 是否仍有入口角标提醒：0=否，1=是。 */
    val noticePending: Int? = null,
)

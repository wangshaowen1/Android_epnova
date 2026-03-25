package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 举报
 * @author xiaoowuu
 * @param otherUserToken 被举报的用户id
 * @param tipoffScene 举报发生的场景 1-音乐播放界面
 * @param tipoffItemId 举报关联ID,音乐id
 * @param tipoffContent 举报内容 (举报聊天记录时必填)
 */
data class TipOffBody(
    //被举报人ID
    val otherUserToken: String?,
    //举报发生的场景 1-短剧 2-剧集
    val tipoffScene: Int?,
    //举报关联ID
    val tipoffItemId: String?,
    //举报类型 1-内容违规 2-侵权抄袭 3-色情低俗 4-违法犯罪 5-政治敏感 6-其他问题
    val tipOffType: Int?,
    //举报原因
    val tipoffReasonContent: String?=null,
    //举报内容 (举报聊天记录时必填)
    val tipoffContent: String?=null,
    //举报内容图片（多个用@符号拼接）
    val tipoffImg: String?,
    //联系方式
    val contact: String? = null,
) : UserTokenBody()
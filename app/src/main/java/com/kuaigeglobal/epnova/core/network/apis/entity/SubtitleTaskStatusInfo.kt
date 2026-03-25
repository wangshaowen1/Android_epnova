package com.kuaigeglobal.epnova.core.network.apis.entity

import com.blankj.utilcode.util.GsonUtils
import com.google.gson.reflect.TypeToken

/**
 * {
 *     "id": "",
 *     "subtitleTaskId": "字幕任务ID（雪花算法）",
 *     "userId": "",
 *     "workId": "",
 *     "storyId": "",
 *     "storyboardId": "",
 *     "channelId": "字幕服务渠道 1=火山引擎",
 *     "videoUrl": "",
 *     "externalTaskId": "第三方任务ID（火山 submit 响应）",
 *     "genStatus": "生成状态 0=待生成 1=生成中 2=成功 3=失败",
 *     "subtitleContent": "字幕内容（utterances JSON）",
 *     "errorCode": "",
 *     "errorMsg": "",
 *     "submitRequest": "submit 请求参数（JSON）",
 *     "submitResponse": "submit 响应参数（JSON）",
 *     "timeCostMs": "任务耗时（毫秒）",
 *     "completeTime": "",
 *     "createTime": "创建时间",
 *     "modifyTime": "修改时间",
 *     "recordStatus": "软删除标识.有效状态(0-无效，1-有效)(可选：如果你配置了MyBatisPlus的逻辑删除，建议加上 @TableLogic)",
 *     "deleteTime": "删除时间",
 *     "createBy": "创建人（通常从当前登录用户上下文中获取）",
 *     "updateBy": "最后更新人"
 * }
 */
data class SubtitleTaskStatusInfo(
    val workId: String?,
    val storyId: String?,
    val storyboardId: String?,
    val videoUrl: String?,
    /**
     * 生成状态 0=待生成 1=生成中 2=成功 3=失败
     */
    var genStatus: Int?,
    val subtitleContent: String?,
) {
    var subtitle: List<SubtitleItem>? = null
        get() {
            if (field.isNullOrEmpty()) {
                runCatching {
                    val type = object : TypeToken<List<SubtitleItem>>() {}.type
                    val subtitleList: List<SubtitleItem> = GsonUtils.fromJson(subtitleContent, type)
                    field = subtitleList
                }
            }
            return field
        }
    val generating get() = genStatus == 1
    val generated get() = genStatus == 2
    val generateFailed get() = genStatus == 3
}
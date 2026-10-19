package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 修改灵感大纲阶段，保存后不触发生成。
 * @param workId 作品 ID
 * @param settingId stages 返回的阶段设定 ID
 * @param name 阶段名称，非空白
 * @param content 阶段内容，非空白
 */
data class UpdateOutlineStageBody(
    val workId: Long,
    val settingId: Long,
    val name: String,
    val content: String
)

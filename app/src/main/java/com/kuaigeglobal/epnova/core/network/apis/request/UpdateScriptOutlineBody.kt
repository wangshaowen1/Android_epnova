package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 600 灵感作品-修改本集剧情概览。
 * @param workId 作品 ID
 * @param storyId 目标剧集 ID
 * @param storySummary 非空白的剧情概览/剧本大纲，区别于剧本正文
 */
data class UpdateScriptOutlineBody(
    val workId: Long,
    val storyId: Long,
    val storySummary: String
)

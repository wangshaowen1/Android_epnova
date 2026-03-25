package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:小说-保存续创剧集（只有小说有）
 * @Data: 2026/2/6/周五  16:56
 * @Version: 1.0
 * *****************************************
 */
data class SaveSequelNovelStoryBody(
    //小说续创剧集内容
    val storySummary: String? = null,
    //剧集 id
    val storyId: String? = null,
    //作品 ID
    val workId: String? = null,
)
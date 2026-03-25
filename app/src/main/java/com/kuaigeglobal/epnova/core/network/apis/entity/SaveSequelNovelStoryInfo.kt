package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:小说-保存续创剧集（只有小说有）
 * @Data: 2026/2/6/周五  16:58
 * @Version: 1.0
 * *****************************************
 */
data class SaveSequelNovelStoryInfo(
    //剧集 id
    val storyId: String? = null,
    //作品 ID
    val workId: String? = null,
)
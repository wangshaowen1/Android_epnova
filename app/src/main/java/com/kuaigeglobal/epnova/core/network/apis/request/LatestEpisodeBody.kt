package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:续创时查询上一集内容详情（获取最近一集的内容）
 * @Data: 2026/2/6/周五  10:44
 * @Version: 1.0
 * *****************************************
 */
data class LatestEpisodeBody(
    //作品类型：1-灵感创作 2-小说改编 3小说上传
    val workType: Int? = null,
    //剧集 id（要续创的剧集id）
    val storyId: String? = null,
    //作品 ID
    val workId: String? = null
) : UserTokenBody() {
    companion object {
        //1-灵感创作
        const val WORK_TYPE_INSPIRATION = 1

        //2-小说改编
        const val WORK_TYPE_NOVEL = 2

        //3-小说上传
        const val WORK_TYPE_NOVEL_DOCUMENT = 3
    }
}
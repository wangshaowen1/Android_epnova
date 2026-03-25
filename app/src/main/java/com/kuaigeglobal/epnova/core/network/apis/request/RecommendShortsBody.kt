package com.kuaigeglobal.epnova.core.network.apis.request


class RecommendShortsBody(
    val dramasIds: List<String>?,
    pageNum: Int,
    pageSize: Int
) : PagedBody(pageNum, pageSize){
    companion object {
        var lastRecommendBrowserList: List<String>? = null
    }
}
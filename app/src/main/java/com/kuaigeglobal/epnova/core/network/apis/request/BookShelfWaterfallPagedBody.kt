package com.kuaigeglobal.epnova.core.network.apis.request

/** 首页 v2 瀑布流分页请求；区块编码使用首页配置下发的数值类型。 */
class BookShelfWaterfallPagedBody(
    val bookshelfSectionCode: Int,
    pageNum: Int,
    pageSize: Int,
) : PagedBody(pageNum, pageSize)

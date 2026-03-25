package com.kuaigeglobal.epnova.core.network.apis.request

import kotlin.Int

class DanmakuBody(
    val usePosition: Int? = 2,//使用位置，1-新用户付费解锁弹窗，2-爆款Tab
    pageNum: Int,
    pageSize: Int,
) : PagedBody(pageNum, pageSize)
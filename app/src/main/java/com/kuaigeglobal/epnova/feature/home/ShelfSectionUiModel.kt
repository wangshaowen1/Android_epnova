package com.kuaigeglobal.epnova.feature.home

import com.kuaigeglobal.epnova.core.network.apis.entity.BookShelfSectionInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo

/** 区块配置与独立请求得到的横滑预览作品。 */
data class ShelfSectionUiModel(
    val config: BookShelfSectionInfo,
    val works: List<WorkVOInfo>
)

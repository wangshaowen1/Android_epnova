package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** 首页一级 Tab 及其可见书架区块配置。 */
data class BookShelfConfigInfo(
    val bookshelfTab: Int? = null,
    val bookshelfTabName: String? = null,
    val bookshelfSectionList: List<BookShelfSectionInfo>? = null
)

/** 书架区块配置；接口已按 sortNo 倒序返回，区块作品需另行加载。 */
@Parcelize
data class BookShelfSectionInfo(
    val bookshelfName: String? = null,
    val bookshelfSectionCode: Int? = null,
    val bookshelfType: Int? = null,
    val sortNo: Int? = null
) : Parcelable

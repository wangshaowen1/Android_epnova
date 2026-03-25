package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 *{
 *     "title": "Banner 标题",
 *     "imageUrl": "图片地址(png/jpg/gif)",
 *     "jumpType": "跳转类型(1-app内页,2-app内部H5,3-外部H5)",
 *     "jumpUri": "跳转uri(内页uri或H5链接)-20260227更新字段名",
 *     "sortNo": "排序号(越小越靠前)"
 * }
 */
data class BannerInfo(
    val title: String? = null,
    val imageUrl: String? = null,
    val jumpType: Int? = null,
    val jumpUri: String? = null,
    val sortNo: Int? = null,
)
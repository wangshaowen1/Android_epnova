package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 剧集详情信息
 */
data class SeriesPriceConfigInfo(
    val defaultPriceList: List<Long>?,//默认价格集合,单位：里
    val curPrice: Long?, // 自定义价格,单位：里  0-未设置
    val freeEndWorkNo: Int?, // 从第几集开始收费
    val freeStatus: Int?, // 免费状态 0-收费 1-免费
    val priceTips: String?, // 价格设置说明文案
    val profitTips: String?, // 普通用户可获收益说明文案
    val vipProfitTips: String?, // 会员用户可获收益说明文案
    val recommendIconUrl: String?, // 推荐图标
)
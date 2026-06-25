package com.kuaigeglobal.epnova.core.network.apis.response


data class ShelfStatusData(
    val shelfStatus: Int?,//上架状态 0-下架 1-上架 2-审核中 null-记录不存在
    /**
     * 短剧分类弹窗是否开启(0-关闭,1-开启) 20260423新增
     */
    val dramaCategorySwitch: Int?,
)
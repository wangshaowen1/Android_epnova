package com.kuaigeglobal.epnova.core.network.apis.response


data class ShelfStatusData(
    val shelfStatus: Int?//上架状态 0-下架 1-上架 2-审核中 null-记录不存在
)
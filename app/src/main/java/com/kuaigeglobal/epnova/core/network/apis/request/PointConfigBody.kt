package com.kuaigeglobal.epnova.core.network.apis.request

data class PointConfigBody(
    /**
     * 场景类型：0-普通充值，1-首充
     */
    val sceneType: Int? = 0
) : UserTokenBody()
package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:下单积分
 * @Data: 2025/9/16/周二  9:09
 * @Version: 1.0
 * *****************************************
 */
data class OrderingPointsBody(
    /**
     * 积分配置id，接受Long和String类型
     */
    val configId: String?,
    /**
     * 场景类型：0-支付， 1-带剧本参数支付
     */
    val sceneType: Int? = 0,
    /**
     * 带参数 暂时是剧本iD
     */
    val itemId: String? = null
) : UserTokenBody()
package com.kuaigeglobal.epnova.core.network.apis.response

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:积分充值
 * @Data: 2025/9/11/周四  14:04
 * @Version: 1.0
 * *****************************************
 */
data class ScoreConfigData(
    val items: List<PointsData>?,
    /**
     * 充值协议地址
     */
    val protocolUrl: String?,
) {
    data class PointsData(
        /**
         * 配置ID
         */
        val configId: String?,
        /**
         * 获得积分数量
         */
        val pointNum: String?,
        /**
         * 积分金额
         */
        val pointAmount: Long?,
        /**
         * 默认选中状态 0-不选中 1-选中
         */
        val defaultSelectStatus: Int?
    )
}
package com.kuaigeglobal.epnova.core.network.apis.response

import com.kuaigeglobal.epnova.core.network.apis.ListData
import com.kuaigeglobal.epnova.core.network.apis.entity.PointConfigInfo

/**
 * {
 *     "pointBalanceNum": "可用算力数量",
 *     "items": [
 *         {
 *             "pointConfigId": "算力信息配置ID",
 *             "pointName": "算力名称",
 *             "pointPrice": "实际支付价格（单位：元）",
 *             "pointNum": "算力数量"
 *         }
 *     ]
 * }
 */
data class PointConfigData(
    /**
     * 可用算力数量
     */
    val pointBalanceNum: Int?,
): ListData<PointConfigInfo>()
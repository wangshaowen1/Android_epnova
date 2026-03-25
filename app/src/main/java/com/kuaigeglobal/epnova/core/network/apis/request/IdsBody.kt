package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "ids": [
 *         "泛型Long"
 *     ],
 *     "id": "id",
 *     "operateType": "操作类型（4-合成后的重绘删除）合成完成后重绘删除时传4"
 * }
 */
data class IdsBody(
    val ids: List<String>?,
    val id: String?,
    val operateType: Int? = null
)
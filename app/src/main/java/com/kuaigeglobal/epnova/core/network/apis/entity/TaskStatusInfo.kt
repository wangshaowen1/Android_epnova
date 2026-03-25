package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * {
 *     "code": "",
 *     "msg": "",
 *     "data": {
 *         "bizType": "业务类型 40=生成所有资产描述  70=生成所有分镜描述",
 *         "taskId": "任务id",
 *         "genStatus": "[枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败]",
 *         "result": "查询单个是直接返回结果,比如资产、分镜图片url等"
 *     }
 * }
 */
data class TaskStatusInfo(
    /**
     * 业务类型 40=生成所有资产描述  70=生成所有分镜描述
     */
    val bizType: Int?,
    /**
     * 任务id
     */
    val taskId: String?,
    /**
     * [枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败]
     */
    val genStatus: Int?,
    /**
     * 查询单个是直接返回结果,比如资产、分镜图片url等
     */
    val result: String? = null,
)
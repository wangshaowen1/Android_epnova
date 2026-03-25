package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * @param eventCode 事件编码
 * @param bizId 业务id
 * @param pageLinking 页面链路
 * @param customData 任何额外的自定义数据，以JSON格式存储，可根据业务需求添加任意字段
 */
data class EventTrackingBody(
    val eventCode: String,
    val bizId: String?,
    val pageLinking: String?,
    val customData: String?
) : UserTokenBody()
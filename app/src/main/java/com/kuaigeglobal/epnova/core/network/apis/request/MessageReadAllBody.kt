package com.kuaigeglobal.epnova.core.network.apis.request

/** 按消息分页返回的快照标记全部已读。 */
data class MessageReadAllBody(
    val readSnapshotToken: String
)

package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 出海（海外）专用的设备指纹向量数据模型
 */
data class OverseasDeviceFingerprint(
    val drmId: String?,      // 硬件级：防重装、防刷机 (核心锚点)
    val androidId: String?,  // 系统级：同签名防重装 (辅助锚点)
    val appSetId: String?,   // 生态级：Google 官方合规 ID，同开发者账号下共享不变 (出海主力)
    val cloudUuid: String?   // 应用级：依赖 Google Drive 的 Auto Backup 机制 (兜底锚点)
)
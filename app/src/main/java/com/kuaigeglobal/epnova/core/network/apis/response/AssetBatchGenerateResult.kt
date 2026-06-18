package com.kuaigeglobal.epnova.core.network.apis.response

import com.kuaigeglobal.epnova.core.network.apis.entity.AssetGenerateInfo

/**
 * 一键生成资产图片结果
 */
data class AssetBatchGenerateResult(
    // 本次提交生成的资产数量
    val submittedCount: Int? = null,
    // 因已有成功图或正在生成而跳过的资产数量
    val skippedCount: Int? = null,
    // 本次提交生成的资产任务列表
    val items: List<AssetGenerateInfo>? = null
)

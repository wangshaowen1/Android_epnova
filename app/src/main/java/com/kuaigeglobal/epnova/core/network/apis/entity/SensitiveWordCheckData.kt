package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * {
 *     "code": "校验结果码 0/1=无敏感词 2=有敏感词",
 *     "content": "校验结果描述",
 *     "hitWords": [
 *         "命中的敏感词数组"
 *     ]
 * }
 */
data class SensitiveWordCheckData(
    /**
     * 校验结果码 0/1=无敏感词 2=有敏感词
     */
    val code: Int?,
    /**
     * 校验结果描述
     */
    val content: String?,
    /**
     * 命中的敏感词数组
     */
    val hitWords: List<String>?
) {
    fun isSensitive(): Boolean {
        return code == 2
    }
}
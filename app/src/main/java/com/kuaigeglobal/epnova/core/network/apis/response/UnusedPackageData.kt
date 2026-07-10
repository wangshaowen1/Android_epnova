package com.kuaigeglobal.epnova.core.network.apis.response

import com.kuaigeglobal.epnova.core.network.apis.entity.UnusedPackageInfo

/** 已购买且尚未使用的短剧创作套餐列表。 */
data class UnusedPackageData(
    val hasUnusedPackage: Boolean? = null,
    val displaySubtitle: String? = null,
    val packages: List<UnusedPackageInfo>? = null
) {
    fun validPackages(): List<UnusedPackageInfo> {
        if (hasUnusedPackage != true) return emptyList()
        return packages.orEmpty().filter { it.hasValidInstance() }
    }
}

package com.kuaigeglobal.epnova.core.network.apis.response

/**
 * {
 *     "upgradeDesc": "升级描叙",
 *     "upgradeUrl": "升级地址",
 *     "versionStatus": "版本状态：0-不升级 1-强制升级 2-建议升级",
 *     "versionDesc": "版本描叙",
 *     "md5Hash": "apk 校验码"
 * }
 */
data class UpgradeData(
    /**
     * 升级描叙
     */
    val appVersionDesc: String?,
    /**
     * 升级地址
     */
    val upgradeUrl: String?,
    /**
     * 版本描叙
     */
    val appVersion: String?,
    /**
     * 版本状态：0-不升级 1-强制升级 2-建议升级
     */
    val versionStatus: Int?,
    /**
     * apk 校验码
     */
    val md5Hash:String?
){
    fun isNeedUpdate(): Boolean {
        return versionStatus == 1 || versionStatus == 2
    }

    fun isNeedForceUpdate(): Boolean {
        return versionStatus == 1
    }
}
package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 创作体系发布平台信息。
 *
 * TopEarners 当前只展示平台 logo；后续如果平台跳转或发行信息扩展，也继续放在这个 Creator 模型里。
 */
@Parcelize
data class CreatorPlatformInfo(
    /**平台 code，用于后续识别平台类型。*/
    val platformCode: String? = null,

    /**平台展示名称。*/
    val platformName: String? = null,

    /**平台 logo URL，TopEarners 列表当前使用这个字段展示平台图标。*/
    val platformLogoUrl: String? = null,

    /**作品在该平台的 H5 地址；为空表示不可跳转。*/
    val h5Url: String? = null
) : Parcelable

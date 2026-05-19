package com.kuaigeglobal.epnova.core.network.apis.request


data class AssetSaveOverrideBody(
    val bizType: Int? = null,
    val newAppearanceId: String? = null,
    val workId: String? = null
)


data class AppearanceIdBody(
    val appearanceId: String? = null,
)
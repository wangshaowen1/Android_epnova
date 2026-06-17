package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable

interface SimpleAsset : Parcelable {
    val assetId: String?
    val assetImageId: String?
    val assetAppearanceId: String?
    val status: Int?
    val assetDesc: String?
    val assetName: String?
    val assetImage: String?
    val showReference: Boolean
    val assetTaskId: String?

    fun setAssetImage(imageUrl: String?)
}

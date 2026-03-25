package com.kuaigeglobal.epnova.core.network.apis.entity

interface SimpleAsset {
    val status: Int?
    val assetDesc: String?
    val assetName: String?
    val assetImage: String?
    val showReference: Boolean
}
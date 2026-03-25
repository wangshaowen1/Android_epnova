package com.kuaigeglobal.epnova.feature.download.material

/**
 * 视频用到的所有素材管理（下载），包括图片、配音、水印等
 */
class MicroAnimSourceMaterial(
    val segments: List<MicroAnimSegmentSourceMaterialItem>?,
    val watermark: SourceMaterialItem?,
    runningTaskSize: Int = 8
) : Material(runningTaskSize) {
    override val items: List<SourceMaterialItem> by lazy {
        val items = arrayListOf<SourceMaterialItem>()
        segments?.let { items.addAll(it) }
        watermark?.let { items.add(it) }
        items
    }
}
package com.kuaigeglobal.epnova.feature.download.material

/**
 * 视频用到的所有素材管理（下载），包括封面、背景、原视频、音乐等
 */
class SourceMaterial(
    val images: List<SourceMaterialItem>?,
    val audios: List<SourceMaterialItem>?,
    val overlay: SourceMaterialItem?,
    runningTaskSize: Int = 5
) : Material(runningTaskSize) {
    override val items: List<SourceMaterialItem> by lazy {
        val items = arrayListOf<SourceMaterialItem>()
        images?.let { items.addAll(it) }
        audios?.let { items.addAll(it) }
        overlay?.let { items.add(it) }
        items
    }
}
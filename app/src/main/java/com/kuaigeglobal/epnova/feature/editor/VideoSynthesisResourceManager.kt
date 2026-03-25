package com.kuaigeglobal.epnova.feature.editor

import com.kuaige.videoedit.resource.Resource
import com.kuaige.videoedit.resource.ResourceManager

class VideoSynthesisResourceManager(
    val storyboards: List<Resource>?,
) : ResourceManager() {
    override val items: ArrayList<Resource> by lazy {
        val items = arrayListOf<Resource>()
        storyboards?.let { items.addAll(it) }
        items
    }
}
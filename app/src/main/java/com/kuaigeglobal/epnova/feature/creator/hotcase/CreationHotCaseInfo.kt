package com.kuaigeglobal.epnova.feature.creator.hotcase

import com.kuaigeglobal.epnova.core.network.apis.entity.SameStyleHotCaseInfo

data class CreationHotCaseInfo(
    val id: String?,
    val title: String?,
    val coverUrl: String?,
    val tags: List<String>?,
    val recommendText: String?
) {
    val tagsText: String?
        get() = tags?.joinToString("·")

    companion object {
        fun from(info: SameStyleHotCaseInfo): CreationHotCaseInfo {
            return CreationHotCaseInfo(
                id = info.hotCaseId,
                title = info.workTitle,
                coverUrl = info.coverUrl,
                tags = info.tags.orEmpty(),
                recommendText = info.recommendText
            )
        }
    }
}

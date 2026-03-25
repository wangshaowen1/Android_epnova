package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TemplateQuestion(
    val talkAnswer: String?,
    val talkQuestion: String?
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TemplateQuestion

        if (talkAnswer != other.talkAnswer) return false
        if (talkQuestion != other.talkQuestion) return false

        return true
    }

    override fun hashCode(): Int {
        var result = talkAnswer?.hashCode() ?: 0
        result = 31 * result + (talkQuestion?.hashCode() ?: 0)
        return result
    }
}


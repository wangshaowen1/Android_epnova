package com.kuaigeglobal.epnova.core.network.apis.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MusicPre(
    val playUrl: String?,
    val title: String?
) : Parcelable {
    fun isAvailable(): Boolean {
        return !playUrl.isNullOrEmpty()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MusicPre

        if (playUrl != other.playUrl) return false
        if (title != other.title) return false

        return true
    }

    override fun hashCode(): Int {
        var result = playUrl?.hashCode() ?: 0
        result = 31 * result + (title?.hashCode() ?: 0)
        return result
    }


}
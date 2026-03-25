package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UnlockInfo(
    val unlockPrice: Int?,
    /**
    是否解锁 0=未解锁 1=已解锁
     *
     */
    var unlockStatus: Int?,
) : Parcelable {
    fun isLocked(): Boolean {
        return unlockStatus == 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UnlockInfo

        if (unlockPrice != other.unlockPrice) return false
        if (unlockStatus != other.unlockStatus) return false

        return true
    }

    override fun hashCode(): Int {
        var result = unlockPrice ?: 0
        result = 31 * result + (unlockStatus ?: 0)
        return result
    }


}
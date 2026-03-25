package com.kuaigeglobal.epnova.core.network.apis.entity

import com.kuaigeglobal.epnova.common.ext.formatDecimal
import com.kuaigeglobal.epnova.common.ext.formatFloat
import java.io.Serializable

/**
 * 提现确认弹窗
 */
class WithdrawConfirmInfo : Serializable {
    /**
     * 实际到账
     */
    val actualReceipt: Float? = null

    /**
     * 提现规则说明
     */
    val withdrawalRule: String? = null

    /**
     * 抵扣乐币
     */
    val exchange: Long? = null

    /**
     * 提现比例（0-100）
     */
    val withdrawalRatio: Float? = null

    /**
     * 备注
     */
    val remark: String? = null

    /**
     * 当前用户音乐人等级是否为最高音乐人等级 0-不是 1-是
     */
    val isMaxMusicianLevel: Int? = null

    companion object {
        private const val serialVersionUID = -109L
    }


    val actualReceiptFormatted get() = actualReceipt?.formatDecimal()

    val withdrawalRatioFormatted get() = withdrawalRatio?.let { formatFloat(it, 2) }

}
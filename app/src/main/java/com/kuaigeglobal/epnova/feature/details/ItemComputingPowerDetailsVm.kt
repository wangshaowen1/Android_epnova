package com.kuaigeglobal.epnova.feature.details

import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.response.ComputingPowerDetailsInfo


/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce: 充值明细
 * @Data: 2025/4/17  14:40
 * @Version: 1.0
 * *****************************************
 */
class ItemComputingPowerDetailsVm(
    data: ComputingPowerDetailsInfo?
) : AppItemViewModel<ComputingPowerDetailsInfo>(data) {

    fun quantity(): String {
        val item = data.get() ?: return ""
        return if (item.transType == ComputingPowerDetailsInfo.SCORE_TYPE_GET) {
            "+"
        } else {
            "-"
        } + (item.tradeNum ?: "0")
    }
}
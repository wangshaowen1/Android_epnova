package com.kuaigeglobal.epnova.feature.details

import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.response.ComputingPowerDetailsInfo
import com.kuaigeglobal.epnova.feature.details.child.ComputingPowerDetailsTabFragment


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

    //资产类型（Coins/Credits）
    var assetType: Int = ComputingPowerDetailsTabFragment.ASSET_TYPE_COINS

    fun title(): String {
        return data.get()?.tradeTitle.orEmpty()
    }

    fun quantity(): String {
        val item = data.get() ?: return ""
        return if (item.transType == ComputingPowerDetailsInfo.SCORE_TYPE_GET) {
            "+"
        } else {
            "-"
        } + (item.tradeNum ?: "0")
    }

    fun iconRes(): Int {
        return if (assetType == ComputingPowerDetailsTabFragment.ASSET_TYPE_CREDITS) {
            R.drawable.icon_power
        } else {
            R.drawable.ic_coins
        }
    }

}

package com.kuaigeglobal.epnova.feature.details

import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.response.ComputingPowerDetailsInfo
import com.kuaigeglobal.epnova.feature.details.child.ComputingPowerDetailsTabFragment
import com.nhsd.basic.ext.getResString
import java.util.Locale


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
        val item = data.get() ?: return ""
        val fallbackTitle = item.tradeTitle.orEmpty()
        val titleRes = if (assetType == ComputingPowerDetailsTabFragment.ASSET_TYPE_CREDITS) {
            when (item.tradeSubType) {
                SUB_TYPE_CREDITS_PURCHASE -> R.string.transaction_credits_purchase
                SUB_TYPE_IMAGE_GENERATION -> R.string.transaction_image_generation
                SUB_TYPE_VIDEO_GENERATION -> R.string.transaction_video_generation
                SUB_TYPE_TEXT_PARSING -> R.string.transaction_text_parsing
                else -> null
            }
        } else {
            when (item.tradeSubType) {
                SUB_TYPE_COINS_EPISODE_UNLOCK -> R.string.transaction_history_episode_unlock
                SUB_TYPE_COINS_PURCHASE -> R.string.transaction_history_coins_purchase
                SUB_TYPE_COINS_EXPIRED -> R.string.transaction_history_coins_expired
                SUB_TYPE_COINS_REWARD -> rewardTitleRes(fallbackTitle)
                else -> null
            }
        }
        return titleRes?.getResString() ?: fallbackTitle
    }

    private fun rewardTitleRes(title: String): Int? {
        val lowerTitle = title.lowercase(Locale.ROOT)
        return when {
            lowerTitle.contains("login") || title.contains("登录") -> R.string.transaction_history_login_reward
            lowerTitle.contains("ad") || title.contains("广告") -> R.string.transaction_history_ad_reward
            lowerTitle.contains("check") || lowerTitle.contains("sign") || title.contains("签到") -> R.string.transaction_history_checkin_reward
            else -> null
        }
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

    companion object {
        private const val SUB_TYPE_CREDITS_PURCHASE = 1001
        private const val SUB_TYPE_IMAGE_GENERATION = 2001
        private const val SUB_TYPE_VIDEO_GENERATION = 2002
        private const val SUB_TYPE_TEXT_PARSING = 2004

        private const val SUB_TYPE_COINS_EPISODE_UNLOCK = 2
        private const val SUB_TYPE_COINS_PURCHASE = 1001
        private const val SUB_TYPE_COINS_REWARD = 1002
        private const val SUB_TYPE_COINS_EXPIRED = 2003
    }
}

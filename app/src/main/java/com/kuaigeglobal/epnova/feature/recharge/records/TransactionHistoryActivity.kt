package com.kuaigeglobal.epnova.feature.recharge.records

import android.app.Activity
import android.content.Intent
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.databinding.ActivityTransactionHistoryBinding
import com.kuaigeglobal.epnova.feature.details.child.ComputingPowerDetailsTabFragment
import com.kuaigeglobal.epnova.feature.recharge.MyBalanceViewModel
import com.nhsd.basic.BaseViewModelActivity

/** 使用记录
 * @createtime 2026年08月19日 10:10
 * @author TooWhiteT
 */

class TransactionHistoryActivity : BaseViewModelActivity<ActivityTransactionHistoryBinding, TransactionHistoryViewModel>() {

    companion object {

        fun start(activity: Activity?, currentTab: Int = MyBalanceViewModel.TAB_COINS) {
            val activity = activity ?: topActivity
            val intent = Intent(activity, TransactionHistoryActivity::class.java)
            intent.putExtra(Constants.Extra.TAB, currentTab)
            activity?.startActivity(intent)
        }

    }

    private val currentTab by lazy {
        intent.getIntExtra(Constants.Extra.TAB, MyBalanceViewModel.TAB_COINS)
    }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        // transaction_history_show：交易历史记录明细页展示时上报。
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.STORE,
            FirebaseEvent.Action.TRANSACTION_HISTORY_SHOW
        )
        loadTransactionHistoryFragment()
    }

    private fun loadTransactionHistoryFragment() {
        if (supportFragmentManager.findFragmentById(binding.contentLayout.id) != null) return

        val assetType = if (currentTab == MyBalanceViewModel.TAB_CREDITS) {
            ComputingPowerDetailsTabFragment.ASSET_TYPE_CREDITS
        } else {
            ComputingPowerDetailsTabFragment.ASSET_TYPE_COINS
        }
        supportFragmentManager.beginTransaction()
            .replace(
                binding.contentLayout.id,
                ComputingPowerDetailsTabFragment.newInstance(assetType)
            )
            .commit()
    }
}

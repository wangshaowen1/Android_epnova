package com.kuaigeglobal.epnova.feature.recharge

import android.os.Bundle
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.google.billing.BillingManager
import com.kuaigeglobal.epnova.core.manager.AppEvent
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.databinding.ActivityMyBalanceBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.startActivity

class MyBalanceActivity : BaseViewModelActivity<ActivityMyBalanceBinding, MyBalanceViewModel>() {
    companion object {
        fun start() {
            topActivity?.startActivity(MyBalanceActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventManager.onAppTrack(AppEvent.SF_VIEW_RECHARGE_PAGE)
        BillingManager.markActivePage(true)
        observeEvents {
            on<Events.UserDataEvent> {
                mRealVM.updateBalance()
            }
        }
    }

    override fun createViewModel(): MyBalanceViewModel
    {
        return MyBalanceViewModel(R.layout.item_recharge_payment)
    }

    override fun onDestroy() {
        BillingManager.markActivePage(false)
        super.onDestroy()
    }
}
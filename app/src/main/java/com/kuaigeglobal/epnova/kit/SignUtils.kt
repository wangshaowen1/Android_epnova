package com.kuaigeglobal.epnova.kit

import com.blankj.utilcode.util.ActivityUtils
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.common.Cache
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.feature.aicompose.generating.GeneratingViewModel
import com.kuaigeglobal.epnova.feature.login.LoginActivity
import com.kuaigeglobal.epnova.feature.login.flash.PhoneLoginDialogActivity
import com.nhsd.basic.bus.FlowEventBus

object SignUtils {

    @JvmStatic
    fun signOut() {
        GeneratingViewModel.generating = false
        App.instance.deleteAlias()
        User.get().loginOut()
        Cache.get().clear()
        FlowEventBus.postSync(Events.UserChangedEvent(Events.UserChangedEvent.SIGN_OUT))
    }

    @JvmStatic
    fun signIn() {
        GeneratingViewModel.generating = true
        User.Companion.get().userId?.let { App.instance.setAlias(it) }
        ActivityUtils.finishActivity(LoginActivity::class.java)
        ActivityUtils.finishActivity(PhoneLoginDialogActivity::class.java)
        FlowEventBus.postSync(Events.UserChangedEvent(Events.UserChangedEvent.SIGN_IN))
    }
}
package com.kuaigeglobal.epnova.feature.home

import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.response.LoginTaskData
import com.kuaigeglobal.epnova.core.response

/** Home 容器自身只负责顶部入口和跨 Tab 的登录奖励状态。 */
class HomeTabsViewModel : AppRepositoryViewModel<AppRepository>() {

    val unCheckNum = ObservableInt()
    val loginTaskData = MutableLiveData<LoginTaskData>()

    override fun onCreate() {
        super.onCreate()
        getRewardUserUncheck()
    }

    fun getRewardUserUncheck() {
        if (User.get().isVisitor) {
            unCheckNum.set(0)
            return
        }
        launch(
            block = {
                response(
                    bean = mRepo.api.rewardUserUncheck(UserTokenBody()),
                    result = { unCheckNum.set(data?.unCheckNum ?: 0) },
                    error = { unCheckNum.set(0) }
                )
            },
            onError = { unCheckNum.set(0) }
        )
    }

    fun getLoginTaskData() {
        launch {
            response(mRepo.api.loginTaskPopup()) {
                if (data == null || data?.rewardLabel == "0") return@response
                if (data?.needShow == 1) {
                    loginTaskData.value = data
                    Config.get().loginRewards = data?.rewardLabel
                }
            }
        }
    }
}

package com.kuaigeglobal.epnova.feature.mine

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.response.SimpleBannerInfo
import com.kuaigeglobal.epnova.core.response
import com.nhsd.basic.ext.getResString

/**
 * @Introduce:我的
 */
class MineViewModel : AppRepositoryViewModel<AppRepository>() {

    //是否登录
    val isLogin by lazy { ObservableBoolean(false) }

    //用户资料Map
    val userInfo by lazy { ObservableField<Map<String, String?>>() }

    /**
     * 账户余额
     */
    val balance by lazy { ObservableField<String>() }
    val workCourseBanner by lazy { ObservableField<SimpleBannerInfo>() }

    val loginRewardsValid = ObservableBoolean(User.get().isGuest)
    val loginRewards: String?
        get() = Config.get().loginRewards

    override fun onCreate() {
        super.onCreate()
        //更新必要信息
        updateInformation()
    }

    /**
     * 刷新用户信息
     */
    fun refreshUserInfo() {
        User.get().let { user ->
            loginRewardsValid.set(user.isGuest)
            isLogin.set(user.isLogin)
            if (!isLogin.get()) {
                notLoggedIn()
                return
            }
            logOn()
        }
    }


    /**
     * 更新必要信息
     */
    fun updateInformation() {
        //获取用户主页信息接口
        getUserHomeInfo()
    }

    /**
     * 获取用户主页信息接口
     */
    private fun getUserHomeInfo() {
        User.get().let { user ->
            refreshUserInfo()
            Global.refreshBalance()
            launch {
                response(
                    bean = mRepo.api.getUserHomeInfo(UserTokenBody()),
                    result = {
                        if (data == null) return@response
                        User.get().updateFrom(data)
                        refreshUserInfo()
                    })
            }
        }
    }


    private fun notLoggedIn() {
        val user = User.get()
        userInfo.set(
            mapOf(
                "id" to (user.userUid ?: ""),
                "nickname" to R.string.profile_log_in.getResString(),
                "avatar" to (user.avatarUrl ?: ""),
            )
        )
        balance.set(user.coins.toString())
        workCourseBanner.set(if (user.isPurchasedCourse()) null else Config.get().appConfig.workCourseBanner)
    }

    private fun logOn() {
        val user = User.get()
        userInfo.set(
            mapOf(
                "id" to user.userUid,
                "nickname" to user.nickName,
                "avatar" to user.avatarUrl,
            )
        )
        balance.set(user.coins.toString())
        workCourseBanner.set(if (user.isPurchasedCourse()) null else Config.get().appConfig.workCourseBanner)
    }
}
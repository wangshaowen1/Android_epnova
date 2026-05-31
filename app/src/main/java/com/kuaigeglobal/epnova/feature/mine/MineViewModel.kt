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
import com.kuaigeglobal.epnova.feature.settings.lang.languagesConfigs
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

    //福利中心-未领取奖励总额
    val unCheckNum by lazy { ObservableField("+0") }

    val workCourseBanner by lazy { ObservableField<SimpleBannerInfo>() }

    val language by lazy { languagesConfigs.find { it.code == Config.get().getSavedLanguage() } }

    val loginRewardsValid = ObservableBoolean(User.get().isGuest)
    val loginRewards = ObservableField<String>(Config.get().loginRewards)

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
            loginRewards.set(Config.get().loginRewards)
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
        if (User.get().isVisitor) {
            return
        }
        User.get().let { user ->
            refreshUserInfo()
            //获取用户福利未领取奖励总额
            getRewardUserUncheck()
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

    /**
     * 获取用户福利未领取奖励总额
     */
    private fun getRewardUserUncheck() {
        if (User.get().isVisitor) {
            unCheckNum.set("+0")
            return
        }
        launch(block = {
            response(
                bean = mRepo.api.rewardUserUncheck(UserTokenBody()),
                result = {
                    unCheckNum.set("+${data?.unCheckNum ?: 0}")
                },
                error = {
                    unCheckNum.set("+0")
                }
            )
        }, onError = {
            unCheckNum.set("+0")
        })
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
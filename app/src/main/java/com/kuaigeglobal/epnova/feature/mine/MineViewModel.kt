package com.kuaigeglobal.epnova.feature.mine

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.response.SimpleBannerInfo
import com.kuaigeglobal.epnova.core.response

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:我的
 * @Data: 2025/4/18  14:10
 * @Version: 1.0
 * *****************************************
 */
class MineViewModel : AppRepositoryViewModel<AppRepository>() {

    //是否登录
    val isLogin by lazy { ObservableBoolean(false) }

    //用户资料Map
    val userInfo by lazy { ObservableField<Map<String, String?>>() }

    //没有登录不让进
    val noLogIn = MutableLiveData(false)

    /**
     * 账户余额
     */
    val balance by lazy { ObservableField<String>() }
    val workCourseBanner by lazy { ObservableField<SimpleBannerInfo>() }

    override fun onCreate() {
        super.onCreate()
        refreshUserInfo()
        //更新必要信息
        updateInformation()
    }

    /**
     * 刷新用户信息
     */
    fun refreshUserInfo() {
        User.get().let { user ->
            isLogin.set(!user.isVisitor)
            if (user.isVisitor) {
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
            isLogin.set(!user.isVisitor)
            if (user.isVisitor) {
                notLoggedIn()
                return
            }
            noLogIn.value = false
            Global.refreshBalance()
            launch {
                response(
                    bean = mRepo.api.getUserHomeInfo(UserTokenBody()),
                    result = {
                        if (data == null) return@response
                        User.get().updateFrom(data)
                        logOn()
                    })
            }
        }
    }


    private fun notLoggedIn() {
        userInfo.set(
            mapOf(
                "id" to "",
                "nickname" to "登录注册》",
                "avatar" to "",
            )
        )
        noLogIn.value = true
        balance.set("0")
        workCourseBanner.set(Config.get().appConfig.workCourseBanner)
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
        balance.set(user.computingPower.toString())
        workCourseBanner.set(if (user.isPurchasedCourse()) null else Config.get().appConfig.workCourseBanner)
    }
}
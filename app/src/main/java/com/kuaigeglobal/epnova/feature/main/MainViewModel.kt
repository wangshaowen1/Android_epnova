package com.kuaigeglobal.epnova.feature.main

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.response.ActivityConfigData
import com.kuaigeglobal.epnova.core.network.apis.response.DramasInfoData
import com.kuaigeglobal.epnova.core.network.apis.response.FirstRechargeDialogData
import com.kuaigeglobal.epnova.core.network.apis.response.UserHomeData
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.play.more.ShareShortDramaViewModel
import com.kuaigeglobal.epnova.feature.splash.newer.FirstRechargeDialog
import com.kuaigeglobal.epnova.kit.SignUtils
import com.nhsd.basic.ext.showToast

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2025/4/17  9:38
 * @Version: 1.0
 * *****************************************
 */
class MainViewModel : ShareShortDramaViewModel<AppRepository>() {

    //显示隐藏底部
    val showBottomBar = ObservableBoolean(true)

    //是否登录
    val isLogin by lazy { ObservableBoolean(false) }
    val fromPage by lazy { mBundle.getString(Constants.Extra.FROM_PAGE) }

    private var fetchingUserInfo = false

    //当前显示的tab是第几个
    val showTab = ObservableInt(0)

    //作品中是否是编辑状态
    val isEditing = ObservableBoolean(false)

    //设置编辑的回调
    var setIsEditing: ((Boolean) -> Unit)? = null

    //去创作
    val startCreating = MutableLiveData<Boolean>()

    val generating = ObservableField<DramasInfoData>()
    val activityConfigCache = MutableLiveData<ActivityConfigData>()
    val firstRechargeCache = MutableLiveData<FirstRechargeDialogData>()

    fun initialization() {
        //获取用户主页信息接口
        fetchUserInfo()
        initHomeConfig()
    }



    override fun onResume() {
        super.onResume()
        isLogin.set(User.get().isLogged)
        if (isLogin.get()) {
            Global.refreshBalance()
        }
    }

    /**
     * 获取用户主页信息接口
     */
    fun fetchUserInfo() {
        if (User.get().isVisitor) {
            return
        }
        if (fetchingUserInfo) return
        fetchingUserInfo = true
        launch(
            block = {
                response(
                    bean = mRepo.api.getUserHomeInfo(UserTokenBody()),
                    result = {
                        fetchingUserInfo = false
                        User.get().updateFrom(this.data)
                    },
                    error = { fetchingUserInfo = false }
                )
            },
            onError = { fetchingUserInfo = false }
        )
    }


    /**
     * 用户日活记录
     */
    fun daily() {
        if (User.get().isVisitor) return
        launch {
            response(
                bean = mRepo.api.daily(UserTokenBody()),
                result = {})
        }
    }


    fun getTabColorSelector(showTab: Int): Int {
        return if (showTab == 0) R.color.main_tab_txt_color_dark else R.color.main_tab_txt_color
    }

    // 是否本次显示的充值弹窗 优先级最高
    var isFirstRechargeLogicActive = false
    //true 表示有活动弹窗需要显示 但因为优先级原因本次没显示 下一次显示
    var hasActivityConfig = false

    fun initHomeConfig() {
        launch {
            if (fromPage == MainActivity.FROM_NEWER) {
                // 1. 检查本地和登录状态，提前拦截
                val canShowFirstRecharge = !User.get().hasPaid() && !Config.get().isFirstRechargeShown(FirstRechargeDialog.SCENE_HOME)
                if (canShowFirstRecharge) {
                    // 2. 请求首充接口 (这里必须先于活动配置完成)
                    val firstRechargeResponse = mRepo.api.getFirstRechargeDialogInfo()
                    if (firstRechargeResponse.data != null && firstRechargeResponse.data?.isNeedShow() == true) {
                        isFirstRechargeLogicActive = true
                        firstRechargeCache.value = firstRechargeResponse.data
                    } else {
                        isFirstRechargeLogicActive = false
                    }
                } else {
                    isFirstRechargeLogicActive = false
                }
            }
            // 3. 首充判断结束后，再请求/分发活动配置
            val activityResponse = mRepo.api.activityConfig()
            // 这里赋值后，HomeFragment 会收到通知，此时 isFirstRechargeLogicActive 状态已经确定了
            activityConfigCache.value = activityResponse.data
            if (isFirstRechargeLogicActive && activityResponse.data != null) {
                hasActivityConfig = true
            }
        }
    }

}
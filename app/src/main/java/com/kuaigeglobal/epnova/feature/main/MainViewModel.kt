package com.kuaigeglobal.epnova.feature.main

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.response.LoginTaskData
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.play.more.ShareShortDramaViewModel

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

    val loginTaskData = MutableLiveData<LoginTaskData>()

    fun initialization() {
        //获取用户主页信息接口
        fetchUserInfo()
    }


    override fun onResume() {
        super.onResume()
        isLogin.set(User.get().isLogged)
        if (isLogin.get()) {
            Global.refreshCoinsAndPoints()
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

                        if (User.get().isGuest && loginTaskData.value == null) {
                            getLoginTaskData()
                        }

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
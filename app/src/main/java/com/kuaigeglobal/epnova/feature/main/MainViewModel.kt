package com.kuaigeglobal.epnova.feature.main

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.request
import com.kuaigeglobal.epnova.core.network.apis.entity.PopupClaimInfo
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
    private var claimingPopup = false
    private var claimedPopupUserId: String? = null

    //当前显示的tab是第几个
    val showTab = ObservableInt(0)

    //作品中是否是编辑状态
    val isEditing = ObservableBoolean(false)

    private val _creatingEncouragementPopup = MutableLiveData<PopupClaimInfo?>()
    val creatingEncouragementPopup: LiveData<PopupClaimInfo?> = _creatingEncouragementPopup

    //设置编辑的回调
    var setIsEditing: ((Boolean) -> Unit)? = null

    fun initialization() {
        //获取用户主页信息接口
        fetchUserInfo()
        //首页书架配置（前置）
        loadBookShelfConfig()
    }

    fun claimCreatingEncouragementPopup() {
        val userId = User.get().id?.takeIf { it.isNotBlank() } ?: return
        if (claimingPopup || claimedPopupUserId == userId) return

        claimingPopup = true
        request(
            apiBlock = { mRepo.api.claimPopup() },
            showToast = false,
            onError = { claimingPopup = false }
        ) {
            claimingPopup = false
            claimedPopupUserId = userId
            if (User.get().id == userId) {
                data?.takeIf { it.showPopup == 1 }?.let {
                    _creatingEncouragementPopup.value = it
                }
            }
        }
    }

    fun consumeCreatingEncouragementPopup() {
        _creatingEncouragementPopup.value = null
    }

    private fun loadBookShelfConfig() {
        launch {
            response(
                bean = mRepo.api.bookShelfConfigList(),
                result = { data?.let { Config.get().storeBookShelfConfig(it) } },
                // 前置请求仅更新缓存，页面加载与重试由 HomeTabsViewModel 负责。
                error = {}
            )
        }
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
}

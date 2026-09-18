package com.kuaigeglobal.epnova.feature.home

import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.LoadState
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.network.apis.entity.BookShelfConfigInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.BookShelfSectionInfo
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.response.LoginTaskData
import com.kuaigeglobal.epnova.core.response

/** Home 容器负责书架配置加载、顶部入口和跨 Tab 的登录奖励状态。 */
class HomeTabsViewModel : AppRepositoryViewModel<AppRepository>() {

    /** Mine/Home 顶部奖励入口的未领取数量。 */
    val unCheckNum = ObservableInt()
    /** 登录奖励欢迎弹窗使用的任务数据。 */
    val loginTaskData = MutableLiveData<LoginTaskData>()
    /** 书架配置加载状态，驱动主页的加载、空态和错误展示。 */
    val bookShelfConfigLoadState = MutableLiveData<LoadState>(LoadState.Loading)

    /** 避免重复触发书架配置请求。 */
    private var loadingBookShelfConfig = false

    /** 优先使用本地书架配置；没有可展示的 Tab 时再请求远端配置。 */
    override fun onCreate() {
        super.onCreate()
        getRewardUserUncheck()
        if (Config.get().bookShelfConfigList.any { it.bookshelfTab == 1 || it.bookshelfTab == 2 }) {
            bookShelfConfigLoadState.value = LoadState.NotLoading(true)
        } else {
            loadBookShelfConfig()
        }
    }

    /** 请求并持久化书架配置，供 HomeFragment 创建固定的子页面列表。 */
    fun loadBookShelfConfig() {
        if (loadingBookShelfConfig) return
        loadingBookShelfConfig = true
        bookShelfConfigLoadState.value = LoadState.Loading
        launch(
            block = {
                response(
                    bean = mRepo.api.bookShelfConfigList(),
                    result = {
                        data?.let { Config.get().storeBookShelfConfig(it) }
                        bookShelfConfigLoadState.value = LoadState.NotLoading(true)
                    },
                    error = {
                        bookShelfConfigLoadState.value = LoadState.Error(this?.apiException ?: Exception())
                    }
                )
            },
            onError = { bookShelfConfigLoadState.value = LoadState.Error(it) },
            onComplete = { loadingBookShelfConfig = false }
        )
    }

    /** 访客不请求奖励未读数，普通用户失败时回退为 0，避免残留红点。 */
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

    /** 获取登录任务弹窗数据，仅发布需要展示且奖励有效的任务。 */
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

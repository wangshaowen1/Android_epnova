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

    val unCheckNum = ObservableInt()
    val loginTaskData = MutableLiveData<LoginTaskData>()
    val bookShelfConfigLoadState = MutableLiveData<LoadState>(LoadState.Loading)

    private var loadingBookShelfConfig = false

    override fun onCreate() {
        super.onCreate()
        getRewardUserUncheck()
        Config.get().storeBookShelfConfig(mockBookShelfConfigList) //现在没数据 临时测试数据
        if (Config.get().bookShelfConfigList.any { it.bookshelfTab == 1 || it.bookshelfTab == 2 }) {
            bookShelfConfigLoadState.value = LoadState.NotLoading(true)
        } else {
            loadBookShelfConfig()
        }
    }


    private val mockBookShelfConfigList by lazy {
        listOf(
            BookShelfConfigInfo(
                bookshelfTab = 1,
                bookshelfTabName = "Trending",
                bookshelfSectionList = listOf(
                    BookShelfSectionInfo(
                        bookshelfName = "New Release",
                        bookshelfSectionCode = 1001,
                        bookshelfType = 1,
                        sortNo = 3
                    ),
                    BookShelfSectionInfo(
                        bookshelfName = "Most Popular",
                        bookshelfSectionCode = 1002,
                        bookshelfType = 1,
                        sortNo = 2
                    ),
                    BookShelfSectionInfo(
                        bookshelfName = "Recommended",
                        bookshelfSectionCode = 1003,
                        bookshelfType = 2,
                        sortNo = 1
                    )
                )
            ),
            BookShelfConfigInfo(
                bookshelfTab = 2,
                bookshelfTabName = "All Dramas",
                bookshelfSectionList = listOf(
                    BookShelfSectionInfo(
                        bookshelfName = "Weekly Top",
                        bookshelfSectionCode = 2001,
                        bookshelfType = 1,
                        sortNo = 2
                    ),
                    BookShelfSectionInfo(
                        bookshelfName = "Weekly New",
                        bookshelfSectionCode = 2002,
                        bookshelfType = 1,
                        sortNo = 1
                    )
                )
            )
        )
    }

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

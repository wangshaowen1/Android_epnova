package com.kuaigeglobal.epnova.feature.home

import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.EntityCache
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.db.mapper.toDomain
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.network.apis.entity.BannerInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.core.network.apis.request.BannerBody
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.response.HomeBookShelfData
import com.kuaigeglobal.epnova.core.network.apis.response.LoginTaskData
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.home.data.VideoRemoteMediator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map


class HomeViewModel : AppRepositoryViewModel<AppRepository>() {

    val bannerData = MutableLiveData<List<BannerInfo>>()
    val unCheckNum = ObservableInt()
    private val bannerCache: EntityCache<List<BannerInfo>> by lazy {
        object : EntityCache<List<BannerInfo>>("HomeBannerList") {}
    }

    val bookShelfData = MutableLiveData<HomeBookShelfData>()
    val bookShelfError = MutableLiveData<Throwable?>()
    private val bookShelfCache: EntityCache<HomeBookShelfData> by lazy {
        object : EntityCache<HomeBookShelfData>("HomeBookShelfData") {}
    }

    val loginTaskData = MutableLiveData<LoginTaskData>()

    override fun onCreate() {
        super.onCreate()
        bannerCache.cache?.let { bannerData.value = it }
        bookShelfCache.cache?.let { bookShelfData.value = it }
        getData()
        loadData()
        getRewardUserUncheck()
    }

    fun getData() {
        launch({
            response(mRepo.api.getAppBanner(BannerBody(BannerBody.LOCATION_HOME)), {
                bannerData.value = this.data
                bannerCache.cache(this.data) // 更新缓存
            })
        })
    }

    fun loadData() {
        launch(
            block = {
                bookShelfError.value = null
                response(mRepo.api.bookShelfHome(), {
                    bookShelfData.value = data
                    bookShelfCache.cache(data) // 更新缓存
                }, {
                    bookShelfError.value = this?.apiException ?: RuntimeException()
                })
            },
            onError = {
                bookShelfError.value = it
            }
        )
    }

    fun getRewardUserUncheck() {
        if (User.get().isVisitor) {
            unCheckNum.set(0)
            return
        }
        launch(block = {
            response(
                bean = mRepo.api.rewardUserUncheck(UserTokenBody()),
                result = {
                    unCheckNum.set(data?.unCheckNum ?: 0)
                },
                error = {
                    unCheckNum.set(0)
                }
            )
        }, onError = {
            unCheckNum.set(0)
        })
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


    // 用于触发瀑布流加载的 ID。当它改变时，flatMapLatest 会自动取消旧的 Pager 并创建新的
    val currentWaterfallId = MutableStateFlow<String>("")

    @OptIn(ExperimentalPagingApi::class, ExperimentalCoroutinesApi::class)
    val feedFlow: Flow<PagingData<WorkVOInfo>> = currentWaterfallId
        .flatMapLatest { bookshelfId ->
            // 【新增逻辑】：如果后端没有下发瀑布流配置，或者 ID 为空
            if (bookshelfId.isEmpty()) {
                // 直接返回空的 Paging 流，ConcatAdapter 会自动清空底部瀑布流 UI
                kotlinx.coroutines.flow.flowOf(PagingData.empty())
            } else {
                // 如果有真实的 ID，才去构建 Pager 发起数据库和网络请求
                Pager(
                    config = PagingConfig(pageSize = 10, enablePlaceholders = false, prefetchDistance = 6),
                    remoteMediator = VideoRemoteMediator(bookshelfId, mRepo.appDatabase, mRepo.api),
                    pagingSourceFactory = { mRepo.appDatabase.workDao.getPagingData() }
                ).flow.map { pagingData ->
                    pagingData.map { entity -> entity.toDomain() }
                }
            }
        }
        .cachedIn(viewModelScope)
}

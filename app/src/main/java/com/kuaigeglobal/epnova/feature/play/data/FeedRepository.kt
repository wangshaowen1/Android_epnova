package com.kuaigeglobal.epnova.feature.play.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.kuaigeglobal.epnova.core.db.AppDatabase
import com.kuaigeglobal.epnova.core.db.mapper.toDomain
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.feature.home.data.VideoRemoteMediator
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * 全局单例仓库：负责数据分发、缓存管理和预加载桥接
 */
object FeedRepository {

    private const val TAG = "FeedRepository"

    // 1. 定义一个全局作用域，确保 Paging 数据流在 ViewModel 销毁后依然存活（针对推荐流）
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val appDatabase = AppDatabase.getInstance()

    // ------------------- 缓存区 -------------------

    // [Cache A] 推荐流缓存 (长期持有，除非强制刷新或退出APP)
    private var cachedRecommendFlow: Flow<PagingData<WorkInfo>>? = null

    // [Cache B] 剧集流缓存 (只缓存“当前/最后”打开的那一部剧，防止切回来重加载)
    private var cachedSeriesFlow: Flow<PagingData<WorkInfo>>? = null
    private var lastSeriesId: String? = null // 配合 cachedSeriesFlow 使用，防止张冠李戴

    // [Cache C] 预加载数据 (内存直传，用于 Series 模式下的过滤列表)
    private var preloadedSeriesData: List<WorkInfo>? = null
    private var preloadedWorkId: String? = null // 用于校验预加载数据归属

    // ------------------- 预加载逻辑 (内存直传) -------------------

    /**
     * 外部调用：设置预加载数据 (通常在跳转前的 Activity/Fragment 调用)
     * @param workId 当前作品ID (用于校验)
     * @param list 已经过滤掉未完成集数、并转换为 WorkInfo 的列表
     */
    fun setPreloadedSeries(workId: String, list: List<WorkInfo>) {
        Logger.d(TAG, "Setting preload data for id=$workId, size=${list.size}")
        preloadedWorkId = workId
        preloadedSeriesData = list
    }

    /**
     * 获取剧集模式的数据流：
     * 策略：优先使用内存预加载数据 -> 其次使用网络请求 (cachedSeriesFlow)
     */
    fun getSeriesFlowOrNetwork(
        workId: String,
        isSeriesMode: Boolean
    ): Flow<PagingData<WorkInfo>> {
        if (!isSeriesMode) return getFeedFlow(false, null) // 容错处理

        // 1. 优先检查是否有预加载的数据 (且 ID 匹配)
        val preload = preloadedSeriesData
        val preloadId = preloadedWorkId

        if (preload != null && preload.isNotEmpty() && preloadId == workId) {
            Logger.d(TAG, "Using PRELOADED series data for id=$workId")
            // 直接将 List 转为 PagingData，无需网络，秒开
            return flowOf(PagingData.from(preload))
        }

        // 2. 如果没有预加载数据（比如内存回收、DeepLink进入），则走网络兜底
        Logger.d(
            TAG,
            "No valid preload data (req=$workId, cache=$preloadId), fetching from NETWORK..."
        )
        return getSeriesFlowInternal(workId, forceRefresh = true)
    }

    // ------------------- 网络/Paging 逻辑 -------------------

    /**
     * 通用获取入口：根据模式分发
     */
    fun getFeedFlow(
        isSeriesMode: Boolean,
        id: String?,
        forceRefresh: Boolean = false
    ): Flow<PagingData<WorkInfo>> {
        return if (isSeriesMode) {
            getSeriesFlowInternal(id, forceRefresh)
        } else {
            getRecommendFlowInternal(forceRefresh)
        }
    }

    /**
     * 内部实现：获取推荐流 (Recommend Mode)
     */
    private fun getRecommendFlowInternal2(forceRefresh: Boolean): Flow<PagingData<WorkInfo>> {
        // 1. 命中缓存：不强制刷新 且 缓存存在
        if (!forceRefresh && cachedRecommendFlow != null) {
            Logger.d(TAG, "Returning CACHED -> RECOMMEND flow")
            return cachedRecommendFlow!!
        }

        Logger.d(TAG, "Creating NEW -> RECOMMEND flow")

        // 2. 创建新流
        val newFlow = Pager(
            config = PagingConfig(
                pageSize = 10,
                prefetchDistance = 3,
                enablePlaceholders = false,
                initialLoadSize = 10
            ),
            pagingSourceFactory = {
                // 使用原有的推荐流接口
                VideoFeedPagingSource()
            }
        ).flow.cachedIn(repositoryScope) // [关键] 绑定全局生命周期

        // 3. 更新缓存
        cachedRecommendFlow = newFlow
        return newFlow
    }

    @OptIn(ExperimentalPagingApi::class)
    private fun getRecommendFlowInternal(forceRefresh: Boolean): Flow<PagingData<WorkInfo>> {
        // 1. 如果流已经存在，直接返回它！（实现多界面共享的核心）
        if (cachedRecommendFlow != null && !forceRefresh) {
            Logger.d(TAG, "Returning Shared CACHED -> RECOMMEND flow")
            return cachedRecommendFlow!!
        }

        Logger.d(TAG, "Creating NEW -> RECOMMEND flow with Room & Mediator")

        // 2. 只有第一次（或强制要求重建时），才创建 Pager
        val newFlow = Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 3,
                initialLoadSize = 20
            ),
            remoteMediator = VideoRemoteMediator(appDatabase, ApiFactory.apiService),
            pagingSourceFactory = { appDatabase.workDao.getPagingData() }
        ).flow
         .map { pagingData -> pagingData.map { it.toDomain() } } // 转回业务模型
         .cachedIn(repositoryScope) // 绑定在 Repository 的全局作用域上

        // 3. 缓存起来，后续界面直接复用
        cachedRecommendFlow = newFlow
        return newFlow
    }

    /**
     * 内部实现：获取剧集流 (Series Mode) - 网络兜底用
     * todo 这个逻辑暂不需要  数据源也没有做转换  暂时不要用
     */
    private fun getSeriesFlowInternal(
        id: String?,
        forceRefresh: Boolean
    ): Flow<PagingData<WorkInfo>> {
        val safeId = id ?: ""

        // 1. 命中缓存：不强制刷新 且 缓存存在 且 ID一致
        if (!forceRefresh && cachedSeriesFlow != null && lastSeriesId == safeId) {
            Logger.d(TAG, "Returning CACHED -> SERIES flow for id=$safeId")
            return cachedSeriesFlow!!
        }

        Logger.d(TAG, "Creating NEW -> SERIES flow for id=$safeId")

        // 2. 创建新流
        val newFlow = Pager(
            config = PagingConfig(
                pageSize = 30,
                enablePlaceholders = false,
                initialLoadSize = 30
            ),
            pagingSourceFactory = {
                VideoFeedPagingSource()
            }
        ).flow.cachedIn(repositoryScope)

        // 3. 更新缓存
        cachedSeriesFlow = newFlow
        lastSeriesId = safeId

        return newFlow
    }

    /**
     * 清理缓存（退出登录或模块销毁时调用）
     */
    fun clearCache() {
        cachedRecommendFlow = null
        cachedSeriesFlow = null
        lastSeriesId = null
        preloadedSeriesData = null
        preloadedWorkId = null
        Logger.d(TAG, "Cache cleared")
    }
}
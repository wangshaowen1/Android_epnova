package com.kuaigeglobal.epnova.feature.home.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.kuaigeglobal.epnova.core.db.AppDatabase
import com.kuaigeglobal.epnova.core.db.entity.RemoteKeys
import com.kuaigeglobal.epnova.core.db.entity.WorkInfoEntity
import com.kuaigeglobal.epnova.core.db.mapper.toEntity
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiService
import com.kuaigeglobal.epnova.core.network.apis.request.BookShelfIdPagedBody

@OptIn(ExperimentalPagingApi::class)
class VideoRemoteMediator(
    private val bookShelfId: String?,
    private val database: AppDatabase,
    private val api: ApiService
) : RemoteMediator<Int, WorkInfoEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, WorkInfoEntity>
    ): MediatorResult {
        // 1. 确定要请求的页码
        val page = when (loadType) {
            LoadType.REFRESH -> 1 // 首页Feed流下拉刷新通常直接请求第一页
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true) // Feed流通常不支持向上翻页，直接返回
            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                // 【修复】处理首次加载或DB被清空时的边界情况
                val nextKey = remoteKeys?.nextKey
                if (nextKey == null) {
                    return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                }
                nextKey
            }
        }

        return try {
            // 2. 发起网络请求
            val response = api.homeWaterfallPage(BookShelfIdPagedBody(bookShelfId, page, state.config.pageSize))

            // 原版直接取 data，如果接口报错（如 token 失效），会吞掉异常并被误认为是列表到底了。
            if (!response.isSuccess) {
                return MediatorResult.Error(ApiException(response.code, response.msg))
            }

            val items = response.data?.items?.filterNotNull() ?: emptyList()

            // 不能只判断 isNullOrEmpty，如果返回的数据量小于请求的 pageSize，说明已经是最后一页了
            val isEndOfList = items.isEmpty() || items.size < state.config.pageSize

            // 3. 开启数据库事务，保证 RemoteKey 和 Data 的一致性
            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    // 刷新时清空旧缓存
                    database.remoteKeysDao.clearRemoteKeys()
                    database.workDao.clearAll()
                }

                val prevKey = if (page == 1) null else page - 1
                val nextKey = if (isEndOfList) null else page + 1

                // 构造 RemoteKeys 列表
                val keys = items.map {
                    RemoteKeys(workId = it.workId ?: "", prevKey = prevKey, nextKey = nextKey)
                }

                // 把后端的 WorkInfo 转为数据库的 WorkInfoEntity
                val entityList = items.map { it.toEntity(page) }

                database.remoteKeysDao.insertAll(keys)
                database.workDao.insertAll(entityList)
            }

            MediatorResult.Success(endOfPaginationReached = isEndOfList)
        } catch (e: Exception) {
            // 捕获网络异常等
            MediatorResult.Error(e)
        }
    }

    // 获取当前列表中最后一个 item 对应的 RemoteKey
    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, WorkInfoEntity>): RemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()?.let { entity ->
            database.remoteKeysDao.getRemoteKeysId(entity.workId)
        }
    }
}
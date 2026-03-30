package com.kuaigeglobal.epnova.feature.works

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.db.AppDatabase
import com.kuaigeglobal.epnova.core.db.entity.WorkInformationInfo
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.request.MyWorkBody
import kotlin.coroutines.cancellation.CancellationException

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:作品
 * @Data: 2026/2/3/周二  9:37
 * @Version: 1.0
 * *****************************************
 */
@OptIn(ExperimentalPagingApi::class)
class WorkInformationRemoteMediator(
    private val database: AppDatabase,
) : RemoteMediator<Int, WorkInformationInfo>() {

    private val complaintDetailsDao = database.workInformationDao()


    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, WorkInformationInfo>
    ): MediatorResult {
        return try {
            // 根据加载类型决定页码
            val page = when (loadType) {
                LoadType.REFRESH -> {
                    // 刷新操作，通常从头开始加载或根据策略决定
//                    1
                    val anchorPosition = state.anchorPosition ?: return MediatorResult.Success(endOfPaginationReached = false)
                    val item = state.closestItemToPosition(anchorPosition)

                    // 计算当前项所在的页码。
                    // 如果你的后端不支持通过 ID 刷新，只能通过页码，
                    // 建议计算方式：(anchorPosition / state.config.pageSize) + 1
                    if (item == null) 1 else (anchorPosition / state.config.pageSize) + 1
                }

                LoadType.PREPEND -> {
                    // 向前加载（加载更旧的数据），通常使用第一页的 prevKey
                    // 如果假设不需要向前加载，返回 null 表示结束
                    return MediatorResult.Success(endOfPaginationReached = true)
                }

                LoadType.APPEND -> {
                    // 获取当前已加载的最后一项，从中得到下一页的页码
                    val lastItem = state.lastItemOrNull()

                    if (lastItem == null) 1 else (state.pages.sumOf { it.data.size } / state.config.pageSize) + 1
//                    if (lastItem == null) {
//                        1 // 如果没有加载过任何项，也从第1页开始
//                    } else {
//                        // 假设你的 lastItem 里有一个 page 字段记录它属于哪一页
//                        // 或者，更常见的，你根据 state 的配置和已加载项的数量来计算
//                        // 这里是一种简单的计算方式：最后一页的页码 + 1
//                        state.pages.size + 1
//                    }
                }
            }

            if (User.get().isVisitor) {
                //没有登录，是游客
                database.withTransaction {
                    // 刷新时清空旧数据
                    complaintDetailsDao.clearAll()
                }
//                val throwable = ApiException(ApiException.AUTHORIZATION_ERROR, R.string.toast_login_please.getResString())
                return MediatorResult.Success(endOfPaginationReached = true)
            }

            val body = MyWorkBody(
                workId = null,
                pageNum = page,
                pageSize = state.config.pageSize
            )
            val response = ApiFactory.apiService.getMyWork(body)
            if (response.isSuccess.not()) {
                return MediatorResult.Error(ApiException(response.code, response.msg))
            }

            val workInformation = mutableListOf<WorkInformationInfo>()
            // 从网络获取数据
            val data = response.data?.items

            data?.mapIndexed { index, workInformationData ->
                val absoluteSortIndex = (page - 1) * state.config.pageSize + index
                WorkInformationInfo(
                    workId = workInformationData.workId ?: "",
                    sortIndex = absoluteSortIndex,
                    work = workInformationData
                )
            }?.let { workInformation.addAll(it) }

            // 在数据库事务中处理数据
            database.withTransaction {
                if (page <= 1) {
                    // 刷新时清空旧数据
                    complaintDetailsDao.clearAll()
                }
                // 插入新数据
                complaintDetailsDao.insertAll(workInformation.toList())
            }
            // 加载下一页的键（为 null 表示没有下一页） true 表示没有更多数据了
            val endOfPaginationReached = data.isNullOrEmpty() || data.size < state.config.pageSize
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            MediatorResult.Error(e)
        }
    }
}
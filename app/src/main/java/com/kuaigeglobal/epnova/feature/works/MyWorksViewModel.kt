package com.kuaigeglobal.epnova.feature.works

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kuaigeglobal.epnova.core.db.entity.WorkInformationInfo
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constant
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.core.network.apis.request.DeleteWorkBody
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkIdPagedBody
import com.kuaigeglobal.epnova.core.response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

class MyWorksViewModel : AppRepositoryViewModel<AppRepository>() {

    //当前显示状态是查看(true)还是编辑(false)
    var isView = MutableLiveData(true)

    //合成中列表数据
    var workInformationIds = hashMapOf<String, WorkInfo>()

    //刷新列表
    val isRefresh = MutableLiveData(false)

    //当前页面显示状态
    val isShow = AtomicBoolean(false)

    //当前选中的数量
    var selectedCount = MutableLiveData(0)

    //作品详情
    val workDtl = MutableLiveData<WorkInfo?>()

    //剧集详情
    val episodeList = MutableLiveData<List<EpisodeInfo>?>()

    // 1. 创建一个重载触发器，初始发射一个 Unit 确保首次加载
    private val reloadTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    private var checkGeneratingJob: Job? = null

    /**
     * 投诉消息列表
     */
    @OptIn(ExperimentalPagingApi::class, ExperimentalCoroutinesApi::class)
    fun getWorkInformation(): Flow<PagingData<WorkInformationInfo>> {
        return reloadTrigger.flatMapLatest {
            Pager(
                config = PagingConfig(
                    initialLoadSize = Constant.Paged.PAGE_SIZE,
                    pageSize = Constant.Paged.PAGE_SIZE,
                    prefetchDistance = 200,
                ), remoteMediator = WorkInformationRemoteMediator(
                    database = mRepo.appDatabase
                )
            ) {
                mRepo.appDatabase.workInformationDao().getPosts()
            }.flow
        }.cachedIn(viewModelScope)
    }

    /**
     * 专门为切换账号/严重状态变更准备的重置方法
     */
    fun resetPagingForNewUser() {
        launch {
            // 1. 先彻底清空上一任账号的数据库残留
            mRepo.appDatabase.workInformationDao().clearAll()
            // 2. 触发生成全新的 Pager 数据流
            reloadTrigger.emit(Unit)
        }
    }


    /**
     * 删除作品
     */
    fun delete(workIds: List<String>) {
        if (workIds.isEmpty()) return
        launchWithLoading {
            response(
                bean = mRepo.api.batchDeleteWork(DeleteWorkBody(ids = workIds)),
                result = {
                    launch {
                        //删除数据库中的
                        mRepo.appDatabase.workInformationDao().deleteByWorkIds(workIds)
                        isView.value = true
                    }
                })
        }
    }

    /**
     * 删除全部作品
     */
    fun deleteAll() {
        launch {
            //删除数据库中的
            mRepo.appDatabase.workInformationDao().clearAll()
        }
    }


    /**
     * 开始循环请求
     */
    fun startChecking() {
        if (User.get().isVisitor
            || !isShow.get()
        ) {
            stopCheckGenerating()
            return
        }
        if (checkGeneratingJob?.isActive == true) return
        checkGeneratingJob = viewModelScope.launch {
            // 注意：内部 checkGenerating 涉及网络，切换到 Dispatchers.IO
            while (isActive && isShow.get()) {
                try {
                    // 将网络请求包装在 withContext(Dispatchers.IO)
                    val hasGenerating = doCheckGeneratingWork()

                    // 如果已经没有正在生成的了，自动停掉轮询节省资源
                    if (!hasGenerating) {
                        stopCheckGenerating()
                        break
                    }

                    delay(3000) // 间隔
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    e.printStackTrace()
                    delay(5000) // 出错后延长下次重试时间
                }
            }
        }
    }

    /**
     *查询是否有正在生成中的
     */
    private suspend fun doCheckGeneratingWork(): Boolean = withContext(Dispatchers.IO) {
        val beam = mRepo.api.getWorkStatusList(UserTokenBody())
        if (!beam.isSuccess) return@withContext false

        val items = beam.data

        // 逻辑处理：如果没有生成中的数据
        if (items.isNullOrEmpty()) {
            if (workInformationIds.isNotEmpty()) {
                withContext(Dispatchers.Main) { isRefresh.value = true }
                workInformationIds.clear()
            }
            return@withContext false
        }

        // 处理逻辑：更新数据库和内存状态
        val newWorkInformationId = hashMapOf<String, WorkInfo>()
        items.forEach { newWorkInformationId[it.workId ?: ""] = it }

        upsertByPictureBookIdAndType(items)

        if (newWorkInformationId.keys != workInformationIds.keys) {
            workInformationIds.clear()
            workInformationIds.putAll(newWorkInformationId)
            withContext(Dispatchers.Main) { isRefresh.value = true }
        }
        return@withContext true // 还有正在生成的，继续轮询
    }

    fun upsertByPictureBookIdAndType(pictureBooks: List<WorkInfo>?) {
        if (pictureBooks.isNullOrEmpty()) return

        viewModelScope.launch {
            // 1. 提取所有需要更新的 workId 集合
            val workIds = pictureBooks.mapNotNull { it.workId }
            if (workIds.isEmpty()) return@launch

            // 2. ⚡️ 一次数据库交互：批量拿到这批数据的旧 sortIndex
            val oldSortIndexes = mRepo.appDatabase.workInformationDao().getSortIndexesByIds(workIds)

            // 3. 转换为内存 Map，方便极速查找
            // 最终格式大概是：{ "id_A" -> 5, "id_B" -> 6 }
            val sortIndexMap = oldSortIndexes.associateBy({ it.workId }, { it.sortIndex })

            // 4. 带着原来的排队号，重新组装全量数据
            val updatedEntities = pictureBooks.mapNotNull { workInfo ->
                val workId = workInfo.workId ?: return@mapNotNull null

                // 从 Map 中极速取出旧的排队号。如果是个新来的(极少见)，兜底给个 0
                val oldSortIndex = sortIndexMap[workId] ?: 0

                WorkInformationInfo(
                    workId = workId,
                    sortIndex = oldSortIndex, // 完美继承旧的顺序！
                    work = workInfo
                )
            }

            // 5. ⚡️ 第二次数据库交互：全量覆盖更新
            mRepo.appDatabase.workInformationDao().upsertWorkInformationDao(updatedEntities)
        }
    }
    /**
     * 作品详情
     */
    fun getWorkDtl(workId: String?) {
        if (workId.isNullOrEmpty()) return
        launchWithLoading(
            block = {
                mRepo.api.getWorkInfo(WorkIdBody(workId = workId)).data?.let {
                    workDtl.value = it
                }
            }
        )
    }

    /**
     * 获取剧集列表
     */
    fun getEpisodes(workId: String?) {
        if (workId.isNullOrEmpty()) return

        launchWithLoading {
            response(mRepo.api.episodeList(WorkIdPagedBody(workId, 1, 20))) {
                episodeList.value = data?.items
            }
        }
    }


    /**
     * 关闭（彻底停止）
     */
    fun stopCheckGenerating() {
        checkGeneratingJob?.cancel()
        checkGeneratingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopCheckGenerating()
    }
}
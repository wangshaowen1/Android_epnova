package com.kuaigeglobal.epnova.feature.works

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constant
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.db.entity.WorkInformationInfo
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
        // 如果已经在运行，先取消
        checkGeneratingJob?.cancel()
        if (checkGeneratingJob?.isActive == true) return
        checkGeneratingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    checkGenerating()
                    delay(3000)
                } catch (e: CancellationException) {
                    // 这是正常关闭（通过 stopCheckGenerating() 或 ViewModel 销毁）,正常退出，不需要额外处理
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                    stopCheckGenerating()
                }
            }
        }
    }

    /**
     *查询是否有正在生成中的
     */
    private suspend fun checkGenerating() {
        if (User.get().isVisitor || !isShow.get()) {
            workInformationIds.clear()
            stopCheckGenerating()
            return
        }
        val beam = mRepo.api.getWorkStatusList(UserTokenBody())
        if (beam.isSuccess.not()) return
        val items = beam.data
        if (items.isNullOrEmpty()) {
            //没有正在生成的绘本
            if (workInformationIds.isNotEmpty()) {
                //原来有但现在没了，需要刷新一下
                withContext(Dispatchers.Main) {
                    isRefresh.value = true
                }
                workInformationIds.clear()
            }
            stopCheckGenerating()
            return
        }
        //有那就需要刷新了
        val newWorkInformationId = hashMapOf<String, WorkInfo>()
        items.forEach { workInformation ->
            newWorkInformationId[workInformation.workId ?: ""] = workInformation
        }
        //更新数据库
        upsertByPictureBookIdAndType(items)
        if (newWorkInformationId.keys != workInformationIds.keys) {
            //跟上次的不相同，表示有新的绘本已经完成了，需要更新一下ui
            workInformationIds.clear()
            workInformationIds.putAll(newWorkInformationId)
            withContext(Dispatchers.Main) {
                isRefresh.value = true
            }
        }
    }

    fun upsertByPictureBookIdAndType(pictureBook: List<WorkInfo>?) {
        if (pictureBook == null) return
        viewModelScope.launch {
            val pictureBooks = pictureBook.map {
                WorkInformationInfo(
                    work = it
                )
            }
            mRepo.appDatabase.workInformationDao().upsertWorkInformationDao(pictureBooks)
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
        workInformationIds.clear()
        checkGeneratingJob?.cancel()
        checkGeneratingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopCheckGenerating()
    }
}
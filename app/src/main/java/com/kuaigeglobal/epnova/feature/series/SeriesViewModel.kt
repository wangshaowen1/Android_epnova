package com.kuaigeglobal.epnova.feature.series

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.PagedProxy
import com.kuaigeglobal.epnova.common.adapter.QuickAdapter
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.ext.requestWithLoading
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.core.network.apis.request.DeleteWorkBody
import com.kuaigeglobal.epnova.core.network.apis.request.LatestEpisodeBody
import com.kuaigeglobal.epnova.core.network.apis.response.WorkPublishStatusData
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.widget.LoadStateViewHolder
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.flow.Flow

/**
 * 剧集详情页
 */
class SeriesViewModel : AppRepositoryViewModel<SeriesRepository>() {
    val workId by lazy { mBundle.getString(Constants.Extra.ID) }
    val loadState by lazy { ObservableInt(LoadStateViewHolder.CONTENT) }
    val workInfo by lazy { MutableLiveData<WorkInfo?>() }

    //创建续集信息
    val episodeInfo = MutableLiveData<EpisodeInfo?>()
    val publishedPlatformAdapter by lazy { QuickAdapter<ItemStringVm>(R.layout.item_publish_platform, emptyList()) }

    override fun onResume() {
        super.onResume()
        getWorkDtl()
    }

    /**
     *作品详情
     */
    fun getWorkDtl() {
        if (workInfo.value == null) loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                mRepo.getWorkInfo(workId).data?.let {
                    it.userId = User.get().userId
                    workInfo.value = it
                    it.publishPlatformImageList?.map { ItemStringVm(it) }.let { publishedPlatformAdapter.submitList(it) }
                }
                updateLoadState()
            },
            onError = {
                updateLoadState()
            }
        )
    }

    private fun updateLoadState() {
        loadState.set(LoadStateViewHolder.CONTENT)
        if (workInfo.value == null) {
            loadState.set(LoadStateViewHolder.RETRY)
        } else {
            loadState.set(LoadStateViewHolder.CONTENT)
        }
    }

    fun episodeList(): Flow<PagingData<EpisodeInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = PagedProxy.DEFAULT_PAGE_SIZE * 3,
                pageSize = PagedProxy.DEFAULT_PAGE_SIZE
            ),
            pagingSourceFactory = { EpisodePagingSource(workId) }
        ).flow.cachedIn(viewModelScope)
    }

    /**
     *获取剧集最新一集剧本详情
     */
    fun getEpisodeInfo(workType: Int?) {
        launchWithLoading {
            response(
                mRepo.api.latestEpisode(
                    LatestEpisodeBody(
                        workType = workType,
                        workId = workId
                    )
                )
            ) {
                episodeInfo.value = data
            }
        }
    }

    /**
     * 删除作品
     */
    fun delete() {
        if (workId.isNullOrEmpty()) return
        val workIds = listOf(workId ?: "")
        launchWithLoading {
            response(
                bean = mRepo.api.batchDeleteWork(DeleteWorkBody(ids = workIds)),
                result = {
                    launch {
                        showToast(R.string.delete_success)
                        mRepo.appDatabase.workInformationDao().deleteByWorkIds(workIds)
                        finish()
                    }
                })
        }
    }

    fun publishFailed(workInfo: WorkInfo?) = workInfo?.isPublishReviewFailed() == true

    @ColorRes
    fun publishStatusColor(workInfo: WorkInfo?) = when {
        workInfo?.isPublishReviewFailed() == true -> R.color.func_failure_dark
        workInfo?.coverBadgeType == WorkInfo.COVER_BADGE_IN_REVIEW ||
                workInfo?.globalPublishStatus == WorkInfo.GLOBAL_PUBLISH_STATUS_REVIEWING ||
                workInfo?.publishStatus == WorkInfo.PUBLISH_STATUS_REVIEWING -> R.color.func_warning_dark
        workInfo?.coverBadgeType == WorkInfo.COVER_BADGE_PUBLISHED ||
                workInfo?.globalPublishStatus == WorkInfo.GLOBAL_PUBLISH_STATUS_PUBLISHED ||
                workInfo?.publishStatus == WorkInfo.PUBLISH_STATUS_PASSED ||
                (workInfo?.publishedEpisodeCnt ?: 0) > 0 -> R.color.func_success_dark
        else -> R.color.text_color_3_dark
    }

    fun publishStatusText(workInfo: WorkInfo?) = workInfo?.publishBadgeText().orEmpty()

    fun showPublishStatus(workInfo: WorkInfo?) = !workInfo?.publishBadgeText().isNullOrEmpty()

    fun showPublishButton(workInfo: WorkInfo?) = workInfo?.showPublishButton() == true

    fun enablePublishButton(workInfo: WorkInfo?) = workInfo?.isPublishButtonEnabled() == true

    fun publishButtonText(workInfo: WorkInfo?) = R.string.drama_detail_publish.getResString()

    fun queryWorkPublishStatus(
        openPublishPage: (String?) -> Unit,
        confirmNativePublish: (WorkPublishStatusData) -> Unit
    ) {
        val info = workInfo.value ?: return
        if (!info.isPublishButtonEnabled()) return
        requestWithLoading(
            apiBlock = { mRepo.getWorkPublishStatus(workId) },
            onSuccess = {
                val status = data
                if (status == null) {
                    R.string.tip_publish_no_episode_available.getResString().showToast()
                } else if (!status.canApply()) {
                    val message = status.reasonMsg?.takeIf { it.isNotBlank() }
                        ?: R.string.tip_publish_no_episode_available.getResString()
                    message.showToast()
                } else if (status.needLegalInfo()) {
                    openPublishPage(info.workContractPublishPageUrl)
                } else if (!status.hasValidEpisodeRange()) {
                    R.string.tip_publish_no_episode_available.getResString().showToast()
                } else {
                    confirmNativePublish(status)
                }
            }
        )
    }

    fun submitWorkPublish(status: WorkPublishStatusData) {
        if (!status.hasValidEpisodeRange()) {
            R.string.tip_publish_no_episode_available.getResString().showToast()
            return
        }
        requestWithLoading(
            apiBlock = {
                mRepo.submitWorkPublish(
                    workId = workId,
                    auditBatchId = status.auditBatchId,
                    startEpisode = status.startEpisode,
                    endEpisode = status.endEpisode
                )
            },
            onSuccess = {
                R.string.tip_application_submit_success.getResString().showToast()
                FlowEventBus.postSync(Events.WorkPublishChangedEvent(workId))
            }
        )
    }

    /**
     * @param contractLevel 签约评级（S/A/B/C等）
     */
    @DrawableRes
    fun workLevelIcon(contractLevel: String?) = when (contractLevel) {
        /**[枚举]签约等级(0-普通 1-专业 2-金牌)*/
        "S" -> R.drawable.icon_work_level_s
        "A" -> R.drawable.icon_work_level_a
        "B" -> R.drawable.icon_work_level_b
        "C" -> R.drawable.icon_work_level_c
        else -> 0
    }
}

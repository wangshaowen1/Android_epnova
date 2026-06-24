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
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorCoverBadgeType
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorEpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import com.kuaigeglobal.epnova.core.network.apis.request.DeleteWorkBody
import com.kuaigeglobal.epnova.core.network.apis.request.LatestEpisodeBody
import com.kuaigeglobal.epnova.core.network.apis.response.WorkPublishStatusData
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.creatorplay.mapper.LegacyCreatorBridgeMapper
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
    val workInfo by lazy { MutableLiveData<CreatorWorkInfo?>() }

    //创建续集信息
    val episodeInfo = MutableLiveData<CreatorEpisodeInfo?>()
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
                    it.publishPlatformImageList?.map { ItemStringVm(it) }
                        .let { publishedPlatformAdapter.submitList(it) }
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

    fun episodeList(): Flow<PagingData<CreatorEpisodeInfo>> {
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
                episodeInfo.value = LegacyCreatorBridgeMapper.toCreatorEpisodeInfo(data, workId)
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

    fun publishFailed(workInfo: CreatorWorkInfo?) = workInfo?.isPublishReviewFailed() == true

    @ColorRes
    fun publishStatusColor(workInfo: CreatorWorkInfo?) = when (workInfo?.coverBadgeType) {
        CreatorCoverBadgeType.IN_REVIEW -> R.color.func_warning_dark
        CreatorCoverBadgeType.REVIEW_FAILED -> R.color.func_failure_dark
        CreatorCoverBadgeType.PUBLISHED -> R.color.func_success_dark
        else -> R.color.text_color_3_dark
    }

    @DrawableRes
    fun publishBadgeBackground(workInfo: CreatorWorkInfo?) = when(workInfo?.coverBadgeType) {
        CreatorCoverBadgeType.IN_REVIEW -> R.drawable.bg_func_warning_r27
        CreatorCoverBadgeType.REVIEW_FAILED -> R.drawable.bg_func_failure_r27
        CreatorCoverBadgeType.PUBLISHED -> R.drawable.bg_func_success_r27
        CreatorCoverBadgeType.UNPUBLISHED -> R.drawable.bg_func_normal_r27
        else -> R.drawable.bg_func_draft_r27
    }

    fun publishStatusText(workInfo: CreatorWorkInfo?) = workInfo?.publishSeriesStatusText().orEmpty()

    fun showPublishStatus(workInfo: CreatorWorkInfo?) = !workInfo?.publishBadgeText().isNullOrEmpty()

    fun showPublishButton(workInfo: CreatorWorkInfo?) = workInfo?.showPublishButton() == true

    fun enablePublishButton(workInfo: CreatorWorkInfo?) = workInfo?.isPublishButtonEnabled() == true

    fun publishButtonText(workInfo: CreatorWorkInfo?) = R.string.drama_detail_publish.getResString()

    fun queryWorkPublishStatus(
        openPublishPage: (String?) -> Unit,
        confirmNativePublish: (WorkPublishStatusData) -> Unit,
        confirmRejectionPublish: (WorkPublishStatusData) -> Unit
    ) {
        val info = workInfo.value ?: return
        if (!info.isPublishButtonEnabled()) return
        requestWithLoading(
            apiBlock = { mRepo.getWorkPublishStatus(workId) },
            onSuccess = {
                val status = data
                if (status == null) {
                    R.string.tip_publish_no_episode_available.getResString().showToast()
                } else {
                    // 作品发布流程
                    if (status.shouldOpenH5()) {
                        openPublishPage(info.workContractPublishPageUrl)
                    } else {
                        if (status.canApply()) {
                            if (status.exisRejectionEpisode()) {
                                confirmRejectionPublish(status)
                            } else {
                                confirmNativePublish(status)
                            }
                        } else {
                            val message = status.reasonMsg?.takeIf { it.isNotBlank() }
                                ?: R.string.tip_publish_no_episode_available.getResString()
                            message.showToast()
                        }
                    }
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

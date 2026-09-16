package com.kuaigeglobal.epnova.feature.settings.feedback

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.request
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.FeedbackPageInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.KeyValueInfo
import com.kuaigeglobal.epnova.core.network.apis.request.FeedbackBody
import com.kuaigeglobal.epnova.core.network.apis.request.FeedbackClearNoticeBody
import com.kuaigeglobal.epnova.core.network.apis.request.FeedbackDetailBody
import com.kuaigeglobal.epnova.core.network.apis.request.ImgUrlBody
import com.kuaigeglobal.epnova.core.network.apis.response.FeedbackDetailData
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.kuaigeglobal.epnova.common.Constant
import com.kuaigeglobal.epnova.common.utils.DateUtil
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2024/7/19 星期五 19:44
 * @Version: 1.0
 * *****************************************
 */

class FeedbackViewModel : AppRepositoryViewModel<AppRepository>() {
    companion object {
        const val STATUS_IN_PROGRESS = "In Progress"
        const val STATUS_REPLIED = "Replied"
    }

    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.profile_feedback_suggestions.getResString()
    )

    var mToolbarVMHistory = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.feedback_history.getResString()
    )

    var mToolbarVMDetail = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.my_feedback.getResString()
    )

    val email = MutableLiveData<String>()
    val isUnprocessed = MutableLiveData<Boolean>()
    val photoSize = MutableLiveData<Int>(0)
    val type = ObservableField<KeyValueInfo?>()
    val typeNames by lazy { Config.get().appConfig.userFeedBackTypeList }
    override fun onCreate() {
        super.onCreate()
        photoSize.value = 0
    }

    fun feedback(content: String?, images: List<String>?) {
        setLoading(true)
        if (images.isNullOrEmpty()) {
            submit(content, null)
            return
        }
        val ossClient = OssClient.newInstance(App.instance)
        ossClient.uploadFile(images, OssClient.TYPE_NORMAL, object : OnUploadListener() {
            override fun onUploadSuccess(uploadSuccess: List<String?>) {
                val images = uploadSuccess.filterNotNull()
                if (images.isEmpty()) {
                    onUploadFail(null)
                } else {
                    submit(content, images)
                }
            }

            override fun onUploadFail(uploadFail: MutableList<String>?) {
                super.onUploadFail(uploadFail)
                showToast(R.string.toast_img_upload_fail)
                setLoading(false)
            }
        })
    }

    private fun submit(content: String?, images: List<String>?) {
        launch(apiBlock@{
            images?.forEach { image ->
                val sensitiveCheckRes = withContext(Dispatchers.IO) { ApiFactory.apiService.sensitiveImageCheck(ImgUrlBody(image)) }
                val sensitiveCheckData = sensitiveCheckRes.data
                if (sensitiveCheckRes.isSuccess && sensitiveCheckData != null) {
                    if (sensitiveCheckData.isSensitive()) {
                        sensitiveCheckData.content?.showToast()
                        setLoading(false)
                        return@apiBlock
                    }
                } else {
                    (sensitiveCheckRes.msg ?: R.string.image_validation_failed.getResString()).showToast()
                    setLoading(false)
                    return@apiBlock
                }
            }
            val body = FeedbackBody(type.get()?.key, content, images?.joinToString(separator = ","), email.value)
            val response = withContext(Dispatchers.IO) { mRepo.api.feedback(body) }
            setLoading(false)
            if (response.isSuccess) {
                showToast(R.string.feedback_success)
                finish()
            } else {
                response.msg.showToast()
            }
        }, {
            setLoading(false)
        })
    }

    private val feedbackHistoryRefresh = MutableStateFlow(0)
    private val feedbackNoticeCleared = AtomicBoolean(false)
    val feedbackHistory: Flow<PagingData<FeedbackPageInfo>> = feedbackHistoryRefresh
        .flatMapLatest {
            Pager(
                config = PagingConfig(
                    initialLoadSize = Constant.Paged.PAGE_SIZE,
                    pageSize = Constant.Paged.PAGE_SIZE,
                    prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
                ),
                pagingSourceFactory = {
                    FeedbackHistoryPagingSource(
                        api = mRepo.api,
                        onNoticeSnapshotToken = ::clearFeedbackNoticeIfNeeded
                    )
                }
            ).flow
        }
        .cachedIn(viewModelScope)

    val feedbackDetail = ObservableField<FeedbackDetailData?>()


    fun refreshFeedbackHistory() {
        feedbackHistoryRefresh.value += 1
    }

    private fun clearFeedbackNoticeIfNeeded(noticeSnapshotToken: String?) {
        val token = noticeSnapshotToken?.takeIf(String::isNotBlank) ?: return
        if (!feedbackNoticeCleared.compareAndSet(false, true)) return
        request(
            apiBlock = {
                mRepo.api.clearFeedbackNotice(FeedbackClearNoticeBody(token))
            },
            showToast = false,
            onSuccess = {}
        )
    }

    /** 使用反馈历史条目携带的雪花业务 ID 查询详情。 */
    fun loadFeedbackDetail(feedbackId: String?) {
        if (feedbackId.isNullOrBlank()) {
            feedbackDetail.set(null)
            return
        }
        request(
            apiBlock = { mRepo.api.feedbackDetail(FeedbackDetailBody(feedbackId)) },
            showToast = true,
            onError = { feedbackDetail.set(null) }
        ) {
            feedbackDetail.set(data)
        }
    }

    fun feedbackTypeText(problemType: Int?): String {
        return typeNames?.firstOrNull { it.key == problemType }?.value
            ?: problemType?.toString().orEmpty()
    }

    /** 接口未定义状态码文案映射，按是否存在当前回复展示页面状态。 */
    fun feedbackStatusText(detail: FeedbackDetailData?): String {
        return if (hasReply(detail)) STATUS_REPLIED else STATUS_IN_PROGRESS
    }

    fun statusIconRes(detail: FeedbackDetailData?): Int {
        return if (hasReply(detail)) R.drawable.ic_replied else R.drawable.ic_feedback_in_progress
    }

    fun statusColorRes(detail: FeedbackDetailData?): Int {
        return if (hasReply(detail)) R.color.fixed_white_1 else R.color.fixed_white_1
    }

    fun feedbackSubmittedTime(createTime: String?): String {
        return formatFeedbackTime(createTime, DateUtil.FORMAT_DATETIME2)
    }

    fun feedbackReplyTime(replyTime: String?): String {
        return formatFeedbackTime(replyTime, DateUtil.FORMAT_EN_DAY)
    }

    fun feedbackReplyText(detail: FeedbackDetailData?): String {
        return detail?.replyContent?.content.orEmpty()
    }

    fun hasFeedbackImages(detail: FeedbackDetailData?): Boolean =
        !detail?.imagesUrls.isNullOrEmpty()

    fun hasReplyImages(detail: FeedbackDetailData?): Boolean =
        !detail?.replyContent?.imagesUrls.isNullOrEmpty()

    fun hasReply(detail: FeedbackDetailData?): Boolean = detail?.replyContent != null

    private fun formatFeedbackTime(value: String?, format: String): String {
        val rawValue = value?.trim().orEmpty()
        return rawValue.toLongOrNull()?.let { DateUtil.format(it, format) } ?: rawValue
    }
}

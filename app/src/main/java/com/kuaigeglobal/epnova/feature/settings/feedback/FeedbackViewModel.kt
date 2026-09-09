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
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.KeyValueInfo
import com.kuaigeglobal.epnova.core.network.apis.request.FeedbackBody
import com.kuaigeglobal.epnova.core.network.apis.request.ImgUrlBody
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.kuaigeglobal.epnova.common.Constant
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext

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
        const val STATUS_COMPLETED = "Completed"

        private const val DEMO_PACKAGE = "com.kuaigeglobal.epnova"
        private val DEMO_IMAGES = listOf(
            "android.resource://$DEMO_PACKAGE/drawable/img_default_cover_1_1",
            "android.resource://$DEMO_PACKAGE/drawable/img_default_cover_3_4",
            "android.resource://$DEMO_PACKAGE/drawable/img_default_cover_4_3",
            "android.resource://$DEMO_PACKAGE/drawable/img_default_cover_16_9"
        )
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
    val feedbackHistory: Flow<PagingData<FeedbackHistoryItem>> = feedbackHistoryRefresh
        .flatMapLatest {
            Pager(
                config = PagingConfig(
                    initialLoadSize = Constant.Paged.PAGE_SIZE,
                    pageSize = Constant.Paged.PAGE_SIZE,
                    prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
                ),
                pagingSourceFactory = {
                    FeedbackHistoryPagingSource(
                        demoHistory.sortedByDescending(FeedbackHistoryItem::submittedAtMillis)
                    )
                }
            ).flow
        }
        .cachedIn(viewModelScope)

    val feedbackDetail = ObservableField<FeedbackDetail?>()


    fun refreshFeedbackHistory() {
        feedbackHistoryRefresh.value += 1
    }

    fun loadFeedbackDetail(id: String?) {
        feedbackDetail.set(demoDetails.firstOrNull { it.id == id } ?: demoDetails.first())
    }

    fun statusIconRes(status: String?): Int {
        return when (status) {
            STATUS_IN_PROGRESS -> R.drawable.ic_feedback_in_progress
            STATUS_COMPLETED -> R.drawable.ic_feedback_completed
            else -> R.drawable.ic_replied
        }
    }

    fun statusColorRes(status: String?): Int {
        return when (status) {
            STATUS_IN_PROGRESS -> R.color.color_ea9800
            STATUS_COMPLETED -> R.color.color_00cf18
            else -> R.color.fixed_white_1
        }
    }

    fun hasReply(detail: FeedbackDetail?): Boolean = !detail?.reply.isNullOrBlank()

    // 临时假数据，仅用于按 Figma 验证列表、空状态和详情状态；接入接口后替换这里的数据源。
    private val demoHistory = listOf(
        FeedbackHistoryItem(
            id = "feedback-001",
            type = "Other Issues",
            submittedOn = "July 23, 2026",
            description = "Can I make original video make upload to reels or TikTok to make Money……",
            status = STATUS_REPLIED,
            submittedAtMillis = 3L
        ),
        FeedbackHistoryItem(
            id = "feedback-002",
            type = "Other Issues",
            submittedOn = "July 23, 2026",
            description = "Can I make original video make upload to reels or TikTok to make Money……",
            status = STATUS_IN_PROGRESS,
            submittedAtMillis = 2L,
            imageUrls = DEMO_IMAGES.take(2)
        ),
        FeedbackHistoryItem(
            id = "feedback-003",
            type = "Other Issues",
            submittedOn = "July 23, 2026",
            description = "Can I make original video make upload to reels or TikTok to make Money……",
            status = STATUS_COMPLETED,
            submittedAtMillis = 1L,
            imageUrls = DEMO_IMAGES.take(2)
        )
    )

    private val demoDetails = listOf(
        FeedbackDetail(
            id = "feedback-001",
            type = "Other Issues",
            submittedOn = "2026/12/02 12:31:12",
            description = "Can I make original video make upload to reels or TikTok to make Money……",
            imageUrls = DEMO_IMAGES,
            status = STATUS_REPLIED,
            reply = "Can I make original video make upload to reels or TikTok to make Money Can I make original video make upload to reels or TikTok to make Money Can I make original video make upload to reels or TikTok to make Money Can I make original video make upload to reels or TikTok to make Money",
            replyOn = "Sep 3, 2026",
            replyImageUrls = DEMO_IMAGES
        ),
        FeedbackDetail(
            id = "feedback-002",
            type = "Other Issues",
            submittedOn = "2026/12/02 12:31:12",
            description = "Can I make original video make upload to reels or TikTok to make Money……",
            imageUrls = DEMO_IMAGES.take(2),
            status = STATUS_IN_PROGRESS
        ),
        FeedbackDetail(
            id = "feedback-003",
            type = "Other Issues",
            submittedOn = "2026/12/02 12:31:12",
            description = "Can I make original video make upload to reels or TikTok to make Money……",
            imageUrls = DEMO_IMAGES.take(2),
            status = STATUS_COMPLETED,
            reply = "Thanks for helping us improve EpNova. We have completed the review of your feedback.",
            replyOn = "Sep 3, 2026",
            replyImageUrls = DEMO_IMAGES.take(2)
        )
    )
}

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
 * 项目名称: Epnova
 * @Author: SZYMN
 * @Introduce: 提供反馈提交、历史分页、详情数据及对应的页面展示状态。
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

    /** 联系邮箱，与反馈表单输入双向绑定。 */
    val email = MutableLiveData<String>()
    /** 历史入口的未读提醒红点，由打开反馈页时传入的参数初始化。 */
    val isUnprocessed = MutableLiveData<Boolean>()
    val photoSize = MutableLiveData<Int>(0)
    val type = ObservableField<KeyValueInfo?>()
    /** 反馈类型来自应用配置，页面展示 value，提交时使用所选类型的 key。 */
    val typeNames by lazy { Config.get().appConfig.userFeedBackTypeList }
    override fun onCreate() {
        super.onCreate()
        photoSize.value = 0
    }

    /**
     * 提交反馈：无图时直接进入提交流程，有图时先上传，再使用返回的 URL 继续提交。
     * @param images 用户选择的本地图片路径，尚未转换为服务器 URL。
     */
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
                // 上传回调没有返回可用 URL 时按失败处理，避免继续提交空图片结果。
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

    /** 逐张审核上传后的图片 URL，遇到敏感图片或审核失败时终止本次提交。 */
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
            // 保存反馈时将图片 URL 列表转为接口接收的逗号分隔字符串。
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

    // 递增值驱动 flatMapLatest 重建 Pager，主动刷新时切换到新的分页流。
    private val feedbackHistoryRefresh = MutableStateFlow(0)
    // 标记本实例是否已发起通知清除请求；请求失败也不重置，后续刷新不会重复请求。
    private val feedbackNoticeCleared = AtomicBoolean(false)
    /** 历史记录分页流，分页数据缓存在当前 ViewModel 生命周期内。 */
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

    /** 当前详情同时供 XML 文本绑定和 Activity 图片观察回调使用。 */
    val feedbackDetail = ObservableField<FeedbackDetailData?>()


    /** 重新创建分页流，例如从详情页返回历史列表时获取最新记录。 */
    fun refreshFeedbackHistory() {
        feedbackHistoryRefresh.value += 1
    }

    /** 收到首个非空白快照令牌后尝试清除通知，每个 ViewModel 实例最多发起一次。 */
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

    /** 优先展示配置中的类型名称，未知类型回退为编号，空类型显示空文本。 */
    fun feedbackTypeText(problemType: Int?): String {
        return typeNames?.firstOrNull { it.key == problemType }?.value
            ?: problemType?.toString().orEmpty()
    }

    /** 详情页按当前回复对象是否存在展示状态文案，与状态图标使用相同判断。 */
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

    /** 回复对象存在即视为已回复，也支持只有图片而没有正文的回复。 */
    fun hasReply(detail: FeedbackDetailData?): Boolean = detail?.replyContent != null

    /** 数字字符串按毫秒时间戳格式化，其余内容保留原文，兼容已格式化的时间。 */
    private fun formatFeedbackTime(value: String?, format: String): String {
        val rawValue = value?.trim().orEmpty()
        return rawValue.toLongOrNull()?.let { DateUtil.format(it, format) } ?: rawValue
    }
}

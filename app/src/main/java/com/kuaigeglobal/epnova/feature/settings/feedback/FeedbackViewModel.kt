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
import com.kuaigeglobal.epnova.feature.message.NotificationItem
import com.kuaigeglobal.epnova.feature.message.NotificationsPagingSource
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
    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.profile_feedback_suggestions.getResString()
    )

    var mToolbarVMHistory = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.feedback_history.getResString()
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

    private val feedbackHistoryItems = MutableStateFlow<List<FeedbackHistoryItem>>(emptyList())
    val feedbackHistory: Flow<PagingData<FeedbackHistoryItem>> = feedbackHistoryItems
        .flatMapLatest { items ->
            Pager(
                config = PagingConfig(
                    initialLoadSize = Constant.Paged.PAGE_SIZE,
                    pageSize = Constant.Paged.PAGE_SIZE,
                    prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
                ),
                pagingSourceFactory = { FeedbackHistoryPagingSource(items) }
            ).flow
        }
        .cachedIn(viewModelScope)
}
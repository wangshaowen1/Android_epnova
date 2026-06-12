package com.kuaigeglobal.epnova.feature.creator

import android.graphics.Color
import android.util.Log
import androidx.annotation.ColorInt
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.utils.TextEncodingUtil
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.parseMessage
import com.kuaigeglobal.epnova.core.ext.requestWithLoading
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiResponse
import com.kuaigeglobal.epnova.core.network.apis.entity.InspirationInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoConfigData
import com.kuaigeglobal.epnova.core.network.apis.request.CreateTaskBody
import com.kuaigeglobal.epnova.core.network.apis.request.SSEContentBody
import com.kuaigeglobal.epnova.core.network.apis.request.UploadStoryBody
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkTaskData
import com.kuaigeglobal.epnova.core.network.apis.response.FirstRechargeCreditsDialogData
import com.kuaigeglobal.epnova.core.network.sse.SSEStreamHandler
import com.kuaigeglobal.epnova.core.network.sse.processor.PlainTextSseProcessor
import com.kuaigeglobal.epnova.feature.creator.CreationFragment.Companion.KEY_MODEL
import com.kuaigeglobal.epnova.feature.creator.CreationFragment.Companion.MODEL_INSPIRATION
import com.kuaigeglobal.epnova.feature.creator.data.CreationRepository
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.recharge.PowerRechargeDialog
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.getThemeColor
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.launch

class CreationViewModel : AppRepositoryViewModel<CreationRepository>() {
    val input by lazy { MutableLiveData<String?>() }
    var fileName: String? = null
    var fileUrl: String? = null
    var analysisResult: TextEncodingUtil.AnalysisResult? = null
    val minInputLength = 10
    // 海外统一20000
    val maxInputLength by lazy {
        ObservableInt(20000)
    }
    val promptGenerating = MutableLiveData<Pair<Boolean, String>>()

    val hint = ObservableField<String>()
    val creationModel by lazy {
        mBundle.getInt(KEY_MODEL)
    }

    // 缓存当前批次尚未展示的数据池
    private val unshownInspirations = mutableListOf<InspirationInfo>()
    val inspirationPrompts by lazy { MutableLiveData<List<InspirationInfo>>() }
    val videoSetConfig by lazy { MutableLiveData<VideoConfigData>() }
    val firstRechargeCache = MutableLiveData<FirstRechargeCreditsDialogData>()

    fun inspirationModel(): Boolean {
        return creationModel == MODEL_INSPIRATION
    }

    override fun onCreate() {
        super.onCreate()
        hint.set(if (inspirationModel()) R.string.story_inspiration_hint.getResString() else R.string.paste_novel_hint.getResString())
//        maxInputLength.set(if (inspirationModel()) 1000 else 5000)
        input.value =
            if (inspirationModel()) Global.inspirationContent else Global.customizedContent
        if (inspirationModel()) {
            randomInspirations()
        }
        videoSetConfig {

        }
    }

    @ColorInt
    fun getLimitColor(length: Int?): Int {
        val isMax = length == maxInputLength.get()
        if (isMax) {
            showToast(R.string.input_limit_reached)
        }
        val attr =
            if (isMax) R.attr.funcColorFailure else R.attr.textColor4
        return topActivity?.getThemeColor(attr) ?: Color.BLACK
    }

    private var sseHandler: SSEStreamHandler? = null

    fun isStreaming(): Boolean {
        return sseHandler?.isStreaming?.value ?: false
    }

    fun sseGenerate(bizType: Int? = null, promptGeneratingContent: String?) {
        promptGenerating.value = true to (promptGeneratingContent ?: "")
        val sseFlow =
            if (bizType == SSEContentBody.TYPE_RANDOM_INSPIRATION) mRepo.api.sseContentRandom(
                UserTokenBody()
            ) else mRepo.api.sseContentOptimize(
                SSEContentBody(input.value, bizType)
            )
        // 2. 初始化处理器 (如果还没初始化，或者想每次重建也可以)
        sseHandler?.cancel()
        sseHandler = SSEStreamHandler(
            scope = viewModelScope, // 传入协程作用域
            processor = PlainTextSseProcessor(
                scope = viewModelScope,
                onStart = {
                    Logger.d("[SSE]", "Open")
                    input.value = ""
                },
                onUpdate = { fullContent ->
                    if (bizType == SSEContentBody.TYPE_RANDOM_INSPIRATION) {
                        if (fullContent.contains("%")) {
                            promptGenerating.value = false to ""
                            // 提取 % 之后的所有内容
                            val displayContent = fullContent.substringAfter("%")
                            input.value = displayContent
                        }
                    } else {
                        promptGenerating.value = false to ""
                        input.value = fullContent
                    }
                },
                onComplete = {
                    Logger.d("[SSE]", "Finished")
                    // 触发原本结束后的逻辑
                    promptGenerating.value = false to ""
                },
                onErrorCallback = { e ->
                    Log.e("[SSE]", "Error: $e")
                    // 根据需要处理特定错误
                    parseMessage(e)?.showToast()
                    promptGenerating.value = false to ""
                })
        )
        // 3. 启动
        Logger.d("[SSE]", "Starting with content body = $input")
        sseHandler?.start(sseFlow)
    }

    /**
     * 页面销毁或需要重置时调用
     */
    override fun onCleared() {
        super.onCleared()
        sseHandler?.cancel()
    }

    fun videoSetConfig(showLoading: Boolean = false, function: (() -> Unit)? = null) {
        if (videoSetConfig.value != null) {
            function?.invoke()
            return
        }
        requestWithLoading(
            apiBlock = { mRepo.videoSetConfig() },
            showLoading = showLoading
        ) {
            videoSetConfig.value = data
            function?.invoke()
        }
    }

    fun randomInspirations(showLoading: Boolean = false) {
        if (unshownInspirations.isNotEmpty()) {
            displayNextBatch()
            return
        }
        requestWithLoading(
            apiBlock = { mRepo.getInspirationQuickList() },
            showLoading = showLoading
        ) {
            // 判空并处理新数据
            data?.let { newData ->
                unshownInspirations.clear() // 保底清理
                // 将新拿到的一批数据打乱顺序后放入缓存池，这样后续按顺序取相当于随机抽取
                unshownInspirations.addAll(newData.shuffled())

                // 数据准备完毕，展示这一批的前5个
                displayNextBatch()
            }
        }
    }

    private fun displayNextBatch() {
        // 每次取最多 4 个。如果末尾剩余不足 4 个，就全部取出展示
        val countToTake = minOf(4, unshownInspirations.size)

        // 复制出要展示的数据列表
        val batchToShow = unshownInspirations.take(countToTake).toList()

        // 从缓存池中剔除已经取出的数据 (使用 subList.clear() 比 removeAll 更安全且性能更好)
        unshownInspirations.subList(0, countToTake).clear()

        // 更新 LiveData 通知 UI 渲染
        inspirationPrompts.value = batchToShow
    }

    fun createTask(modelType: Int, body: CreateTaskBody, callback: (WorkTaskData?) -> Unit) {
        requestWithLoading(
            apiBlock = {
                when (modelType) {
                    DramaGenerateConstants.MODEL_STORY_SINGLE -> {
                        mRepo.api.createTaskFromSingleEpisode(body)
                    }

                    DramaGenerateConstants.MODEL_INSPIRATION -> {
                        mRepo.api.createSummaryTaskFromInspiration(body)
                    }

                    DramaGenerateConstants.MODEL_STORY -> {
                        mRepo.api.createTaskFromNovel(body)
                    }

                    else -> null
                }
            },
            onSuccess = { callback.invoke(data) }
        )
    }

    fun confirmPowerConsume(body: UploadStoryBody, callback: (WorkTaskData?) -> Unit) {
        requestWithLoading(
            apiBlock = {
                mRepo.api.novelUploadSummary(body)
            },
            onSuccess = {
                Global.refreshCoinsAndPoints()
                callback.invoke(data)
            },
            onError = {
                if (it is ApiException && it.errorCode == ApiResponse.RESPONSE_CODE_POWER_NOT_ENOUGH) {
                    viewModelScope.launch {
                        //这里和VideoSetDialog共用首充礼包弹出场景，前面会弹，这就不需要弹了，直接弹出充值弹出
                        PowerRechargeDialog.show(showToast = true, rechargeTab = PowerRechargeDialog.TAB_CREDITS)
                    }
                } else {
                    it?.message?.showToast()
                }
            }
        )
    }

    fun extractStoryName(input: String?): String? {
        if (input.isNullOrBlank()) return null
        // 兼容中英文标题标签和常见包裹符号；无包裹符号时取标题行剩余内容。
        val titleLabel = """(?:故事名称|故事标题|作品名称|作品标题|Story\s*Title|Title)"""
        val regex = """$titleLabel\s*[:：]\s*([^\r\n]+)""".toRegex(RegexOption.IGNORE_CASE)
        val title = regex.find(input)?.groups?.get(1)?.value?.trim() ?: return null

        return title.removeSurrounding("《", "》")
            .removeSurrounding("“", "”")
            .removeSurrounding("\"", "\"")
            .removeSurrounding("'", "'")
            .removePrefix("//")
            .removeSuffix("//")
            .trim()
            .takeIf { it.isNotEmpty() }
    }

    fun getFirstRechargeInfo(callback: () -> Unit) {
        launch {
            // 2. 请求首充接口 (这里必须先于活动配置完成)
            val firstRechargeResponse = mRepo.api.getFirstRechargeCreditsDialogInfo()
            if (firstRechargeResponse.data != null && firstRechargeResponse.data?.isNeedShow() == true) {
                firstRechargeCache.value = firstRechargeResponse.data
            } else {
                callback.invoke()
            }
        }
    }
}

package com.kuaigeglobal.epnova.feature.creator

import android.graphics.Color
import android.util.Log
import androidx.annotation.ColorInt
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.utils.TextEncodingUtil
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.ext.handlePointInsufficientError
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.parseMessage
import com.kuaigeglobal.epnova.core.ext.request
import com.kuaigeglobal.epnova.core.ext.requestWithLoading
import com.kuaigeglobal.epnova.core.ext.showPointInsufficientGuide
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.entity.InspirationInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SameStylePrefillInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.UnusedPackageInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoConfigData
import com.kuaigeglobal.epnova.core.network.apis.request.CreateTaskBody
import com.kuaigeglobal.epnova.core.network.apis.request.CreationScene
import com.kuaigeglobal.epnova.core.network.apis.request.FirstRechargeCreditsBody
import com.kuaigeglobal.epnova.core.network.apis.request.InsufficientGuideBody
import com.kuaigeglobal.epnova.core.network.apis.request.SSEContentBody
import com.kuaigeglobal.epnova.core.network.apis.request.UploadStoryBody
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkTaskData
import com.kuaigeglobal.epnova.core.network.apis.response.FirstRechargeCreditsDialogData
import com.kuaigeglobal.epnova.core.network.apis.response.PointInsufficientGuideData
import com.kuaigeglobal.epnova.core.network.sse.SSEStreamHandler
import com.kuaigeglobal.epnova.core.network.sse.processor.PlainTextSseProcessor
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.creation.CreationFragment.Companion.KEY_MODEL
import com.kuaigeglobal.epnova.feature.creation.CreationFragment.Companion.MODEL_INSPIRATION
import com.kuaigeglobal.epnova.feature.creator.data.CreationRepository
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.creator.hotcase.CreationHotCaseInfo
import com.kuaigeglobal.epnova.feature.creator.hotcase.CreationHotCasePagingSource
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.getThemeColor
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.flow.Flow

class CreationViewModel : AppRepositoryViewModel<CreationRepository>() {

    companion object {
        const val CONTENT_LENGTH = 20000
    }

    val input by lazy { MutableLiveData<String?>() }
    var fileName: String? = null
    var fileUrl: String? = null
    var analysisResult: TextEncodingUtil.AnalysisResult? = null
    val minInputLength = 10
    // 海外统一20000
    val maxInputLength by lazy {
        ObservableInt(CONTENT_LENGTH)
    }
    val promptGenerating = MutableLiveData<Pair<Boolean, String>>()
    val workCreating = MutableLiveData(false)

    val showRefreshInspirations = ObservableBoolean(true)
    val showInspirationsExample = ObservableBoolean(false)

    val hint = ObservableField<String>()
    val creationModel by lazy {
        mBundle.getInt(KEY_MODEL)
    }

    // 缓存当前批次尚未展示的数据池
    private val unshownInspirations = mutableListOf<InspirationInfo>()
    val inspirationPrompts by lazy { MutableLiveData<List<InspirationInfo>>() }
    val videoSetConfig by lazy { MutableLiveData<VideoConfigData>() }
    val firstRechargeCache = MutableLiveData<FirstRechargeCreditsDialogData>()
    private var selectedInspirationReportId: String? = null
    private var shouldShowInputFromStart = false

    var currentUseInspirationInfo: InspirationInfo? = null
        private set

    val creationHotCaseFlow: Flow<PagingData<CreationHotCaseInfo>> = Pager(
        config = PagingConfig(
            pageSize = 10,
            initialLoadSize = 10,
            prefetchDistance = 3,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { CreationHotCasePagingSource(mRepo.api) }
    ).flow.cachedIn(viewModelScope)

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

    fun enableCreationTabUi() {
        maxInputLength.set(CONTENT_LENGTH)
        hint.set(
            if (inspirationModel()) R.string.story_inspiration_hint.getResString()
            else R.string.paste_novel_hint.getResString()
        )
    }

    fun selectInspirationPrompt(prompt: InspirationInfo) {
        shouldShowInputFromStart = true
        input.value = prompt.inspirationContent
        selectedInspirationReportId = prompt.inspirationConfigId
        currentUseInspirationInfo = prompt
    }

    fun consumeShouldShowInputFromStart(): Boolean {
        return shouldShowInputFromStart.also { shouldShowInputFromStart = false }
    }

    fun updateInputFromUser(content: String?) {
        if (input.value != content) clearInspirationPromptUsage()
        input.value = content
    }

    fun clearInspirationPromptUsage() {
        selectedInspirationReportId = null
    }

    fun clearCurrentUseInspiration() {
        currentUseInspirationInfo = null
    }

    fun getInspirationReportIdForTask(): String? = selectedInspirationReportId

    fun reportSelectedInspirationUsageIfNeeded() {
        EventManager.reportCreationUsageCount(
            selectedInspirationReportId,
            EventManager.REPORT_CREATION_TYPE_INSPIRATION
        )
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
        if (bizType == SSEContentBody.TYPE_RANDOM_INSPIRATION ||
            bizType == SSEContentBody.TYPE_AI_OPTIMIZE_INSPIRATION
        ) {
            clearInspirationPromptUsage()
        }
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
    override fun onDestroy() {
        super.onDestroy()
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

    fun sameStylePrefill(hotCaseId: String?, callback: (SameStylePrefillInfo?) -> Unit) {
        requestWithLoading(
            apiBlock = { mRepo.sameStylePrefill(hotCaseId) },
            onSuccess = { callback(data) }
        )
    }

    fun getUnusedCreationPackages(callback: (List<UnusedPackageInfo>, String?) -> Unit) {
        request(
            apiBlock = { mRepo.api.unusedPackageList(UserTokenBody()) },
            onSuccess = { callback(data?.validPackages().orEmpty(), data?.displaySubtitle) },
            onError = { callback(emptyList(), null) }
        )
    }

    fun randomInspirations(showLoading: Boolean = false) {
        if (unshownInspirations.isNotEmpty()) {
            displayNextBatch(false)
            return
        }
        requestWithLoading(
            apiBlock = { mRepo.getInspirationQuickList() },
            showLoading = showLoading
        ) {
            // 判空并处理新数据
            data?.let { newData ->
                showInspirationsExample.set(newData.isNotEmpty())
                // 小于5条不展示刷新按钮
                showRefreshInspirations.set(newData.size >= 5)

                unshownInspirations.clear() // 保底清理
                // 将新拿到的一批数据打乱顺序后放入缓存池，这样后续按顺序取相当于随机抽取
                unshownInspirations.addAll(newData.shuffled())

                // 数据准备完毕，展示这一批的前5个
                displayNextBatch(true)
            }
        }
    }

    /**
     * 取灵感展示
     * @param flag 标记读远程数据还是缓存数据 true: 远程 false：缓存
     */
    private fun displayNextBatch(flag: Boolean = true) {
        val limits = 5
        // 每次取最多 5 个。如果末尾剩余不足 5 个，就全部取出展示
        val countToTake = minOf(limits, unshownInspirations.size)

        // 复制出要展示的数据列表
        val batchToShow = unshownInspirations.take(countToTake).toMutableList()

        // 从缓存池中剔除已经取出的数据 (使用 subList.clear() 比 removeAll 更安全且性能更好)
        unshownInspirations.subList(0, countToTake).clear()

        // 读缓存才触发这段逻辑，取缓存展示
        if (!flag) {
            if (batchToShow.size < limits) {
                val supplement = inspirationPrompts.value
                    .orEmpty()
                    .shuffled()
                    .take(limits - batchToShow.size)
                batchToShow.addAll(supplement)
            }
        }

        // 更新 LiveData 通知 UI 渲染
        inspirationPrompts.value = batchToShow
    }

    fun createTask(modelType: Int, body: CreateTaskBody, callback: (WorkTaskData?) -> Unit) {
        workCreating.value = true
        request(
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
            onError = { workCreating.value = false },
            onSuccess = {
                workCreating.value = false
                callback.invoke(data)
            }
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
                // 该流程前面已处理首充，guideType=0 时保持原逻辑，直接展示充值半屏。
                val handled = it is ApiException && handlePointInsufficientError(
                    creationScene = CreationScene.STORY_OUTLINE,
                    errorCode = it.errorCode,
                    allowFirstRechargeForLegacyGuide = false,
                )
                if (!handled) {
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
            val firstRechargeResponse = mRepo.api.getFirstRechargeCreditsDialogInfo(
                FirstRechargeCreditsBody(CreationScene.STORY_OUTLINE)
            )
            if (firstRechargeResponse.data != null && firstRechargeResponse.data?.isNeedShow() == true) {
                firstRechargeCache.value = firstRechargeResponse.data
            } else {
                callback.invoke()
            }
        }
    }

    /** 查询故事梗概场景的算力不足引导，并展示服务端指定的付费入口。 */
    fun pointInsufficientGuide(nextBloack: () -> Unit) {
        launch(showToast = true) {
            var guideType: Int? = null
            response(
                block = {
                    mRepo.api.pointInsufficientGuide(
                        InsufficientGuideBody(CreationScene.STORY_OUTLINE)
                    )
                },
                result = {
                    guideType = data?.guideType
                        ?: PointInsufficientGuideData.GUIDE_TYPE_LEGACY
                },
            )
            val resolvedGuideType = guideType ?: return@launch
            showPointInsufficientGuide(
                creationScene = CreationScene.STORY_OUTLINE,
                guideType = resolvedGuideType,
                nextBloack = nextBloack,
            )
        }
    }
}

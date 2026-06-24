package com.kuaigeglobal.epnova.feature.creator.storyboard

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.KeyboardUtils
import com.drake.spannable.replaceSpan
import com.drake.spannable.span.CenterImageSpan
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.showCommonDialog
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.utils.AutoProgressSimulator
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.ext.showRechargeDialog
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.ApiResponse
import com.kuaigeglobal.epnova.core.network.apis.entity.CreationModelInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleStoryboardInfo
import com.kuaigeglobal.epnova.core.network.apis.request.IdsBody
import com.kuaigeglobal.epnova.core.network.apis.request.StrategyTypeBody
import com.kuaigeglobal.epnova.core.network.apis.request.SwitchModelBody
import com.kuaigeglobal.epnova.core.network.apis.request.TaskIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkBody
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.creator.assets.ItemAddVM
import com.kuaigeglobal.epnova.feature.creator.assets.ItemAssetVM
import com.kuaigeglobal.epnova.feature.recharge.PowerRechargeDialog
import com.kuaigeglobal.epnova.widget.LoadStateViewHolder
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 分镜设计
 */
class StoryboardDesignViewModel : AppRepositoryViewModel<AppRepository>() {
    companion object {
        private const val POWER = "[power]"

        /**
         * 初始化
         */
        const val STEP_INIT = 0

        /**
         * 分镜描述生成中
         */
        private const val STEP_DESCRIPTION_GENERATING = 1

        /**
         * 分镜描述已生成
         */
        private const val STEP_DESCRIPTION_GENERATED = 2

        /**
         * 分镜描述生成失败
         */
        private const val STEP_DESCRIPTION_GENERATE_FAILED = 3
    }

    var workId: String? = null
    var storyId: String? = null
    var taskId: String? = null
        set(value) {
            field = value
            if (!value.isNullOrEmpty()) {
                step.set(STEP_DESCRIPTION_GENERATING)
            }
        }
    val loadState by lazy { ObservableInt(LoadStateViewHolder.CONTENT) }
    val adapter by lazy { StoryboardDesignAdapter() }
    private val addStoryboardItem by lazy { ItemAddVM(R.string.add_storyboard.getResString(), 16).apply { onItemClick.value = { addStoryboard(this) } } }
    private var editingStoryboardDescVM: ItemStoryboardVM? = null
    private var loopAllStoryboardDescriptionStatusJob: Job? = null
    private var inputJob: Job? = null
    val step by lazy { ObservableInt(STEP_INIT) }
    val cost by lazy { ObservableInt() }
    val creationModels = MutableLiveData<List<CreationModelInfo>>()
    val currentCreationModel = MutableLiveData<CreationModelInfo?>()
    /** key = strategyType（3-动态漫，5-解说漫），value = 单位价格；均在 Main 线程读写 */
    private val priceCache = mutableMapOf<Int, Int>()
    /** 正在进行中的价格请求，避免并发重复发起 */
    private val priceFetchJobs = mutableMapOf<Int, Job>()
    val keyboardHeight by lazy { ObservableInt() }
    var generating = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    progressHelper.start(R.string.extract_storyboard_info.getResString(), step = 1, intervalMs = 100L)
                } else {
                    progressHelper.stop()
                }
            }
        }
    private val progressHelper by lazy { AutoProgressSimulator(viewModelScope) { show, title, progress -> promptGenerating.set(show) } }
    val promptGenerating by lazy { ObservableBoolean() }
    private val powerSpan by lazy { CenterImageSpan(App.instance, R.drawable.icon_power).apply { setDrawableSize(16.dp, 16.dp) } }
    private var editConfirm = false

    override fun onResume() {
        super.onResume()
        topActivity?.let {
            KeyboardUtils.registerSoftInputChangedListener(it) { height ->
                keyboardHeight.set(height)
//                if (height <= 0) {
//                    editingStoryboardDescVM?.exitEditStoryboardDesc()
//                    editingStoryboardDescVM = null
//                }
            }
        }
    }

    override fun onPause() {
        topActivity?.window?.let { KeyboardUtils.unregisterSoftInputChangedListener(it) }
        super.onPause()
    }

    override fun onCleared() {
        loopAllStoryboardDescriptionStatusJob?.cancel()
        inputJob?.cancel()
        super.onCleared()
    }

    fun retry() {
        loadState.set(LoadStateViewHolder.CONTENT)
        when (step.get()) {
            STEP_INIT, STEP_DESCRIPTION_GENERATE_FAILED -> generateAllStoryboardDescription()
            STEP_DESCRIPTION_GENERATING -> loopAllStoryboardDescriptionStatus()
            STEP_DESCRIPTION_GENERATED -> getStoryboards()
        }
    }

    private fun generateAllStoryboardDescription() {
        if (!arrayOf(STEP_INIT, STEP_DESCRIPTION_GENERATE_FAILED).contains(step.get())) return
        generating = true
        launch(block = {
            val allStoryboardDescriptionStatus = withContext(Dispatchers.IO) { mRepo.api.generateAllStoryboardDescription(WorkBody(workId, storyId)).data }
            taskId = allStoryboardDescriptionStatus?.taskId
            if (allStoryboardDescriptionStatus?.genStatus == 1 || allStoryboardDescriptionStatus?.genStatus == 2) {
                adapter.submitList(emptyList())
                step.set(STEP_DESCRIPTION_GENERATING)
                loopAllStoryboardDescriptionStatus()
            } else {
                loadState.set(LoadStateViewHolder.RETRY)
                generating = false
            }
        }, onError = {
            loadState.set(LoadStateViewHolder.RETRY)
            generating = false
        })
    }

    private fun loopAllStoryboardDescriptionStatus() {
        loopAllStoryboardDescriptionStatusJob?.cancel()
        generating = true
        loopAllStoryboardDescriptionStatusJob = launch(block = {
            var genStatus: Int? = 1
            runCatching {
                val status = withContext(Dispatchers.IO) { mRepo.api.generateAllStoryboardDescriptionStatus(TaskIdBody(taskId, null)).data?.genStatus }
                genStatus = status
            }
            //生成状态 [0=待生成 1=生成中 2=成功 3=失败]
            when (genStatus) {
                1 -> {
                    delay(3000)
                    loopAllStoryboardDescriptionStatus()
                }

                2 -> {
                    step.set(STEP_DESCRIPTION_GENERATED)
                    getStoryboards()
                }

                else -> {
                    step.set(STEP_DESCRIPTION_GENERATE_FAILED)
                    loadState.set(LoadStateViewHolder.RETRY)
                    generating = false
                }
            }
        }, onError = {
            loadState.set(LoadStateViewHolder.RETRY)
            generating = false
        })
    }

    fun getStoryboards() {
        generating = true
        launch(
            block = {
                val storyboards = mRepo.api.workStoryboardList(WorkBody(workId, storyId)).data
                val items = mutableListOf<AppItemViewModel<*>>()
                storyboards?.map { it.toItemStoryboardVM() }?.let { items.addAll(it) }
                addOrRemoveStoryboardItem(items)
                adapter.items.forEach { if (it is ItemAssetVM) it.clear() }
                adapter.submitList(items)
                updateItemNo()
                loadState.set(LoadStateViewHolder.CONTENT)
                generating = false
            },
            onError = {
                loadState.set(LoadStateViewHolder.RETRY)
                generating = false
            }
        )
    }

    fun addOrRemoveStoryboardItem(items: MutableList<AppItemViewModel<*>>) {
        val isNarration = currentCreationModel.value?.configCode == Constants.Model.STORYBOARD_NARRATION
        val hasAddItem = items.contains(addStoryboardItem)
        if (isNarration && hasAddItem) {
            items.remove(addStoryboardItem)
        } else if (!isNarration && !hasAddItem) {
            items.add(addStoryboardItem)
        }
    }

    private fun SimpleStoryboardInfo.toItemStoryboardVM() = ItemStoryboardVM(workId, storyId, this).applyConfig()

    fun addStoryboard(addItem: ItemAddVM) {
        val index = adapter.items.indexOf(addItem)
        adapter.add(index, ItemStoryboardVM(workId, storyId, null).applyConfig())
        updateItemNo()
    }

    private fun updateItemNo() {
        var storyboardNum = 0
        adapter.items.forEachIndexed { index, item ->
            if (item is ItemStoryboardVM) {
                storyboardNum++
                item.no.set(storyboardNum)
            }
        }
        calcCost()
    }

    private fun ItemStoryboardVM.applyConfig(): ItemStoryboardVM {
        onItemClick.value = click@{ view ->
            when (view?.id) {
                R.id.etStoryboardDesc -> {
//                    if (editingStoryboardDescVM == this) return@click
//                    inputJob?.cancel()
//                    val edit: (delay: Long) -> Unit = { delay ->
//                        editingStoryboardDescVM?.exitEditStoryboardDesc()
//                        editingStoryboardDescVM = this
//                        if (delay > 0) {
//                            inputJob = launch {
//                                delay(delay)
//                                editStoryboardDesc()
//                            }
//                        } else {
//                            editStoryboardDesc()
//                        }
//                    }
                    if (storyBoardDesc.get().isNullOrEmpty() || editConfirm) {
                        editConfirm = true
                        editStoryboardDesc()
                    } else {
                        editConfirm = true
                        topActivity?.showCommonDialog(tip = R.string.edit_text_warning.getResString(), confirmText = R.string.continue_editing.getResString()) {
                            editStoryboardDesc()
                        }
                    }
                }

                R.id.btnDelete -> {
                    if (data.get() == null) {
                        adapter.remove(this)
                        updateItemNo()
                    } else {
                        topActivity?.showCommonDialog(title = R.string.confirm_deletion.getResString(), tip = R.string.delete_impact_warning.getResString(), confirmText = R.string.del.getResString()) {
                            deleteStoryboard(this)
                        }
                    }
                }
            }
        }
        return this
    }

    private fun deleteStoryboard(item: ItemStoryboardVM) {
        launchWithLoading {
            response(mRepo.api.workStoryboardDelete(IdsBody(null, item.data.get()?.storyboardId))) {
                adapter.remove(item)
                updateItemNo()
            }
        }
    }

    private fun calcCost() {
        cost.set(0)
        launch {
            val model = currentCreationModel.value
            val type = if (model?.isNarration() == true) 5 else 3
            if (!priceCache.containsKey(type)) {
                // 若已有同 type 的请求在途，直接等它完成，不重复发起
                val job = priceFetchJobs.getOrPut(type) {
                    launch {
                        val pointNum = withContext(Dispatchers.IO) {
                            mRepo.api.queryAiPrice(StrategyTypeBody(type, workId = workId, storyId = storyId)).data?.pointNum
                        }
                        // 写回 Main 线程，与读操作同线程，无并发安全问题
                        pointNum?.let { priceCache[type] = it }
                        priceFetchJobs.remove(type)
                    }
                }
                job.join()
            }
            val price = priceCache[type] ?: 0
            if (price <= 0) {
                showToast(R.string.request_failed_tips)
                return@launch
            }
            val totalDuration = adapter.items.sumOf { item ->
                if (item is ItemStoryboardVM) {
                    item.data.get()?.duration ?: 15
                } else {
                    0
                }
            }
            cost.set(price * totalDuration)
        }
    }

    private fun generateStoryboardVideo(toNextStep: ((workId: String?, storyId: String?, taskId: String?) -> Unit)? = null) {
        if (adapter.items.isEmpty()) {
            showToast(R.string.please_add_storyboard_first)
            return
        }
        val cost = cost.get()
        val type = if (currentCreationModel.value?.isNarration() == true) 5 else 3
        if (cost == 0 && !priceCache.containsKey(type)) {
            calcCost()
            return
        }
        generate(cost, toNextStep)
    }

    private fun generate(cost: Int, toNextStep: ((workId: String?, storyId: String?, taskId: String?) -> Unit)? = null) {
        if (PowerRechargeDialog.checkLowBalance(cost, PowerRechargeDialog.TAB_CREDITS)) return
        EventManager.onFirebaseTrack(FirebaseEvent.STORYBOARD_GENERATE_CLICK)
        launchWithLoading {
            adapter.items.forEach { if (it is ItemStoryboardVM && !it.save()) return@launchWithLoading }
            response(
                bean = mRepo.api.workStoryboardVideoGenerate(WorkBody(workId, storyId)),
                result = { toNextStep?.invoke(workId, storyId, data?.taskId) },
                error = {
                    if (this?.code == ApiResponse.RESPONSE_CODE_POWER_NOT_ENOUGH) {
                        viewModelScope.launch {
                            showRechargeDialog(true)
                        }
                    } else {
                        this?.msg?.showToast()
                    }
                })
        }
    }

    fun buttonEnable(step: Int): Boolean {
        return step == STEP_DESCRIPTION_GENERATED
    }

    fun buttonText(step: Int, cost: Int): CharSequence? {
        return when (step) {
            STEP_DESCRIPTION_GENERATED -> if (cost > 0) "${R.string.generate_storyboard_video.getResString()} $POWER $cost".replaceSpan(POWER) { powerSpan } else R.string.generate_storyboard_video.getResString()
            else -> null
        }
    }

    /**
     * 点击下一步
     */
    fun clickNext(toNextStep: ((workId: String?, storyId: String?, taskId: String?) -> Unit)?) {
        when (step.get()) {
            STEP_DESCRIPTION_GENERATED -> {
                //下一步：生成分镜图片
                generateStoryboardVideo(toNextStep)
            }
        }
    }

    fun notSaved(): Boolean {
        return adapter.items.any { it is ItemStoryboardVM && it.notSaved() }
    }

    fun saveAndFinish(callback: () -> Unit) {
        launchWithLoading {
            adapter.items.forEach { if (it is ItemStoryboardVM) it.save(false) }
            callback.invoke()
        }
    }

    suspend fun getModels(): Int {
        val response = withContext(Dispatchers.IO) { mRepo.api.getStoryboardModelConfig(WorkBody(workId, storyId)) }
        if (!response.isSuccess) {
            return 0
        }
        val data = response.data
        creationModels.value = data ?: emptyList()
        val selectedModel = data?.firstOrNull { it.isDefault() } ?: data?.firstOrNull()
        currentCreationModel.value = selectedModel
        return selectedModel?.configValue ?: 0
    }

    fun switchModel(model: CreationModelInfo?, callback: (() -> Unit)?) {
        launchWithLoading {
            response(mRepo.api.switchModel(SwitchModelBody(workId, storyId, model?.configCode))) {
                model?.let {
                    currentCreationModel.value = it
                    callback?.invoke()
                }
            }
        }
    }
}

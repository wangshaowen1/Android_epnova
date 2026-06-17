package com.kuaigeglobal.epnova.feature.creator.assets

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.StringUtils.getString
import com.drake.spannable.replaceSpan
import com.drake.spannable.span.CenterImageSpan
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.ext.showCommonDialog
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.utils.AutoProgressSimulator
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.ext.showRechargeDialog
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.ApiResponse
import com.kuaigeglobal.epnova.core.network.apis.entity.AssetInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.RoleImageInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleAsset
import com.kuaigeglobal.epnova.core.network.apis.entity.SimplePropInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleRoleInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleSceneInfo
import com.kuaigeglobal.epnova.core.network.apis.request.AssetStoryImageBody
import com.kuaigeglobal.epnova.core.network.apis.request.StrategyTypeBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkBody
import com.kuaigeglobal.epnova.core.network.apis.response.AssetStoryListInfo
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.PhotoPagerActivity
import com.kuaigeglobal.epnova.feature.recharge.PowerRechargeDialog
import com.kuaigeglobal.epnova.widget.LoadStateViewHolder
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 资产管理
 */
class AssetManagementViewModel : AppRepositoryViewModel<AssetManagementRepository>() {
    companion object {
        private const val POWER = "[power]"

        /**
         * 初始化
         */
        private const val STEP_INIT = 0

        /**
         * 资产描述生成中
         */
        private const val STEP_DESCRIPTION_GENERATING = 1

        /**
         * 资产描述已生成
         */
        private const val STEP_DESCRIPTION_GENERATED = 2

        /**
         * 资产图片生成中
         */
        const val STEP_IMAGE_GENERATING = 3

        /**
         * 资产图片生成失败
         */
        private const val STEP_IMAGE_GENERATE_FAILED = 4

        /**
         * 资产图片已生成
         */
        private const val STEP_IMAGE_GENERATED = 5
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
    val adapter by lazy { AssetManagementAdapter() }
    private val addRoleItem by lazy { ItemAddVM(R.string.add_character.getResString()).apply { onItemClick.value = { addRole(this) } } }
    private val addSceneItem by lazy { ItemAddVM(R.string.add_scene.getResString()).apply { onItemClick.value = { addScene(this) } } }
    private val addPropItem by lazy { ItemAddVM(R.string.add_prop.getResString()).apply { onItemClick.value = { addProp(this) } } }
    private var editingAssetDescVM: ItemAssetVM<*>? = null
    private var editingAssetNameVM: ItemAssetVM<*>? = null
    private var loopNextEnabledJob: Job? = null
    private var loopAllAssetDescriptionStatusJob: Job? = null
    private var inputJob: Job? = null
    val step by lazy { ObservableInt(STEP_INIT) }
    val cost by lazy { ObservableInt() }
    private var aiPrice = 0
    val keyboardHeight by lazy { ObservableInt(100) }
    var generating = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    progressHelper.start(R.string.extract_assets.getResString(), step = 1, intervalMs = 100L)
                } else {
                    progressHelper.stop()
                }
            }
        }
    private val progressHelper by lazy { AutoProgressSimulator(viewModelScope) { show, title, progress -> promptGenerating.set(show) } }
    val promptGenerating by lazy { ObservableBoolean() }
    private val powerSpan by lazy { CenterImageSpan(App.instance, R.drawable.icon_power).apply { setDrawableSize(16.dp, 16.dp) } }
    private var editConfirm = false
    val hasRole by lazy { ObservableBoolean(false) }
    val hasScene by lazy { ObservableBoolean(false) }
    val hasProp by lazy { ObservableBoolean(false) }

    var onAssetImageSelectEvent: ((ItemAssetVM<*>) -> Unit)? = null

    override fun onResume() {
        super.onResume()
        topActivity?.let {
            KeyboardUtils.registerSoftInputChangedListener(it) { height ->
                keyboardHeight.set(height)
                if (height <= 0) {
//                    editingAssetDescVM?.exitEditAssetDesc()
                    editingAssetNameVM?.exitEditAssetName()
//                    editingAssetDescVM = null
                    editingAssetNameVM = null
                }
            }
        }
    }

    override fun onPause() {
        loopNextEnabledJob?.cancel()
        topActivity?.window?.let { KeyboardUtils.unregisterSoftInputChangedListener(it) }
        super.onPause()
    }

    override fun onCleared() {
        loopAllAssetDescriptionStatusJob?.cancel()
        inputJob?.cancel()
        adapter.items.forEach { if (it is ItemAssetVM<*>) it.clear() }
        super.onCleared()
    }

    fun retry() {
        loadState.set(LoadStateViewHolder.CONTENT)
        when (step.get()) {
            STEP_INIT -> generateAllAssetDescription()
            STEP_DESCRIPTION_GENERATING -> loopAllAssetDescriptionStatus()
            STEP_DESCRIPTION_GENERATED -> getAssets()
        }
    }

    private fun generateAllAssetDescription() {
        if (step.get() > STEP_INIT) return
        generating = true
        loadState.set(LoadStateViewHolder.CONTENT)
        launch(block = {
            val allAssetDescriptionStatus = withContext(Dispatchers.IO) { mRepo.api.generateAllAssetDescription(WorkBody(workId, storyId)).data }
            taskId = allAssetDescriptionStatus?.taskId
            if (allAssetDescriptionStatus?.genStatus == 1 || allAssetDescriptionStatus?.genStatus == 2) {
                step.set(STEP_DESCRIPTION_GENERATING)
                loopAllAssetDescriptionStatus()
            } else {
                loadState.set(LoadStateViewHolder.RETRY)
                generating = false
            }
        }, onError = {
            loadState.set(LoadStateViewHolder.RETRY)
            generating = false
        })
    }

    private fun loopAllAssetDescriptionStatus() {
        loopAllAssetDescriptionStatusJob?.cancel()
        generating = true
        loopAllAssetDescriptionStatusJob = launch(block = {
            var genStatus: Int? = 1
            runCatching {
                val status = withContext(Dispatchers.IO) { mRepo.workAssetAllStatus(taskId, null).data?.genStatus }
                genStatus = status
            }
            //生成状态 [0=待生成 1=生成中 2=成功 3=失败]
            when (genStatus) {
                1 -> {
                    delay(3000)
                    loopAllAssetDescriptionStatus()
                }

                2 -> {
                    step.set(STEP_DESCRIPTION_GENERATED)
                    getAssets()
                }

                else -> {
                    step.set(STEP_INIT)
                    loadState.set(LoadStateViewHolder.RETRY)
                    generating = false
                }
            }
        }, onError = {
            loadState.set(LoadStateViewHolder.RETRY)
            generating = false
        })
    }

    fun getAssets() {
        generating = true
        launch(
            block = {
                val response = mRepo.workAssetStoryList(workId, storyId)
                if (!response.isSuccess) {
                    loadState.set(LoadStateViewHolder.RETRY)
                    generating = false
                    return@launch
                }
                submitAssetStoryList(response.data)
            },
            onError = {
                loadState.set(LoadStateViewHolder.RETRY)
                generating = false
            }
        )
    }

    private fun submitAssetStoryList(data: AssetStoryListInfo?) {
        val items = mutableListOf<AppItemViewModel<*>>()
        val roles = data?.roles?.map { info -> info.toSimpleRoleInfo().toItemAssetVM() }
        if (roles.isNullOrEmpty()) {
            hasRole.set(false)
        } else {
            hasRole.set(true)
            items.add(ItemTitleVM(R.string.roles.getResString()))
            items.addAll(roles)
//                    items.add(addRoleItem)
        }
        val scenes = data?.scenes?.map { info -> info.toSimpleSceneInfo().toItemAssetVM() }
        if (scenes.isNullOrEmpty()) {
            hasScene.set(false)
        } else {
            hasScene.set(true)
            items.add(ItemTitleVM(R.string.scenes.getResString()))
            items.addAll(scenes)
//                    items.add(addSceneItem)
        }
        val props = data?.props?.map { info -> info.toSimplePropInfo().toItemAssetVM() }
        if (props.isNullOrEmpty()) {
            hasProp.set(false)
        } else {
            hasProp.set(true)
            items.add(ItemTitleVM(R.string.props.getResString()))
            items.addAll(props)
//                    items.add(addPropItem)
        }
        adapter.items.forEach { if (it is ItemAssetVM<*>) it.clear() }
        adapter.submitList(items)
        onItemChanged()
        loadState.set(LoadStateViewHolder.CONTENT)
        generating = false
    }

    private fun updateAssetStoryList(assetList: AssetStoryListInfo?) {
        assetList?.roles?.forEach { info ->
            val role = info.toSimpleRoleInfo()
            adapter.items.find { it is ItemRoleVM && it.data.get()?.roleId == role.roleId }?.let {
                (it as? ItemRoleVM)?.apply {
                    data.set(role)
                    loopGenStatus()
                }
            }
        }
        assetList?.scenes?.forEach { info ->
            val scene = info.toSimpleSceneInfo()
            adapter.items.find { it is ItemSceneVM && it.data.get()?.sceneId == scene.sceneId }?.let {
                (it as? ItemSceneVM)?.apply {
                    data.set(scene)
                    loopGenStatus()
                }
            }
        }
        assetList?.props?.forEach { info ->
            val prop = info.toSimplePropInfo()
            adapter.items.find { it is ItemPropVM && it.data.get()?.propId == prop.propId }?.let {
                (it as? ItemPropVM)?.apply {
                    data.set(prop)
                    loopGenStatus()
                }
            }
        }
        onItemChanged()
    }

    private fun AssetInfo.toSimpleRoleInfo() = SimpleRoleInfo(
        roleId = assetId,
        appearanceId = assetImageId,
        roleName = assetName,
        rolePrompt = assetPrompt,
        imageUrl = imageUrl,
        genStatus = genStatus,
        taskId = taskId,
        hasHistory = hasHistory
    )

    private fun AssetInfo.toSimpleSceneInfo() = SimpleSceneInfo(
        sceneId = assetId,
        appearanceId = assetImageId,
        sceneName = assetName,
        scenePrompt = assetPrompt,
        imageUrl = imageUrl,
        genStatus = genStatus,
        taskId = taskId,
        hasHistory = hasHistory
    )

    private fun AssetInfo.toSimplePropInfo() = SimplePropInfo(
        propId = assetId,
        appearanceId = assetImageId,
        propName = assetName,
        propPrompt = assetPrompt,
        imageUrl = imageUrl,
        genStatus = genStatus,
        taskId = taskId,
        hasHistory = hasHistory
    )

    private fun SimpleRoleInfo.toItemAssetVM() =
        ItemRoleVM(workId, storyId, ::onItemChanged, this, getAiPriceProvider = { getOrRequestAiPrice() }).applyConfig()

    private fun SimpleSceneInfo.toItemAssetVM() =
        ItemSceneVM(workId, storyId, ::onItemChanged, this, getAiPriceProvider = { getOrRequestAiPrice() }).applyConfig()

    private fun SimplePropInfo.toItemAssetVM() =
        ItemPropVM(workId, storyId, ::onItemChanged, this, getAiPriceProvider = { getOrRequestAiPrice() }).applyConfig()

    fun addRole(addItem: ItemAddVM) {
        val count = adapter.items.count { it is ItemRoleVM || it is ItemPropVM || it is ItemSceneVM }
        if (count >= 9) {
            getString(R.string.asset_image_limit_reached).showToast()
            return
        }
        val index = adapter.items.indexOf(addItem)
        adapter.add(index, ItemRoleVM(workId, storyId, ::onItemChanged, null, getAiPriceProvider = { getOrRequestAiPrice() }).applyConfig())
        onItemChanged()
    }

    fun addScene(addItem: ItemAddVM) {
        val count = adapter.items.count { it is ItemRoleVM || it is ItemPropVM || it is ItemSceneVM }
        if (count >= 9) {
            getString(R.string.asset_image_limit_reached).showToast()
            return
        }
        val index = adapter.items.indexOf(addItem)
        adapter.add(index, ItemSceneVM(workId, storyId, ::onItemChanged, null, getAiPriceProvider = { getOrRequestAiPrice() }).applyConfig())
        onItemChanged()
    }

    fun addProp(addItem: ItemAddVM) {
        val count = adapter.items.count { it is ItemRoleVM || it is ItemPropVM || it is ItemSceneVM }
        if (count >= 9) {
            getString(R.string.asset_image_limit_reached).showToast()
            return
        }
        val index = adapter.items.indexOf(addItem)
        adapter.add(index, ItemPropVM(workId, storyId, ::onItemChanged, null, getAiPriceProvider = { getOrRequestAiPrice() }).applyConfig())
        onItemChanged()
    }

    private fun onItemChanged(showToast: Boolean = true) {
        var newNum = 0
        var generatingNum = 0
        var generatedNum = 0
        var failedNum = 0
        adapter.items.forEachIndexed { index, item ->
            if (item is ItemAssetVM<*>) {
                //-1=新创建 0=待生成 1=生成中 2=成功 3=失败 4=算力不足
                when (item.status.get()) {
                    -1, 0, 10 -> newNum++
                    1 -> generatingNum++
                    2 -> generatedNum++
                    3, 4 -> failedNum++
                }
            }
        }
        when {
            newNum > 0 -> {
                step.set(STEP_DESCRIPTION_GENERATED)
                calcCost()
            }

            failedNum > 0 -> {
                if (showToast && step.get() != STEP_IMAGE_GENERATE_FAILED) {
                    R.string.asset_generation_partial_failure.getResString().showToast()
                }
                step.set(STEP_IMAGE_GENERATE_FAILED)
                calcCost()
            }

            generatingNum > 0 -> step.set(STEP_IMAGE_GENERATING)

            else -> step.set(STEP_IMAGE_GENERATED)
        }
    }

    private fun ItemAssetVM<*>.applyConfig(): ItemAssetVM<*> {
        onItemClick.value = click@{ view ->
            when (view?.id) {
                R.id.ivCover -> {
                    assetImage.get()?.let { PhotoPagerActivity.launchWithAiChip(topActivity, it) }
                }

                R.id.ivCoverBorder -> {
                    if (status.get() == -1) return@click
                    if (assetImage.get().isNullOrEmpty() || (status.get() == 0 || status.get() == 10)) {
                        onAssetImageSelectEvent?.invoke(this)
                        return@click
                    }
                }

                R.id.btnImageDel -> {
                    data.get()?.let {
                        cancelImage(this)
                    }
                }

                R.id.etAssetDesc -> {
                    editingAssetNameVM?.exitEditAssetName()
                    editingAssetNameVM = null
//                    if (editingAssetDescVM == this) return@click
//                    inputJob?.cancel()
//                    val edit: (delay: Long) -> Unit = { delay ->
//                        editingAssetDescVM?.exitEditAssetDesc()
//                        editingAssetDescVM = this
//                        if (delay > 0) {
//                            inputJob = launch {
//                                delay(delay)
//                                editAssetDesc()
//                            }
//                        } else {
//                            editAssetDesc()
//                        }
//                    }
                    if (assetDesc.get().isNullOrEmpty() || editConfirm) {
                        editConfirm = true
                        editAssetDesc()
                    } else {
                        editConfirm = true
                        topActivity?.showCommonDialog(
                            tip = R.string.edit_text_warning.getResString(), confirmText =
                                R.string.continue_editing.getResString()
                        ) {
                            editAssetDesc()
                        }
                    }
                }

                R.id.etAssetNameCover -> {
//                    editingAssetDescVM?.exitEditAssetDesc()
                    editingAssetNameVM?.exitEditAssetName()
//                    editingAssetDescVM = null
                    editingAssetNameVM = this
                    editAssetName()
                }

                R.id.btnDelete -> {
                    if (data.get() == null) {
                        adapter.remove(this)
                        onItemChanged(false)
                    } else {
                        topActivity?.showCommonDialog(
                            title = R.string.confirm_deletion.getResString(),
                            tip = R.string.delete_impact_warning.getResString(),
                            confirmText = R.string.del.getResString()
                        ) {
                            deleteAsset(this)
                        }
                    }
                }
            }
        }
        return this
    }

    private fun deleteAsset(item: ItemAssetVM<*>) {
        launchWithLoading {
            response(item.delete()) {
                item.clear()
                adapter.remove(item)
                onItemChanged(false)
            }
        }
    }

    private fun oneKeyGenerate() {
        val cost = cost.get()
        if (cost == 0) {
            calcCost()
            return
        }
        generate(cost)
    }

    private fun generate(cost: Int) {
        if (PowerRechargeDialog.checkLowBalance(cost, PowerRechargeDialog.TAB_CREDITS)) return
        EventManager.onFirebaseTrack(FirebaseEvent.ASSET_GENERATE_CLICK)
        var notInput = false
        val notGenerateItems = arrayListOf<Triple<ItemAssetVM<*>, String, String>>()
        adapter.items.forEachIndexed { index, item ->
            if (item is ItemAssetVM<*>) {
                if (item.status.get() < 1 || item.status.get() == 3 || item.status.get() == 4 || item.status.get() == 10) {
                    val assetDesc = item.assetDesc.get()
                    val assetName = if (item.editingAssetName.get()) item.editingAssetNameText else item.assetName.get()
                    if (assetDesc.isNullOrEmpty() || assetName.isNullOrEmpty()) {
                        notInput = true
                    } else {
                        notGenerateItems.add(Triple(item, assetDesc.toString(), assetName.toString()))
                    }
                }
            }
        }
        if (notInput) {
            R.string.missing_asset_description.getResString().showToast()
        }
        if (notGenerateItems.isNotEmpty()) {
            launchWithLoading {
                notGenerateItems.forEach { (item, assetDesc, assetName) -> if (!item.save(assetDesc, assetName)) return@launchWithLoading }
                val response = withContext(Dispatchers.IO) { mRepo.api.workAssetOneKeyGenerate(WorkBody(workId, storyId)) }
                if (!response.isSuccess) {
                    if (response.code == ApiResponse.RESPONSE_CODE_POWER_NOT_ENOUGH) {
                        showRechargeDialog(true)
                    } else {
                        response.msg?.showToast()
                    }
                    return@launchWithLoading
                }
                val assetsResponse = mRepo.workAssetStoryList(workId, storyId)
                if (!assetsResponse.isSuccess) {
                    assetsResponse.msg?.showToast()
                    return@launchWithLoading
                }
                updateAssetStoryList(assetsResponse.data)
            }
        }
    }

    private fun calcCost() {
        cost.set(0)
        launch {
            getOrRequestAiPrice()
            if (aiPrice > 0) {
                val num = adapter.items.count { it is ItemAssetVM<*> && it.status.get().let { status -> status < 1 || status == 3 || status == 4 || status == 10 } }
                cost.set(aiPrice * num)
            }
        }
    }

    /**
     * 确保获取到有效的 AI 价格
     */
    suspend fun getOrRequestAiPrice(): Int {
        if (aiPrice <= 0) {
            return withContext(Dispatchers.IO) {
                val response = mRepo.api.queryAiPrice(StrategyTypeBody(1))
                val price = response.data?.pointNum ?: 0
                aiPrice = price
                price
            }
        }
        return aiPrice
    }

    private fun generateAllStoryboardDescription(toNextStep: ((workId: String?, storyId: String?, taskId: String?) -> Unit)?) {
        launchWithLoading {
            response(mRepo.api.generateAllStoryboardDescription(WorkBody(workId, storyId))) {
                toNextStep?.invoke(workId, storyId, data?.taskId)
            }
        }
    }

    fun cancelImage(vm: ItemAssetVM<*>?) {
        launchWithLoading {
            val asset = vm?.data?.get()
            val response = withContext(Dispatchers.IO) {
                mRepo.api.workAssetStoryRemoveImage(
                    AssetStoryImageBody(
                        workId = workId,
                        storyId = storyId,
                        assetId = asset?.assetId
                    )
                )
            }
            if (!response.isSuccess) {
                response.msg?.showToast()
                return@launchWithLoading
            } else {
                vm?.updateStatus(0, null)
                calcCost()
            }
        }
    }

    fun saveImage(simpleAsset: SimpleAsset?, roleImageInfo: RoleImageInfo?, assetId: String? = null) {
        launchWithLoading {
            val response = withContext(Dispatchers.IO) {
                when (simpleAsset) {
                    is SimpleSceneInfo -> mRepo.workAssetSceneSave(
                        assetId,
                        simpleAsset?.assetAppearanceId,
                        workId,
                        storyId,
                        null,
                        null,
                        imageUrl = simpleAsset?.assetImage
                    )

                    is SimplePropInfo -> mRepo.workAssetPropSave(
                        assetId,
                        simpleAsset?.assetAppearanceId,
                        workId,
                        storyId,
                        null,
                        null,
                        simpleAsset?.assetImage
                    )

                    else -> mRepo.workAssetRoleSave(
                        simpleAsset?.assetId,
                        roleImageInfo?.appearanceId,
                        workId,
                        storyId,
                        null,
                        null,
                        roleImageInfo?.sourceAppearanceId,
                        roleImageInfo?.imageUrl
                    )
                }
            }
            if (response.isSuccess) {
                adapter.items.find {
                    it is ItemAssetVM<*> && it.data.get()?.assetId == (assetId ?: simpleAsset?.assetId)
                }?.let {
                    (it as? ItemAssetVM<*>)?.apply {
                        updateStatus(2, response.data?.assetImage)
                    }
                    calcCost()
                }
            } else {
                response.msg?.showToast()
            }
        }
    }

    fun buttonEnable(step: Int): Boolean {
        return step == STEP_DESCRIPTION_GENERATED || step == STEP_IMAGE_GENERATE_FAILED || step == STEP_IMAGE_GENERATED
    }

//    fun buttonText(step: Int, cost: Int): CharSequence? {
//        return when (step) {
//            STEP_DESCRIPTION_GENERATED -> if (cost > 0) "一键生成  $POWER $cost".replaceSpan(POWER) { powerSpan } else "一键生成"
//            STEP_IMAGE_GENERATING -> "图片生成中..."
//            STEP_IMAGE_GENERATE_FAILED -> if (cost > 0) "重试失败项  $POWER $cost".replaceSpan(POWER) { powerSpan } else "重试失败项"
//            STEP_IMAGE_GENERATED -> "下一步：分镜设计"
//            else -> null
//        }
//    }

    fun buttonText(step: Int, cost: Int): CharSequence? {
        return when (step) {
            STEP_DESCRIPTION_GENERATED -> {
                if (cost > 0) {
                    R.string.btn_one_click_generate_cost.getResString(POWER, cost)
                        .replaceSpan(POWER) { powerSpan }
                } else {
                    R.string.btn_one_click_generate.getResString()
                }
            }

            STEP_IMAGE_GENERATING -> {
                R.string.btn_image_generating.getResString()
            }

            STEP_IMAGE_GENERATE_FAILED -> {
                if (cost > 0) {
                    R.string.btn_retry_failed_cost.getResString(POWER, cost)
                        .replaceSpan(POWER) { powerSpan }
                } else {
                    R.string.btn_retry_failed.getResString()
                }
            }

            STEP_IMAGE_GENERATED -> {
                R.string.btn_next_step_storyboard.getResString()
            }

            else -> null
        }
    }

    /**
     * 点击下一步
     */
    fun clickNext(toNextStep: ((workId: String?, storyId: String?, taskId: String?) -> Unit)?) {
        when (step.get()) {
            STEP_DESCRIPTION_GENERATED, STEP_IMAGE_GENERATE_FAILED -> {
                //一键生成、重试失败项
                oneKeyGenerate()
            }

            STEP_IMAGE_GENERATED -> {
                //下一步：分镜设计
                EventManager.onFirebaseTrack(FirebaseEvent.ASSET_MANAGEMENT_NEXT_CLICK)
                generateAllStoryboardDescription(toNextStep)
            }
        }
    }

    fun onRoleGenerate(event: Events.RoleGenerateEvent) {
        adapter.items.firstOrNull { it is ItemRoleVM && it.data.get()?.roleId == event.role?.roleId }?.let { it as? ItemRoleVM }?.apply {
            val role = data.get()
            role?.taskId = event.role?.taskId
            role?.genStatus = event.role?.genStatus
            event.role?.genStatus?.let { updateStatus(it) }
            data.set(role)
            loopGenStatus()
        }
    }

    fun onSceneGenerate(event: Events.SceneGenerateEvent) {
        adapter.items.firstOrNull { it is ItemSceneVM && it.data.get()?.sceneId == event.scene?.sceneId }?.let { it as? ItemSceneVM }?.apply {
            val scene = data.get()
            scene?.taskId = event.scene?.taskId
            scene?.genStatus = event.scene?.genStatus
            event.scene?.genStatus?.let { updateStatus(it) }
            data.set(scene)
            loopGenStatus()
        }
    }

    fun onPropGenerate(event: Events.PropGenerateEvent) {
        adapter.items.firstOrNull { it is ItemPropVM && it.data.get()?.propId == event.prop?.propId }?.let { it as? ItemPropVM }?.apply {
            val prop = data.get()
            prop?.taskId = event.prop?.taskId
            prop?.genStatus = event.prop?.genStatus
            event.prop?.genStatus?.let { updateStatus(it) }
            data.set(prop)
            loopGenStatus()
        }
    }
}

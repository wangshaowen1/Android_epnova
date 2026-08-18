package com.kuaigeglobal.epnova.feature.creator.assets

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.sensitiveWordCheck
import com.kuaigeglobal.epnova.core.ext.showRechargeDialog
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.ApiResponse
import com.kuaigeglobal.epnova.core.network.apis.entity.AssetGenerateInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.AssetInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.ReferenceImageInfo
import com.kuaigeglobal.epnova.core.network.apis.request.AssetGenerateBody
import com.kuaigeglobal.epnova.core.network.apis.request.AssetSaveBody
import com.kuaigeglobal.epnova.core.network.apis.request.AssetUnlinkBody
import com.kuaigeglobal.epnova.core.network.apis.request.TaskIdBody
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.creator.assets.edit.ReferenceHistoricalImageryDialog
import com.kuaigeglobal.epnova.feature.recharge.PowerConsumeConfirmDialog
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ItemSceneVM(
    workId: String?,
    storyId: String?,
    onItemChanged: () -> Unit,
    data: AssetInfo?,
    private val getAiPriceProvider: suspend () -> Int
) : ItemAssetVM(workId, storyId, onItemChanged, data) {

    override val assetType: String = R.string.scenes.getResString()
    override val assetDescHint: String = R.string.scene_description.getResString()
    override val assetNameHint: String = R.string.scene_name.getResString()
    override val generateButtonText: String = R.string.generate.getResString()

    override fun clickGenerate() {
        if (status.get() == 1) return
        val scenePrompt = assetDesc.get()
        val sceneName = if (editingAssetName.get()) editingAssetNameText else assetName.get()
        if (scenePrompt.isNullOrEmpty()) {
            showToast(R.string.please_enter_scene_description)
            return
        }
        if (sceneName.isNullOrEmpty()) {
            showToast(R.string.please_enter_scene_name)
            return
        }
        val fragmentManager = (topActivity as? FragmentActivity)?.supportFragmentManager
        if (data.get()?.hasHistory == true && fragmentManager != null) {
            ReferenceHistoricalImageryDialog.newInstance(
                data.get()?.assetId,
                storyId,
                workId,
                ReferenceHistoricalImageryDialog.BIZ_TYPE_SCENE_IMAGE
            )
                .apply { onUserReference = { notifyPowerConsume { generate(scenePrompt.toString(), sceneName.toString(), it) } } }
                .show(fragmentManager)
        } else {
            notifyPowerConsume { generate(scenePrompt.toString(), sceneName.toString()) }
        }
    }

    fun notifyPowerConsume(callback: () -> Unit) {
        if (Config.get().notifyPowerConsume(1)) {
            launch {
                val cost = getAiPriceProvider()
                PowerConsumeConfirmDialog.show(cost)?.apply {
                    onConfirm = {
                        callback.invoke()
                    }
                }
            }
        } else {
            callback.invoke()
        }
    }

    override fun generate(scenePrompt: String, sceneName: String, reference: ReferenceImageInfo?) {
        val preStatus = status.get()
        if (preStatus == 1) return
        updateStatus(1)
        launch(
            block = {
                var scene = data.get()
                if (scene?.assetId.isNullOrEmpty() || scenePrompt != scene?.assetPrompt || sceneName != scene.assetName) {
                    sensitiveWordCheck(scenePrompt).let { (isPass, highlightText) ->
                        if (!isPass) {
                            highlightText?.let { assetDesc.set(it) }
                            updateStatus(preStatus)
                            return@launch
                        }
                    }
                    sensitiveWordCheck(sceneName).let { (isPass, highlightText) ->
                        if (!isPass) {
                            highlightText?.let { assetName.set(it) }
                            updateStatus(preStatus)
                            return@launch
                        }
                    }
                    val response = withContext(Dispatchers.IO) {
                        val body = AssetSaveBody(
                            workId = workId,
                            storyId = storyId,
                            assetId = scene?.assetId,
                            assetType = AssetInfo.TYPE_SCENE,
                            assetName = sceneName,
                            assetPrompt = scenePrompt,
                        )
                        ApiFactory.apiService.workAssetSave(body)
                    }
                    if (response.isSuccess && response.data != null) {
                        val savedScene = response.data?.mergeSavedScene(scene, scenePrompt, sceneName)
                        scene = savedScene
                        data.set(savedScene)
                    } else {
                        response.msg?.showToast()
                        updateStatus(preStatus)
                        return@launch
                    }
                }
                val assetId = scene?.assetId
                if (assetId.isNullOrEmpty()) {
                    R.string.toast_save_fail.getResString().showToast()
                    updateStatus(preStatus)
                    return@launch
                }
                val body = AssetGenerateBody(
                    workId = workId,
                    storyId = storyId,
                    assetId = assetId,
                    prompt = scenePrompt,
                    referenceAssetImageId = reference?.assetImageId,
                    referenceImageInStatus = if (reference != null) 1 else 0,
                )
                response(
                    bean = ApiFactory.apiService.workAssetSingleGenerate(body),
                    result = {
                        val info = data
                        if (info == null) {
                            updateStatus(preStatus)
                        } else {
                            val scene = (this@ItemSceneVM.data.get() ?: AssetInfo(assetType = AssetInfo.TYPE_SCENE)).withGenerateInfo(info)
                            this@ItemSceneVM.data.set(scene)
                            info.genStatus?.let { updateStatus(it) }
                            FlowEventBus.postSync(Events.SceneGenerateEvent(workId, storyId, scene))
                            if (info.genStatus == 1) loopGenStatus()
                        }
                    },
                    error = {
                        if (
                            this?.code == ApiResponse.RESPONSE_CODE_POWER_NOT_ENOUGH ||
                            this?.code == ApiResponse.RESPONSE_CODE_POWER_ERROR
                        ) {
                            viewModelScope.launch {
                                showRechargeDialog(true)
                            }
                        } else {
                            this?.msg?.showToast()
                        }
                        updateStatus(preStatus)
                    })
            },
            onError = { updateStatus(preStatus) }
        )
    }

    private fun AssetInfo.withGenerateInfo(info: AssetGenerateInfo?) = copy(
        assetId = info?.assetId ?: assetId,
        assetType = assetType ?: AssetInfo.TYPE_SCENE,
        assetImageId = info?.assetImageId ?: assetImageId,
        genStatus = info?.genStatus ?: genStatus,
        taskId = info?.taskId ?: taskId
    )

    override suspend fun save(scenePrompt: String, sceneName: String): Boolean {
        val scene = data.get()
        if (scenePrompt == scene?.assetPrompt && sceneName == scene.assetName) return true
        if (scenePrompt != scene?.assetPrompt) {
            sensitiveWordCheck(scenePrompt).let { (isPass, highlightText) ->
                if (!isPass) {
                    highlightText?.let { assetDesc.set(it) }
                    return false
                }
            }
        }
        if (sceneName != scene?.assetName) {
            sensitiveWordCheck(sceneName).let { (isPass, highlightText) ->
                if (!isPass) {
                    highlightText?.let { assetName.set(it) }
                    return false
                }
            }
        }
        val response = withContext(Dispatchers.IO) {
            val body = AssetSaveBody(
                workId = workId,
                storyId = storyId,
                assetId = scene?.assetId,
                assetType = AssetInfo.TYPE_SCENE,
                assetName = sceneName,
                assetPrompt = scenePrompt,
            )
            ApiFactory.apiService.workAssetSave(body)
        }
        if (response.isSuccess && response.data != null) {
            data.set(response.data?.mergeSavedScene(scene, scenePrompt, sceneName))
        } else {
            response.msg?.showToast()
            return false
        }
        return true
    }

    private fun AssetInfo.mergeSavedScene(oldScene: AssetInfo?, scenePrompt: String?, sceneName: String?) = copy(
        assetId = assetId ?: oldScene?.assetId,
        assetType = assetType ?: oldScene?.assetType ?: AssetInfo.TYPE_SCENE,
        assetImageId = assetImageId ?: oldScene?.assetImageId,
        assetName = assetName ?: sceneName ?: oldScene?.assetName,
        assetPrompt = assetPrompt ?: scenePrompt ?: oldScene?.assetPrompt,
        imageUrl = imageUrl ?: oldScene?.imageUrl,
        genStatus = genStatus ?: oldScene?.genStatus,
        taskId = taskId ?: oldScene?.taskId,
        hasHistory = hasHistory ?: oldScene?.hasHistory
    )

    override suspend fun delete(): ApiResponse<Any> {
        return withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetSceneDelete(AssetUnlinkBody(workId, storyId, data.get()?.assetId)) }
    }

    override fun loopGenStatus() {
        loopGenStatusJob?.cancel()
        if (cleared) return
        val taskId = data.get()?.taskId
        if (taskId.isNullOrEmpty()) return
        loopGenStatusJob = launch {
            runCatching {
                val taskStatus = withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetSingleStatus(TaskIdBody(taskId, AssetInfo.IMAGE_BIZ_TYPE_SCENE)).data }
                data.get()?.let {
                    data.set(
                        it.copy(
                            genStatus = taskStatus?.genStatus,
                            imageUrl = taskStatus?.result
                        )
                    )
                    assetImage.set(taskStatus?.result)
                }
                data.notifyChange()
                taskStatus?.genStatus?.let { updateStatus(it) }
            }
            if (status.get() == 1) {
                delay(3000)
                loopGenStatus()
            } else {
                onItemChanged()
            }
        }
    }
}

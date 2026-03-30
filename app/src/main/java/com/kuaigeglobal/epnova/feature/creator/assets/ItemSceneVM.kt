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
import com.kuaigeglobal.epnova.core.network.apis.entity.ReferenceImageInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleSceneInfo
import com.kuaigeglobal.epnova.core.network.apis.request.GenerateAssetBody
import com.kuaigeglobal.epnova.core.network.apis.request.IdsBody
import com.kuaigeglobal.epnova.core.network.apis.request.SaveSceneBody
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
    data: SimpleSceneInfo?,
    private val getAiPriceProvider: suspend () -> Int
) : ItemAssetVM<SimpleSceneInfo>(workId, storyId, onItemChanged, data) {

    override val assetType: String = R.string.scenes.getResString()
    override val assetDescHint: String = R.string.scene_description.getResString()
    override val assetNameHint: String = R.string.scene_name.getResString()
    override val generateButtonText: String = R.string.generate.getResString()

    override fun clickGenerate() {
        if (status.get() == 1) return
        val scenePrompt = if (editingAssetDesc.get()) editingAssetDescText else assetDesc.get()
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
                data.get()?.sceneId,
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
                if (scenePrompt != scene?.scenePrompt || sceneName != scene.sceneName) {
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
                        val body = SaveSceneBody(
                            workId = workId,
                            storyId = storyId,
                            sceneId = scene?.sceneId,
                            appearanceId = scene?.appearanceId,
                            scenePrompt = scenePrompt,
                            sceneName = sceneName,
                        )
                        ApiFactory.apiService.workAssetSceneSave(body)
                    }
                    if (response.isSuccess && response.data != null) {
                        scene = response.data
                        data.set(response.data)
                    } else {
                        response.msg?.showToast()
                        updateStatus(preStatus)
                        return@launch
                    }
                }
                val body = GenerateAssetBody(
                    workId = workId,
                    storyId = storyId,
                    assetId = scene?.sceneId,
                    appearanceId = scene?.appearanceId,
                    referenceImageId = reference?.appearanceId,
                    referenceImageUrl = reference?.imageUrl,
                    referenceImageInStatus = if (reference != null) 1 else 0,
                    bizType = if (reference != null) 62 else 61,
                )
                response(
                    bean = ApiFactory.apiService.workAssetSingleGenerate(body),
                    result = {
                        val scene = this@ItemSceneVM.data.get() ?: SimpleSceneInfo()
                        scene.taskId = data?.taskId
                        scene.genStatus = data?.genStatus
                        data?.genStatus?.let { updateStatus(it) }
                        this@ItemSceneVM.data.set(scene)
                        FlowEventBus.postSync(Events.SceneGenerateEvent(workId, storyId, scene))
                        loopGenStatus()
                    },
                    error = {
                        if (this?.code == ApiResponse.RESPONSE_CODE_POWER_NOT_ENOUGH) {
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

    override suspend fun save(scenePrompt: String, sceneName: String): Boolean {
        val scene = data.get()
        if (scenePrompt == scene?.scenePrompt && sceneName == scene.sceneName) return true
        if (scenePrompt != scene?.scenePrompt) {
            sensitiveWordCheck(scenePrompt).let { (isPass, highlightText) ->
                if (!isPass) {
                    highlightText?.let { assetDesc.set(it) }
                    return false
                }
            }
        }
        if (sceneName != scene?.sceneName) {
            sensitiveWordCheck(sceneName).let { (isPass, highlightText) ->
                if (!isPass) {
                    highlightText?.let { assetName.set(it) }
                    return false
                }
            }
        }
        val response = withContext(Dispatchers.IO) {
            val body = SaveSceneBody(
                workId = workId,
                storyId = storyId,
                sceneId = scene?.sceneId,
                appearanceId = scene?.appearanceId,
                scenePrompt = scenePrompt,
                sceneName = sceneName,
            )
            ApiFactory.apiService.workAssetSceneSave(body)
        }
        if (response.isSuccess && response.data != null) {
            data.set(response.data)
        } else {
            response.msg?.showToast()
            return false
        }
        return true
    }

    override suspend fun delete(): ApiResponse<Any> {
        return withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetSceneDelete(IdsBody(null, data.get()?.appearanceId)) }
    }

    override fun loopGenStatus() {
        loopGenStatusJob?.cancel()
        if (cleared) return
        val taskId = data.get()?.taskId
        if (taskId.isNullOrEmpty()) return
        loopGenStatusJob = launch {
            runCatching {
                val taskStatus = withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetSingleStatus(TaskIdBody(taskId, 61)).data }
                data.get()?.let {
                    it.genStatus = taskStatus?.genStatus
                    it.imageUrl = taskStatus?.result
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
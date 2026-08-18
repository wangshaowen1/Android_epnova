package com.kuaigeglobal.epnova.feature.creator.assets

import androidx.fragment.app.FragmentActivity
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.ext.handlePointInsufficientError
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.sensitiveWordCheck
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
import kotlinx.coroutines.withContext

class ItemPropVM(
    workId: String?,
    storyId: String?,
    onItemChanged: () -> Unit,
    data: AssetInfo?,
    private val getAiPriceProvider: suspend () -> Int
) : ItemAssetVM(workId, storyId, onItemChanged, data) {

    override val assetType: String = R.string.props.getResString()
    override val assetDescHint: String = R.string.prop_description.getResString()
    override val assetNameHint: String = R.string.prop_name.getResString()
    override val generateButtonText: String = R.string.generate.getResString()

    override fun clickGenerate() {
        if (status.get() == 1) return
        val propPrompt = assetDesc.get()
        val propName = if (editingAssetName.get()) editingAssetNameText else assetName.get()
        if (propPrompt.isNullOrEmpty()) {
            showToast(R.string.please_enter_prop_description)
            return
        }
        if (propName.isNullOrEmpty()) {
            showToast(R.string.please_enter_prop_name)
            return
        }

        val fragmentManager = (topActivity as? FragmentActivity)?.supportFragmentManager
        if (data.get()?.hasHistory == true && fragmentManager != null) {
            ReferenceHistoricalImageryDialog.newInstance(
                data.get()?.assetId,
                storyId,
                workId,
                ReferenceHistoricalImageryDialog.BIZ_TYPE_PROP_IMAGE
            )
                .apply { onUserReference = { notifyPowerConsume { generate(propPrompt.toString(), propName.toString(), it) } } }
                .show(fragmentManager)
        } else {
            notifyPowerConsume {
                generate(propPrompt.toString(), propName.toString())
            }
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

    override fun generate(propPrompt: String, propName: String, reference: ReferenceImageInfo?) {
        val preStatus = status.get()
        if (preStatus == 1) return
        updateStatus(1)
        launch(
            block = {
                var prop = data.get()
                if (prop?.assetId.isNullOrEmpty() || propPrompt != prop?.assetPrompt || propName != prop.assetName) {
                    sensitiveWordCheck(propPrompt).let { (isPass, highlightText) ->
                        if (!isPass) {
                            highlightText?.let { assetDesc.set(it) }
                            updateStatus(preStatus)
                            return@launch
                        }
                    }
                    sensitiveWordCheck(propName).let { (isPass, highlightText) ->
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
                            assetId = prop?.assetId,
                            assetType = AssetInfo.TYPE_PROP,
                            assetName = propName,
                            assetPrompt = propPrompt,
                        )
                        ApiFactory.apiService.workAssetSave(body)
                    }
                    if (response.isSuccess && response.data != null) {
                        val savedProp = response.data?.mergeSavedProp(prop, propPrompt, propName)
                        prop = savedProp
                        data.set(savedProp)
                    } else {
                        response.msg?.showToast()
                        updateStatus(preStatus)
                        return@launch
                    }
                }
                val assetId = prop?.assetId
                if (assetId.isNullOrEmpty()) {
                    R.string.toast_save_fail.getResString().showToast()
                    updateStatus(preStatus)
                    return@launch
                }
                val body = AssetGenerateBody(
                    workId = workId,
                    storyId = storyId,
                    assetId = assetId,
                    prompt = propPrompt,
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
                            val prop = (this@ItemPropVM.data.get() ?: AssetInfo(assetType = AssetInfo.TYPE_PROP)).withGenerateInfo(info)
                            this@ItemPropVM.data.set(prop)
                            info.genStatus?.let { updateStatus(it) }
                            FlowEventBus.postSync(Events.PropGenerateEvent(workId, storyId, prop))
                            if (info.genStatus == 1) loopGenStatus()
                        }
                    },
                    error = {
                        if (!handlePointInsufficientError(this?.code)) {
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
        assetType = assetType ?: AssetInfo.TYPE_PROP,
        assetImageId = info?.assetImageId ?: assetImageId,
        genStatus = info?.genStatus ?: genStatus,
        taskId = info?.taskId ?: taskId
    )

    override suspend fun save(propPrompt: String, propName: String): Boolean {
        val prop = data.get()
        if (propPrompt == prop?.assetPrompt && propName == prop.assetName) return true
        if (propPrompt != prop?.assetPrompt) {
            sensitiveWordCheck(propPrompt).let { (isPass, highlightText) ->
                if (!isPass) {
                    highlightText?.let { assetDesc.set(it) }
                    return false
                }
            }
        }
        if (propName != prop?.assetName) {
            sensitiveWordCheck(propName).let { (isPass, highlightText) ->
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
                assetId = prop?.assetId,
                assetType = AssetInfo.TYPE_PROP,
                assetName = propName,
                assetPrompt = propPrompt,
            )
            ApiFactory.apiService.workAssetSave(body)
        }
        if (response.isSuccess && response.data != null) {
            data.set(response.data?.mergeSavedProp(prop, propPrompt, propName))
        } else {
            response.msg?.showToast()
            return false
        }
        return true
    }

    private fun AssetInfo.mergeSavedProp(oldProp: AssetInfo?, propPrompt: String?, propName: String?) = copy(
        assetId = assetId ?: oldProp?.assetId,
        assetType = assetType ?: oldProp?.assetType ?: AssetInfo.TYPE_PROP,
        assetImageId = assetImageId ?: oldProp?.assetImageId,
        assetName = assetName ?: propName ?: oldProp?.assetName,
        assetPrompt = assetPrompt ?: propPrompt ?: oldProp?.assetPrompt,
        imageUrl = imageUrl ?: oldProp?.imageUrl,
        genStatus = genStatus ?: oldProp?.genStatus,
        taskId = taskId ?: oldProp?.taskId,
        hasHistory = hasHistory ?: oldProp?.hasHistory
    )

    override suspend fun delete(): ApiResponse<Any> {
        return withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetPropDelete(AssetUnlinkBody(workId, storyId, data.get()?.assetId)) }
    }

    override fun loopGenStatus() {
        loopGenStatusJob?.cancel()
        if (cleared) return
        val taskId = data.get()?.taskId
        if (taskId.isNullOrEmpty()) return
        loopGenStatusJob = launch {
            runCatching {
                val taskStatus = withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetSingleStatus(TaskIdBody(taskId, AssetInfo.IMAGE_BIZ_TYPE_PROP)).data }
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

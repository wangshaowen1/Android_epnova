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
import com.kuaigeglobal.epnova.core.network.apis.entity.AssetInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.ReferenceImageInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SimplePropInfo
import com.kuaigeglobal.epnova.core.network.apis.request.AssetSaveBody
import com.kuaigeglobal.epnova.core.network.apis.request.AssetUnlinkBody
import com.kuaigeglobal.epnova.core.network.apis.request.GenerateAssetBody
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

class ItemPropVM(
    workId: String?,
    storyId: String?,
    onItemChanged: () -> Unit,
    data: SimplePropInfo?,
    private val getAiPriceProvider: suspend () -> Int
) : ItemAssetVM<SimplePropInfo>(workId, storyId, onItemChanged, data) {

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
                data.get()?.propId,
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
                if (propPrompt != prop?.propPrompt || propName != prop.propName) {
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
                            assetId = prop?.propId,
                            assetType = 3,
                            assetName = propName,
                            assetPrompt = propPrompt,
                        )
                        ApiFactory.apiService.workAssetSave(body)
                    }
                    if (response.isSuccess && response.data != null) {
                        val savedProp = response.data?.toSimplePropInfo(prop, propPrompt, propName)
                        prop = savedProp
                        data.set(savedProp)
                    } else {
                        response.msg?.showToast()
                        updateStatus(preStatus)
                        return@launch
                    }
                }
                val body = GenerateAssetBody(
                    workId = workId,
                    storyId = storyId,
                    assetId = prop?.propId,
                    appearanceId = prop?.appearanceId,
                    referenceImageId = reference?.appearanceId,
                    referenceImageUrl = reference?.imageUrl,
                    referenceImageInStatus = if (reference != null) 1 else 0,
                    bizType = if (reference != null) 52 else 51,
                )
                response(
                    bean = ApiFactory.apiService.workAssetSingleGenerate(body),
                    result = {
                        val prop = this@ItemPropVM.data.get() ?: SimplePropInfo()
                        prop.taskId = data?.taskId
                        prop.genStatus = data?.genStatus
                        data?.genStatus?.let { updateStatus(it) }
                        this@ItemPropVM.data.set(prop)
                        FlowEventBus.postSync(Events.PropGenerateEvent(workId, storyId, prop))
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

    override suspend fun save(propPrompt: String, propName: String): Boolean {
        val prop = data.get()
        if (propPrompt == prop?.propPrompt && propName == prop.propName) return true
        if (propPrompt != prop?.propPrompt) {
            sensitiveWordCheck(propPrompt).let { (isPass, highlightText) ->
                if (!isPass) {
                    highlightText?.let { assetDesc.set(it) }
                    return false
                }
            }
        }
        if (propName != prop?.propName) {
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
                assetId = prop?.propId,
                assetType = 3,
                assetName = propName,
                assetPrompt = propPrompt,
            )
            ApiFactory.apiService.workAssetSave(body)
        }
        if (response.isSuccess && response.data != null) {
            data.set(response.data.toSimplePropInfo(prop, propPrompt, propName))
        } else {
            response.msg?.showToast()
            return false
        }
        return true
    }

    private fun AssetInfo.toSimplePropInfo(oldProp: SimplePropInfo?, propPrompt: String?, propName: String?) = SimplePropInfo(
        propId = assetId ?: oldProp?.propId,
        appearanceId = oldProp?.appearanceId,
        assetImageId = assetImageId ?: oldProp?.assetImageId,
        propName = assetName ?: propName ?: oldProp?.propName,
        propPrompt = assetPrompt ?: propPrompt ?: oldProp?.propPrompt,
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
                val taskStatus = withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetSingleStatus(TaskIdBody(taskId, 51)).data }
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

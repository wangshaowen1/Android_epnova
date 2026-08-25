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
import com.kuaigeglobal.epnova.core.network.apis.request.CreationScene
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

class ItemRoleVM(
    workId: String?,
    storyId: String?,
    onItemChanged: () -> Unit,
    data: AssetInfo?,
    private val getAiPriceProvider: suspend () -> Int
) : ItemAssetVM(workId, storyId, onItemChanged, data) {

    override val assetType: String = R.string.roles.getResString()
    override val assetDescHint: String = R.string.role_description.getResString()
    override val assetNameHint: String = R.string.role_name.getResString()
    override val generateButtonText: String = R.string.generate.getResString()

    override fun clickGenerate() {
        if (status.get() == 1) return
        val rolePrompt = assetDesc.get()
        val roleName = if (editingAssetName.get()) editingAssetNameText else assetName.get()
        if (rolePrompt.isNullOrEmpty()) {
            showToast(R.string.please_enter_role_description)
            return
        }
        if (roleName.isNullOrEmpty()) {
            showToast(R.string.please_enter_role_name)
            return
        }

        val fragmentManager = (topActivity as? FragmentActivity)?.supportFragmentManager
        if (data.get()?.hasHistory == true && fragmentManager != null) {
            ReferenceHistoricalImageryDialog.newInstance(
                data.get()?.assetId,
                storyId,
                workId,
                ReferenceHistoricalImageryDialog.BIZ_TYPE_ROLE_IMAGE
            )
                .apply { onUserReference = { notifyPowerConsume { generate(rolePrompt.toString(), roleName.toString(), it) } } }
                .show(fragmentManager)
        } else {
            notifyPowerConsume { generate(rolePrompt.toString(), roleName.toString()) }
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

    override fun generate(rolePrompt: String, roleName: String, reference: ReferenceImageInfo?) {
        val preStatus = status.get()
        if (preStatus == 1) return
        updateStatus(1)
        launch(
            block = {
                var role = data.get()
                if (role?.assetId.isNullOrEmpty() || rolePrompt != role?.assetPrompt || roleName != role.assetName) {
                    sensitiveWordCheck(rolePrompt).let { (isPass, highlightText) ->
                        if (!isPass) {
                            highlightText?.let { assetDesc.set(it) }
                            updateStatus(preStatus)
                            return@launch
                        }
                    }
                    sensitiveWordCheck(roleName).let { (isPass, highlightText) ->
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
                            assetId = role?.assetId,
                            assetType = AssetInfo.TYPE_ROLE,
                            assetName = roleName,
                            assetPrompt = rolePrompt,
                        )
                        ApiFactory.apiService.workAssetSave(body)
                    }
                    if (response.isSuccess && response.data != null) {
                        val savedRole = response.data?.mergeSavedRole(role, rolePrompt, roleName)
                        role = savedRole
                        data.set(savedRole)
                    } else {
                        response.msg?.showToast()
                        updateStatus(preStatus)
                        return@launch
                    }
                }
                val assetId = role?.assetId
                if (assetId.isNullOrEmpty()) {
                    R.string.toast_save_fail.getResString().showToast()
                    updateStatus(preStatus)
                    return@launch
                }
                val body = AssetGenerateBody(
                    workId = workId,
                    storyId = storyId,
                    assetId = assetId,
                    prompt = rolePrompt,
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
                            val role = (this@ItemRoleVM.data.get() ?: AssetInfo(assetType = AssetInfo.TYPE_ROLE)).withGenerateInfo(info)
                            this@ItemRoleVM.data.set(role)
                            info.genStatus?.let { updateStatus(it) }
                            FlowEventBus.postSync(Events.RoleGenerateEvent(workId, storyId, role))
                            if (info.genStatus == 1) loopGenStatus()
                        }
                    },
                    error = {
                        val handled = handlePointInsufficientError(
                            creationScene = CreationScene.IMAGE_GENERATION,
                            errorCode = this?.code,
                        )
                        if (!handled) {
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
        assetType = assetType ?: AssetInfo.TYPE_ROLE,
        assetImageId = info?.assetImageId ?: assetImageId,
        genStatus = info?.genStatus ?: genStatus,
        taskId = info?.taskId ?: taskId
    )

    override suspend fun save(rolePrompt: String, roleName: String): Boolean {
        val role = data.get()
        if (rolePrompt == role?.assetPrompt && roleName == role.assetName) return true
        if (rolePrompt != role?.assetPrompt) {
            sensitiveWordCheck(rolePrompt).let { (isPass, highlightText) ->
                if (!isPass) {
                    highlightText?.let { assetDesc.set(it) }
                    return false
                }
            }
        }
        if (roleName != role?.assetName) {
            sensitiveWordCheck(roleName).let { (isPass, highlightText) ->
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
                assetId = role?.assetId,
                assetType = AssetInfo.TYPE_ROLE,
                assetName = roleName,
                assetPrompt = rolePrompt,
            )
            ApiFactory.apiService.workAssetSave(body)
        }
        if (response.isSuccess && response.data != null) {
            data.set(response.data?.mergeSavedRole(role, rolePrompt, roleName))
        } else {
            response.msg?.showToast()
            return false
        }
        return true
    }

    private fun AssetInfo.mergeSavedRole(oldRole: AssetInfo?, rolePrompt: String?, roleName: String?) = copy(
        assetId = assetId ?: oldRole?.assetId,
        assetType = assetType ?: oldRole?.assetType ?: AssetInfo.TYPE_ROLE,
        assetImageId = assetImageId ?: oldRole?.assetImageId,
        assetName = assetName ?: roleName ?: oldRole?.assetName,
        assetPrompt = assetPrompt ?: rolePrompt ?: oldRole?.assetPrompt,
        imageUrl = imageUrl ?: oldRole?.imageUrl,
        genStatus = genStatus ?: oldRole?.genStatus,
        taskId = taskId ?: oldRole?.taskId,
        hasHistory = hasHistory ?: oldRole?.hasHistory
    )

    override suspend fun delete(): ApiResponse<Any> {
        return withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetRoleDelete(AssetUnlinkBody(workId, storyId, data.get()?.assetId)) }
    }

    override fun loopGenStatus() {
        loopGenStatusJob?.cancel()
        if (cleared) return
        val taskId = data.get()?.taskId
        if (taskId.isNullOrEmpty()) return
        loopGenStatusJob = launch {
            runCatching {
                val taskStatus = withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetSingleStatus(TaskIdBody(taskId, AssetInfo.IMAGE_BIZ_TYPE_ROLE)).data }
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
                if (status.get() == 3) {
                    withContext(Dispatchers.Main) {
                        (taskStatus?.errorMsg ?: R.string.asset_generation_partial_failure.getResString()).showToast()
                    }
                }
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

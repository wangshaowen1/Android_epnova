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
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleRoleInfo
import com.kuaigeglobal.epnova.core.network.apis.request.GenerateAssetBody
import com.kuaigeglobal.epnova.core.network.apis.request.IdsBody
import com.kuaigeglobal.epnova.core.network.apis.request.SaveRoleBody
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

class ItemRoleVM(
    workId: String?,
    storyId: String?,
    onItemChanged: () -> Unit,
    data: SimpleRoleInfo?,
    private val getAiPriceProvider: suspend () -> Int
) : ItemAssetVM<SimpleRoleInfo>(workId, storyId, onItemChanged, data) {

    override val assetType: String = R.string.roles.getResString()
    override val assetDescHint: String = R.string.role_description.getResString()
    override val assetNameHint: String = R.string.role_name.getResString()
    override val generateButtonText: String = R.string.generate.getResString()

    override fun clickGenerate() {
        if (status.get() == 1) return
        val rolePrompt = if (editingAssetDesc.get()) editingAssetDescText else assetDesc.get()
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
                data.get()?.roleId,
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
                if (rolePrompt != role?.rolePrompt || roleName != role.roleName) {
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
                        val body = SaveRoleBody(
                            workId = workId,
                            storyId = storyId,
                            roleId = role?.roleId,
                            appearanceId = role?.appearanceId,
                            rolePrompt = rolePrompt,
                            roleName = roleName,
                        )
                        ApiFactory.apiService.workAssetRoleSave(body)
                    }
                    if (response.isSuccess && response.data != null) {
                        role = response.data
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
                    assetId = role?.roleId,
                    appearanceId = role?.appearanceId,
                    referenceImageId = reference?.appearanceId,
                    referenceImageUrl = reference?.imageUrl,
                    referenceImageInStatus = if (reference != null) 1 else 0,
                    bizType = if (reference != null) 42 else 41,
                )
                response(
                    bean = ApiFactory.apiService.workAssetSingleGenerate(body),
                    result = {
                        val role = this@ItemRoleVM.data.get() ?: SimpleRoleInfo()
                        role.taskId = data?.taskId
                        role.genStatus = data?.genStatus
                        data?.genStatus?.let { updateStatus(it) }
                        this@ItemRoleVM.data.set(role)
                        FlowEventBus.postSync(Events.RoleGenerateEvent(workId, storyId, role))
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

    override suspend fun save(rolePrompt: String, roleName: String): Boolean {
        val role = data.get()
        if (rolePrompt == role?.rolePrompt && roleName == role.roleName) return true
        if (rolePrompt != role?.rolePrompt) {
            sensitiveWordCheck(rolePrompt).let { (isPass, highlightText) ->
                if (!isPass) {
                    highlightText?.let { assetDesc.set(it) }
                    return false
                }
            }
        }
        if (roleName != role?.roleName) {
            sensitiveWordCheck(roleName).let { (isPass, highlightText) ->
                if (!isPass) {
                    highlightText?.let { assetName.set(it) }
                    return false
                }
            }
        }
        val response = withContext(Dispatchers.IO) {
            val body = SaveRoleBody(
                workId = workId,
                storyId = storyId,
                roleId = role?.roleId,
                appearanceId = role?.appearanceId,
                rolePrompt = rolePrompt,
                roleName = roleName,
            )
            ApiFactory.apiService.workAssetRoleSave(body)
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
        return withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetRoleDelete(IdsBody(null, data.get()?.appearanceId)) }
    }

    override fun loopGenStatus() {
        loopGenStatusJob?.cancel()
        if (cleared) return
        val taskId = data.get()?.taskId
        if (taskId.isNullOrEmpty()) return
        loopGenStatusJob = launch {
            runCatching {
                val taskStatus = withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetSingleStatus(TaskIdBody(taskId, 41)).data }
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
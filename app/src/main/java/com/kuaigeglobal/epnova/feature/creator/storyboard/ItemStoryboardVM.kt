package com.kuaigeglobal.epnova.feature.creator.storyboard

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.ext.sensitiveWordCheck
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleStoryboardInfo
import com.kuaigeglobal.epnova.core.network.apis.request.SaveStoryboardBody
import com.kuaigeglobal.epnova.widget.RecyclerEditText
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ItemStoryboardVM(
    /**
     * 作品 ID
     */
    val workId: String?,
    /**
     * 剧集 id
     */
    val storyId: String?,
    data: SimpleStoryboardInfo?
) : AppItemViewModel<SimpleStoryboardInfo>(data) {
    val no by lazy { ObservableInt() }
    val selected by lazy { ObservableBoolean(false) }

    /**
     * 当前应用的分镜描述
     */
    val storyBoardDesc by lazy { ObservableField<CharSequence>(data?.storyBoardDesc) }

    /**
     * 编辑中的分镜描述
     */
    var editingStoryBoardDescText: CharSequence? = data?.storyBoardDesc
        private set
    val editingStoryBoardDesc by lazy { ObservableBoolean(false) }
    val storyboardDescInputListener by lazy { ObservableField<RecyclerEditText.OnTextInputListener>() }

    fun editStoryboardDesc() {
        storyboardDescInputListener.set(object : RecyclerEditText.OnTextInputListener {
            override fun onTextInput(text: String) {
                editingStoryBoardDescText = text
            }
        })
        editingStoryBoardDesc.set(true)
        editingStoryBoardDescText = storyBoardDesc.get()
    }

    fun exitEditStoryboardDesc() {
        storyboardDescInputListener.set(null)
        editingStoryBoardDesc.set(false)
        storyBoardDesc.set(editingStoryBoardDescText)
    }

    /**
     * 是否有未保存的分镜描述
     */
    fun notSaved(): Boolean {
        val storyboard = data.get()
        val prompt = (if (editingStoryBoardDesc.get()) editingStoryBoardDescText else storyBoardDesc.get())?.toString()
        if (prompt.isNullOrEmpty()) {
            return false
        }
        return prompt != storyboard?.storyBoardDesc
    }

    /**
     * 敏感词校验并保存
     * @return 无需保存或保存成功
     */
    suspend fun save(toast: Boolean = true): Boolean {
        var storyboard = data.get()
        val prompt = (if (editingStoryBoardDesc.get()) editingStoryBoardDescText else storyBoardDesc.get())?.toString()
        if (prompt.isNullOrEmpty()) {
            if (toast) showToast(R.string.please_complete_storyboard_description)
            return false
        }
        if (prompt != storyboard?.storyBoardDesc) {
            sensitiveWordCheck(prompt).let { (isPass, highlightText) ->
                if (!isPass) {
                    highlightText?.let { storyBoardDesc.set(it) }
                    return false
                }
            }
            val response = withContext(Dispatchers.IO) {
                val body = SaveStoryboardBody(
                    workId = workId,
                    storyId = storyId,
                    storyboardId = storyboard?.storyboardId,
                    storyBoardDesc = prompt
                )
                ApiFactory.apiService.workStoryboardSave(body)
            }
            if (response.isSuccess && response.data != null) {
                storyboard = response.data
                data.set(storyboard)
            } else {
                response.msg?.showToast()
                return false
            }
        }
        return true
    }
}
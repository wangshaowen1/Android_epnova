package com.kuaigeglobal.epnova.feature.creator.assets

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.blankj.utilcode.util.StringUtils.getString
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.ApiResponse
import com.kuaigeglobal.epnova.core.network.apis.entity.ReferenceImageInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleAsset
import com.kuaigeglobal.epnova.core.network.apis.entity.SimplePropInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleRoleInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleSceneInfo
import com.kuaigeglobal.epnova.feature.editor.info.InputDialog
import com.kuaigeglobal.epnova.widget.RecyclerEditText
import kotlinx.coroutines.Job

abstract class ItemAssetVM<Item : SimpleAsset>(
    /**
     * 作品 ID
     */
    val workId: String?,
    /**
     * 剧集 id
     */
    val storyId: String?,
    protected val onItemChanged: () -> Unit,
    data: Item?
) : AppItemViewModel<Item>(data) {

    /**
     * -1=新创建 0=待生成 1=生成中 2=成功 3=失败 10=取消设置（和待生成0一样处理）
     */
    val status by lazy { ObservableInt(data?.status ?: -1) }

    /**
     * 当前应用的资源描述
     */
    val assetDesc by lazy { ObservableField<CharSequence>(data?.assetDesc) }

    /**
     * 当前应用的资源图片URL
     */
    val assetImage by lazy { ObservableField(data?.assetImage) }

    /**
     * 编辑中的资源描述
     */
    var editingAssetDescText: CharSequence? = data?.assetDesc
    val editingAssetDesc by lazy { ObservableBoolean(false) }
    val assetDescInputListener by lazy { ObservableField<RecyclerEditText.OnTextInputListener>() }

    /**
     * 当前应用的资源名称
     */
    val assetName by lazy { ObservableField<CharSequence>(data?.assetName) }

    /**
     * 编辑中的资源名称
     */
    var editingAssetNameText: CharSequence? = data?.assetName
    val editingAssetName by lazy { ObservableBoolean(false) }
    val assetNameInputListener by lazy { ObservableField<RecyclerEditText.OnTextInputListener>() }

    protected var loopGenStatusJob: Job? = null
    protected var cleared = false

    val hint = ObservableField<String>()

    init {
        if (status.get() == 1) {
            loopGenStatus()
        }
        hint.set(
            when (data) {
                is SimpleRoleInfo -> getString(R.string.set_character_appearance)
                is SimpleSceneInfo -> getString(R.string.set_scene)
                is SimplePropInfo -> getString(R.string.set_prop)
                else -> ""
            }
        )
    }

    fun clear() {
        cleared = true
        loopGenStatusJob?.cancel()
    }

    override fun onCleared() {
        clear()
        super.onCleared()
    }

    fun editAssetDesc() {
        InputDialog.show(InputDialog.TYPE_ASSET_DESC, content = assetDesc.get()?.toString()) {
            assetDesc.set(it)
        }
//        assetDescInputListener.set(object : RecyclerEditText.OnTextInputListener {
//            override fun onTextInput(text: String) {
//                editingAssetDescText = text
//            }
//        })
//        editingAssetDesc.set(true)
//        editingAssetDescText = assetDesc.get()
    }

//    open fun exitEditAssetDesc() {
//        assetDescInputListener.set(null)
//        editingAssetDesc.set(false)
//        assetDesc.set(editingAssetDescText)
//    }

    fun editAssetName() {
        assetNameInputListener.set(object : RecyclerEditText.OnTextInputListener {
            override fun onTextInput(text: String) {
                editingAssetNameText = text
            }
        })
        editingAssetName.set(true)
        editingAssetNameText = assetName.get()
    }

    open fun exitEditAssetName() {
        assetNameInputListener.set(null)
        editingAssetName.set(false)
        assetName.set(editingAssetNameText)
    }

    /**
     * 资产类型
     */
    abstract val assetType: String
    abstract val assetDescHint: String
    abstract val assetNameHint: String
    abstract val generateButtonText: String

    /**
     * 点击单个资产 生成/重新生成
     */
    abstract fun clickGenerate()

    /**
     * 调用生成资产图片接口
     * @param reference 参考图信息
     */
    abstract fun generate(assetDesc: String, assetName: String, reference: ReferenceImageInfo? = null)

    /**
     * 敏感词校验并保存
     * @return 无需保存或保存成功
     */
    abstract suspend fun save(assetDesc: String, assetName: String): Boolean

    /**
     * 调用删除资产接口
     */
    abstract suspend fun delete(): ApiResponse<Any>

    abstract fun loopGenStatus()

    fun updateStatus(status: Int) {
        this.status.set(status)
        onItemChanged()
    }

    fun updateStatus(status: Int, imageUrl: String?) {
        this.status.set(status)
        assetImage.set(imageUrl)
        onItemChanged()
    }

    override fun equals(other: Any?): Boolean {
        return hashCode() == other?.hashCode()
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }
}
package com.kuaigeglobal.epnova.feature.creator.set

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.doAfterTextChanged
import com.blankj.utilcode.util.KeyboardUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.RecyclerViewVM
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.getChildByType
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoConfigData
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoConfigInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoStyleConfigInfo
import com.kuaigeglobal.epnova.feature.creator.set.allstyle.ItemCreationStyleVm
import com.kuaigeglobal.epnova.widget.decoration.GridSpacingItemDecoration
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.showToast

/**
 * *****************************************
 * @Introduce:创作
 * *****************************************
 */
class VideoSetViewModel : AppRepositoryViewModel<AppRepository>() {
    val configsData by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, VideoConfigData::class.java) }
    val workTitle by lazy { mBundle.getString(Constants.Extra.TITLE) }

    var checkedRatio: ItemVideoConfigVm? = null
    var checkedEpisodeSet: ItemVideoConfigVm? = null
    var checkedStyle: ItemCreationStyleVm? = null

    val ratioVms = arrayListOf<ItemVideoConfigVm>()
    val styleVms = arrayListOf<ItemCreationStyleVm>()
    val episodeVms = arrayListOf<ItemVideoConfigVm>()

    override fun onCreate() {
        super.onCreate()
        episodeVms.clear()
        ratioVms.clear()
        styleVms.clear()
        val charCount = mBundle.getInt(Constants.Extra.COUNT)
        if (charCount > 0) {
            configsData?.novelRecommendEpisodeGroupList?.find {
                charCount > (it.minCount ?: 0) && charCount < (it.maxCount ?: 0)
            }?.itemList?.forEach {
                episodeVms.add(initEpisodeVm(it))
            }
        } else {
            configsData?.totalEpisodeCntItemVoList?.forEach {
                episodeVms.add(initEpisodeVm(it))
            }
        }
        checkedEpisodeSet?.checked?.set(false)
        checkedEpisodeSet = episodeVms.maxByOrNull {//找最大的默认
            it.data.get()?.configValue?.toIntOrNull() ?: Int.MIN_VALUE
        }
        checkedEpisodeSet?.checked?.set(true)

        configsData?.ratioItemVoList?.forEach {
            ratioVms.add(initRatioVm(it))
        }
        configsData?.styleItemVOList?.forEach {
            styleVms.add(initStyleVm(it))
        }
    }

    fun initEpisodeVm(ratio: VideoConfigInfo): ItemVideoConfigVm {
        return ItemVideoConfigVm(ratio).apply {
            onItemClick.value = { view ->
                if (checkedEpisodeSet != this) {
                    checkedEpisodeSet?.checked?.set(false)
                    this.checked.set(true)
                    checkedEpisodeSet = this
                }

                if (ratio.isCustomInput()) {
                    val editText = (view as? ViewGroup)?.getChildByType(
                        EditText::class.java,
                        AppCompatEditText::class.java
                    ) as? EditText

                    editText?.apply {
                        onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                            if (!hasFocus) {
                                val num = text.toString().toIntOrNull() ?: 0
                                if (num > 60) {
                                    "剧集数不可超过60集".showToast()
                                    input.set("60")
                                }
                            }
                        }
                        setOnEditorActionListener { v, actionId, _ ->
                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                val num = text.toString().toIntOrNull() ?: 0
                                if (num > 60) {
                                    "剧集数不可超过60集".showToast()
                                    input.set("60")
                                }
                                clearFocus()
                                KeyboardUtils.hideSoftInput(v)
                                true
                            } else false
                        }
                    }

                    // 仅仅使用 post 等待 DataBinding 把 FrameLayout 设为 VISIBLE
                    view?.post {
                        // 直接请求焦点弹键盘
                        // 此时，因为 EditText 已经是 90dp 宽了，
                        // Android 原生的焦点追踪会平滑且完美地把这 90dp 全部推到屏幕内！
                        editText?.requestFocus()
                        editText?.let { KeyboardUtils.showSoftInput(it) }
                    }

                } else {
                    view?.let { KeyboardUtils.hideSoftInput(it) }
                }
            }
        }
    }

    fun initRatioVm(ratio: VideoConfigInfo): ItemVideoConfigVm {
        return ItemVideoConfigVm(ratio).apply {
            if (checkedRatio == null) {
                checked.set(true)
                checkedRatio = this
            }
            onItemClick.value = {
                if (checkedRatio != this) {
                    checkedRatio?.checked?.set(false)
                    this.checked.set(true)
                    checkedRatio = this
                }
            }
        }
    }

    fun initStyleVm(info: VideoStyleConfigInfo): ItemCreationStyleVm {
        return ItemCreationStyleVm(info).apply {
            if (checkedStyle == null) {
                checked.set(true)
                checkedStyle = this
            }
            onItemClick.value = {
                if (checkedStyle != this) {
                    checkedStyle?.checked?.set(false)
                    this.checked.set(true)
                    checkedStyle = this
                }
            }
        }
    }

    val mRatioAdapterVm =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_video_ratio, ratioVms)
            .setItemDecoration(
                GridSpacingItemDecoration(
                    2,
                    12.dp,
                    0
                )
            ).build()

    val mStyleAdapterVm =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_creation_style, styleVms).build()

    val mEpisodeAdapterVm =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_series_count, episodeVms).build()
}
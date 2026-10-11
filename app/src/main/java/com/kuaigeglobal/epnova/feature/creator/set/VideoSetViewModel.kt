package com.kuaigeglobal.epnova.feature.creator.set

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.KeyboardUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.RecyclerViewVM
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.request
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoConfigData
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoConfigInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoStyleConfigInfo
import com.kuaigeglobal.epnova.core.network.apis.request.CreateTaskBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkTaskData
import com.kuaigeglobal.epnova.core.network.apis.response.FirstRechargeDialogData
import com.kuaigeglobal.epnova.databinding.ItemSeriesCountBinding
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.creator.set.allstyle.ItemCreationStyleVm
import com.kuaigeglobal.epnova.widget.decoration.GridSpacingItemDecoration
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast

/**
 * *****************************************
 * @Introduce:创作
 * *****************************************
 */
class VideoSetViewModel : AppRepositoryViewModel<AppRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.same_story.getResString(),
        background = Color.TRANSPARENT
    )

    val configsData by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, VideoConfigData::class.java) }
    val workTitle by lazy { mBundle.getString(Constants.Extra.TITLE) }
    val sameStory by lazy { mBundle.getString(Constants.Extra.CONTENT) }
    val hotCaseId by lazy { mBundle.getString(Constants.Extra.ID) }
    /** GA4 创作同款事件使用的爆款案例名字。 */
    val hotCaseTitle by lazy { mBundle.getString(Constants.Extra.NAME) }

    var checkedRatio: ItemVideoConfigVm? = null
    var checkedEpisodeSet: ItemVideoConfigVm? = null
    var checkedStyle: ItemCreationStyleVm? = null

    val firstRechargeCache = MutableLiveData<FirstRechargeDialogData?>()
    val workCreating = MutableLiveData(false)

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
//        checkedEpisodeSet?.checked?.set(false)
//        checkedEpisodeSet = episodeVms.maxByOrNull {//找最大的默认
//            it.data.get()?.configValue?.toIntOrNull() ?: Int.MIN_VALUE
//        }
//        checkedEpisodeSet?.checked?.set(true)

        configsData?.ratioItemVoList?.forEach {
            ratioVms.add(initRatioVm(it))
        }
        configsData?.styleItemVOList?.forEach {
            styleVms.add(initStyleVm(it))
        }
    }

    fun initEpisodeVm(ratio: VideoConfigInfo): ItemVideoConfigVm {
        return ItemVideoConfigVm(ratio).apply {
            if (ratio.isDefaultConfig()) {
                checked.set(true)
                checkedEpisodeSet = this
            }
            if (ratio.isCustomInput()) {
                hasSingle.set(input.get()?.toIntOrNull() == 1)
                input.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
                    override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                        hasSingle.set(input.get()?.toIntOrNull() == 1)
                    }
                })
            }
            onItemClick.value = { view ->
                val focusContainer = (view?.parent as? RecyclerView)?.parent as? ViewGroup
                focusContainer?.apply {
                    isFocusable = true
                    isFocusableInTouchMode = true
                }
                // 在隐藏自定义输入框之前转移焦点，避免系统把焦点分配给作品标题。
                if (!ratio.isCustomInput() && checkedEpisodeSet?.data?.get()?.isCustomInput() == true) {
                    focusContainer?.requestFocus()
                }
                if (checkedEpisodeSet != this) {
                    checkedEpisodeSet?.checked?.set(false)
                    checked.set(true)
                    checkedEpisodeSet = this
                }

                if (ratio.isCustomInput()) {
                    view?.let { DataBindingUtil.getBinding<ItemSeriesCountBinding>(it) }?.let { binding ->
                        val editText = binding.etInput
                        // 居中数字单行输入的横向滚动会产生超大 scrollX，导致 RecyclerView 卷走并回收焦点 item。
                        editText.setHorizontallyScrolling(false)
                        editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                            if (!hasFocus) checkCustomEpisodeCount(this)
                        }
                        editText.setOnEditorActionListener { v, actionId, _ ->
                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                checkCustomEpisodeCount(this)
                                focusContainer?.requestFocus()
                                KeyboardUtils.hideSoftInput(v)
                                true
                            } else false
                        }
                        binding.executePendingBindings()
                        binding.root.doOnPreDraw {
                            if (checkedEpisodeSet !== this || binding.item !== this ||
                                !editText.isAttachedToWindow || !editText.isShown
                            ) return@doOnPreDraw
                            // 聚焦前清掉旧单行布局的偏移，避免抢在 TextView 自身的 pre-draw 之前触发错误滚动。
                            editText.scrollTo(0, 0)
                            if (!editText.requestFocus()) return@doOnPreDraw
                            editText.post {
                                if (checkedEpisodeSet === this && binding.item === this &&
                                    editText.isAttachedToWindow && editText.hasFocus()
                                ) {
                                    val imm = editText.context.getSystemService(InputMethodManager::class.java)
                                    // 只显示键盘，避免 utilcodex 的 toggleSoftInput 把已打开的键盘关闭。
                                    imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkCustomEpisodeCount(item: ItemVideoConfigVm) {
        if ((item.input.get()?.toIntOrNull() ?: 0) > 60) {
            item.input.set("60")
            R.string.episode_count_limit_60.getResString().showToast()
        }
    }

    fun initRatioVm(ratio: VideoConfigInfo): ItemVideoConfigVm {
        return ItemVideoConfigVm(ratio).apply {
            if (ratio.isDefaultConfig()) {
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
            if (info.isDefaultConfig()) {
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

    fun createSameTask(modelType: Int, body: CreateTaskBody, callback: (WorkTaskData?) -> Unit) {
        workCreating.value = true
        request(
            apiBlock = {
                when (modelType) {
                    DramaGenerateConstants.MODEL_STORY_SINGLE -> mRepo.api.createTaskFromSingleEpisode(body)
                    else -> mRepo.api.createSummaryTaskFromInspiration(body)
                }
            },
            onError = {
                workCreating.value = false
            },
            onSuccess = {
                workCreating.value = false
                callback.invoke(data)
            }
        )
    }

    fun getFirstRechargeInfo(callback: () -> Unit) {
        launch {
            val firstRechargeResponse = mRepo.api.getFirstRechargeDialogInfo()
            if (firstRechargeResponse.data != null && firstRechargeResponse.data?.isNeedShow() == true) {
                firstRechargeCache.value = firstRechargeResponse.data
            } else {
                callback.invoke()
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

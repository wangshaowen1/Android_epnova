package com.kuaigeglobal.epnova.feature.splash.newer

import android.view.View
import androidx.core.graphics.toColorInt
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.nhsd.basic.ext.getResDrawable

class ItemGuideStep3Vm(data: GuideStep3ViewModel.GenerationStep?) : AppItemViewModel<GuideStep3ViewModel.GenerationStep>(data) {

    // 暴露给 XML 绑定的属性
    val stepTitle = ObservableField(data?.title)
    val stepDesc = ObservableField(data?.waitingDesc)
    val descColor = ObservableInt("#8A8E9F".toColorInt())
    val titleColor = ObservableInt("#4dffffff".toColorInt())
    val itemBg = ObservableField(R.drawable.bg_guide_step3_item_waiting.getResDrawable())
    val iconBg = ObservableField(R.drawable.fill_color1_r14.getResDrawable())
    val isCompleteVisible = ObservableInt(View.GONE)
    val isLoadingVisible = ObservableInt(View.GONE)

    init {
        // 初始化状态
        updateStatus(data?.status)
    }

    // 更新状态的方法，供外部（主 ViewModel）调用
    fun updateStatus(newStatus: GuideStep3ViewModel.StepStatus?) {
        data.get()?.status = newStatus ?: GuideStep3ViewModel.StepStatus.WAITING
        when (newStatus) {
            GuideStep3ViewModel.StepStatus.WAITING -> {
                stepDesc.set(data.get()?.waitingDesc)
                descColor.set("#4dffffff".toColorInt())
                titleColor.set("#4dffffff".toColorInt())
                itemBg.set(R.drawable.bg_guide_step3_item_waiting.getResDrawable())
                iconBg.set(R.drawable.fill_color1_r14.getResDrawable())
                isCompleteVisible.set(View.GONE)
                isLoadingVisible.set(View.GONE)
            }

            GuideStep3ViewModel.StepStatus.IN_PROGRESS -> {
                stepDesc.set(data.get()?.inProgressDesc)
                descColor.set("#78A2FF".toColorInt()) // 假设这是你的进行中高亮色
                titleColor.set("#ffffff".toColorInt())
                itemBg.set(R.drawable.bg_guide_step3_item_generating.getResDrawable())
                iconBg.set(R.drawable.bg_10_5a6afe_r14.getResDrawable())
                isCompleteVisible.set(View.GONE)
                isLoadingVisible.set(View.VISIBLE)
            }

            GuideStep3ViewModel.StepStatus.COMPLETED -> {
                stepDesc.set(data.get()?.completedDesc)
                descColor.set("#78A2FF".toColorInt())
                titleColor.set("#ffffff".toColorInt())
                itemBg.set(R.drawable.bg_guide_step3_item_finish.getResDrawable())
                iconBg.set(R.drawable.bg_30_5a6afe_r14.getResDrawable())
                isCompleteVisible.set(View.VISIBLE)
                isLoadingVisible.set(View.GONE)
            }

            else -> {}
        }
    }
}
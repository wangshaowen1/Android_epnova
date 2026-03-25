package com.kuaigeglobal.epnova.feature.splash.newer

import android.graphics.drawable.Drawable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.RecyclerViewVM
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.widget.decoration.GridSpacingItemDecoration
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResDrawable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GuideStep3ViewModel : AppRepositoryViewModel<AppRepository>() {
    // 发送进度百分比 (0.0f ~ 1.0f) 供 Activity 监听执行宽度动画
    val progressEvent = MutableLiveData<Float>()

    // 发送跳转事件
    val navigationEvent = MutableLiveData<Boolean>()

    // 初始化 5 个步骤的数据
    val ratioVms = listOf(
        ItemGuideStep3Vm(
            GenerationStep(
                R.drawable.img_short_script.getResDrawable(),
                "创作故事剧本",
                inProgressDesc = "编剧正在构思剧情，故事即将展开...",
                completedDesc = "剧本创作完成"
            )
        ),
        ItemGuideStep3Vm(
            GenerationStep(
                R.drawable.img_short_scene.getResDrawable(),
                "绘制分镜画面",
                inProgressDesc = "分镜师正在绘制镜头，画面逐帧诞生...",
                completedDesc = "分镜画面已完成"
            )
        ),
        ItemGuideStep3Vm(
            GenerationStep(
                R.drawable.img_short_dubbing.getResDrawable(),
                "生成字幕台词",
                inProgressDesc = "字幕师正在校对台词，让每一句对白更清晰...",
                completedDesc = "字幕制作完成"
            )
        ),
        ItemGuideStep3Vm(
            GenerationStep(
                R.drawable.img_short_music.getResDrawable(),
                "制作背景音乐",
                inProgressDesc = "音乐总监正在为剧情创作专属BGM...",
                completedDesc = "背景音乐已完成"
            )
        ),
        ItemGuideStep3Vm(
            GenerationStep(
                R.drawable.img_short_synthesis.getResDrawable(),
                "质检 & 合成输出",
                inProgressDesc = "制片人正在审核成片，短片即将上映...",
                completedDesc = "成片已生成"
            )
        )
    )

    val step3Adapter =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_generation_step, ratioVms).build()

    // 启动生成流程
    fun startGenerationProcess() {
        viewModelScope.launch {
            val totalSteps = ratioVms.size
            val timePerStep = 3000L

            for (i in 0 until totalSteps) {
                val currentStepVm = ratioVms[i]

                // 1. 设置当前步骤为“进行中”
                currentStepVm.updateStatus(StepStatus.IN_PROGRESS)

                // 2. 通知 UI 层更新进度条动画 (计算当前目标百分比)
                val targetProgress = (i + 1) / totalSteps.toFloat()
                progressEvent.postValue(targetProgress)

                // 3. 挂起 3 秒
                delay(timePerStep)

                // 4. 设置当前步骤为“已完成”
                currentStepVm.updateStatus(StepStatus.COMPLETED)
            }

            // 5. 全部完成，延迟 0.5 秒跳转
            delay(500)
            navigationEvent.postValue(true)
        }
    }

    // 步骤的三个状态
    enum class StepStatus {
        WAITING,       // 等待中
        IN_PROGRESS,   // 进行中
        COMPLETED      // 已完成
    }

    // 步骤数据模型
    data class GenerationStep(
        val icon: Drawable?,
        val title: String,
        val waitingDesc: String = "等待中...",
        val inProgressDesc: String,
        val completedDesc: String,
        var status: StepStatus = StepStatus.WAITING
    )


}
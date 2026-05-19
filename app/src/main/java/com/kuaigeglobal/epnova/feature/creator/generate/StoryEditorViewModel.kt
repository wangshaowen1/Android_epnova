package com.kuaigeglobal.epnova.feature.creator.generate

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.network.apis.entity.CreationModelInfo
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.utils.Logger

class StoryEditorViewModel : AppRepositoryViewModel<AppRepository>() {

    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.untitled_title.getResString(),
        background = Color.TRANSPARENT
    )

    //作品id
    var workId: String? = null

    //作品标题
    val title = MutableLiveData("")

    //当前处于的步骤序号1-5
    val step = MutableLiveData<Int>()

    val dramaCategoryPopupEnabled by lazy { Config.get().isSwitchModelEnable() }

    //当前选择的创作模型
    val currentCreationModel = MutableLiveData<CreationModelInfo?>()
    val switchModelEvent = MutableLiveData<Long>()


    //从1还是从2开始，从2开始就是4步创作流程
    val startFrom = MutableLiveData<Int>()
    val model by lazy { mBundle.getInt(Constants.Extra.MODE) }
    val from by lazy { mBundle.getString(Constants.Extra.FROM_PAGE) }

    //子界面在流式输出的时候 用户要退出的时候需要弹出提示
    var backWarning = false

    //当前正在创作的剧集id
    var storyId: String? = null

    //上一集的剧集id,只有续写的时候才有
    var previousStoryId: String? = null

    //任务id
    var taskId: String? = null

    //视频比例
    var videoRatio: String? = null

    //当前章节的序号 1开始
    var chapterIndex: Int = 1


    override fun onCreate() {
        super.onCreate()
        title.value = mBundle.getString(Constants.Extra.TITLE, "")
        workId = mBundle.getString(Constants.Extra.WORK_ID)
        storyId = mBundle.getString(Constants.Extra.STORY_ID)
        taskId = mBundle.getString(Constants.Extra.TASK_ID)
        videoRatio = mBundle.getString(Constants.Extra.VIDEO_RATIO)
        previousStoryId = mBundle.getString(Constants.Extra.PREVIOUS_STORY_ID)
        chapterIndex = mBundle.getInt(Constants.Extra.CHAPTER_INDEX, 1)

        //中途恢复界面走这里
        val newStep = mBundle.getInt(StoryEditorActivity.KEY_STEP, -1)

        Logger.d("打印参数：model = $model ,newStep = $newStep ")

        when (model) {
            DramaGenerateConstants.MODEL_INSPIRATION, DramaGenerateConstants.MODEL_STORY_DOCUMENTS -> {
                startFrom.value = 1
                if (newStep != -1) {
                    step.value = newStep
                } else {
                    step.value = 1
                }
            }

            DramaGenerateConstants.MODEL_STORY, DramaGenerateConstants.MODEL_STORY_SINGLE -> {
                startFrom.value = 2
                if (newStep != -1) {
                    step.value = newStep
                } else {
                    step.value = 2
                }
            }

            DramaGenerateConstants.MODEL_CONTINUE_INSPIRATION, DramaGenerateConstants.MODE_CONTINUE_WRITING_NOVEL_DOCUMENTS -> {
                startFrom.value = 2
                if (newStep != -1) {
                    step.value = newStep
                } else {
                    step.value = 2
                }
            }

            DramaGenerateConstants.MODEL_CONTINUE_STORY -> {
                startFrom.value = 2
                if (newStep != -1) {
                    step.value = newStep
                } else {
                    step.value = 2
                }
            }

            else -> {
                step.value = 1
                startFrom.value = 1
            }
        }
    }

    fun nextStep(
        step: Int,
        workId: String? = null,
        storyId: String? = null,
        taskId: String? = null
    ) {
        workId?.let {
            this.workId = it
        }
        storyId?.let {
            this.storyId = it
        }
        taskId?.let {
            this.taskId = it
        }
        this.step.value = step
    }

    fun showModels(step: Int): Boolean {
        return step == 4
    }


}
package com.kuaigeglobal.epnova.feature.creator.generate

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.showConfirmPrompt
import com.kuaigeglobal.epnova.databinding.ActivityStoryEditorBinding
import com.kuaigeglobal.epnova.feature.creator.assets.AssetManagementFragment
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.creator.storyboard.StoryboardDesignFragment
import com.kuaigeglobal.epnova.feature.editor.info.DramaTitleEditDialog
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.BasicEdgeToEdgeConfig
import com.nhsd.basic.ext.setupTranslucentStatus

/**
 * 作品生成根界面（灵感模式有5步，小说模式只有4步，从剧本生成开始）
 * 灵感模式，小说续创也是4步 从剧本生成开始
 */
class StoryEditorActivity :
    BaseViewModelActivity<ActivityStoryEditorBinding, StoryEditorViewModel>() {

    // 单独设置当前页面内容可延伸到导航栏区域
//    override val edgeToEdgeStrategy: BasicEdgeToEdgeConfig.Strategy
//        get() = super.edgeToEdgeStrategy.copy(
//            rootInsetsMode = BasicEdgeToEdgeConfig.RootInsetsMode.NAVIGATION_BAR,
//        )

    override fun setTranslucentStatus() {
        // 导航栏图标浅色、背景透明
        setupTranslucentStatus(false, Color.TRANSPARENT)
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleBackPressed()
        }
    }

    companion object {
        const val KEY_STEP = "step"

        //来源 来自剧集详情
        const val FROM_SERIES = "fromSeries"

        //来源 来自我的作品
        const val FROM_MY_WORKS = "fromMyWorks"

        /**
         * 从头开始生成
         * @param workId 作品id（首次生成==》目前仅小说文档上传的情况下有值）
         * @param name 故事名称
         * @param model 故事生成方式
         */
        @JvmStatic
        fun start(
            context: Context?,
            workId: String?,
            name: String?,
            workType: Int? = null,
            model: Int? = DramaGenerateConstants.MODEL_INSPIRATION,
        ) {
            val context = context ?: return
            val starter = Intent(context, StoryEditorActivity::class.java)
            starter.putExtra(Constants.Extra.MODE, model)
            starter.putExtra(Constants.Extra.WORK_ID, workId)
            starter.putExtra(Constants.Extra.WORK_TYPE, workType)
            starter.putExtra(Constants.Extra.TITLE, name)
            context.startActivity(starter)
        }


        //续写
        @JvmStatic
        fun startContinue(
            context: Context?,
            workId: String? = null,
            workTitle: String? = null,
            //剧集id
            storyId: String? = null,
            //上一集的剧集id
            previousStoryId: String? = null,
            //当前章节的序号
            chapterIndex: Int? = 1,
            //剧集内容，续写小说的时候需要
            dramaContent: String? = null,
            workType: Int? = null,
            model: Int? = DramaGenerateConstants.MODEL_CONTINUE_INSPIRATION,
            videoRatio: String? = null,
            from: String? = null
        ) {
            val context = context ?: return
            val starter = Intent(context, StoryEditorActivity::class.java)
            starter.putExtra(Constants.Extra.MODE, model)
            starter.putExtra(Constants.Extra.TITLE, workTitle)
            starter.putExtra(Constants.Extra.WORK_ID, workId)
            starter.putExtra(Constants.Extra.WORK_TYPE, workType)
            starter.putExtra(Constants.Extra.STORY_ID, storyId)
            starter.putExtra(Constants.Extra.PREVIOUS_STORY_ID, previousStoryId)
            starter.putExtra(Constants.Extra.CHAPTER_INDEX, chapterIndex)
            starter.putExtra(Constants.Extra.CONTENT, dramaContent)
            starter.putExtra(Constants.Extra.VIDEO_RATIO, videoRatio)
            starter.putExtra(Constants.Extra.FROM_PAGE, from)
            context.startActivity(starter)
        }

        /**
         * @param step 步骤几  1 故事梗概 2 故事剧本 3 资产管理 4 分镜设计
         */
        @JvmStatic
        fun restore(
            context: Context?,
            step: Int,
            workId: String?,
            storyId: String?,
            taskId: String? = null,
            workTitle: String? = null,
            //当前章节的序号
            chapterIndex: Int? = 1,
            workType: Int? = null,
            model: Int? = DramaGenerateConstants.MODEL_CONTINUE_INSPIRATION,
            videoRatio: String? = null,
            from: String? = null
        ) {
            val context = context ?: return
            val starter = Intent(context, StoryEditorActivity::class.java)
            starter.putExtra(KEY_STEP, step)
            starter.putExtra(Constants.Extra.WORK_ID, workId)
            starter.putExtra(Constants.Extra.STORY_ID, storyId)
            starter.putExtra(Constants.Extra.TASK_ID, taskId)
            starter.putExtra(Constants.Extra.TITLE, workTitle)
            starter.putExtra(Constants.Extra.RESTORE, true)
            starter.putExtra(Constants.Extra.CHAPTER_INDEX, chapterIndex)
            starter.putExtra(Constants.Extra.WORK_TYPE, workType)
            starter.putExtra(Constants.Extra.MODE, model)
            starter.putExtra(Constants.Extra.VIDEO_RATIO, videoRatio)
            starter.putExtra(Constants.Extra.FROM_PAGE, from)
            context.startActivity(starter)
        }
    }

    private fun handleBackPressed() {
        if (mRealVM.step.value == 1 && mRealVM.backWarning) {//因为步骤1流式输出的时候 不会保存生成的内容
            showConfirmPrompt("退出后将无法保留生成的内容，请谨慎操作") {
                exitPage()
            }
        } else {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
            if (currentFragment is OnBackPressedHandler) {
                val isHandled = currentFragment.handleBackPressed()
                if (isHandled) return // 如果 Fragment 返回 true，表示它拦截了退出，Activity 直接 return
            }
            exitPage()
        }
    }

    private fun exitPage() {
        backPressedCallback.isEnabled = false
        onBackPressedDispatcher.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        mRealVM.title.observe(this) {
            mRealVM.mToolbarVM.mTitle.set(it)
        }
        mRealVM.step.observe(this) {
            when (it) {
                1 -> {
                    if (mRealVM.startFrom.value == 2) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.container, DramaGenerateStep2Fragment.newInstance())
                            .commitAllowingStateLoss()
                    } else {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.container, DramaGenerateStep1Fragment.newInstance())
                            .commitAllowingStateLoss()
                    }
                }

                2 -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, DramaGenerateStep2Fragment.newInstance())
                        .commitAllowingStateLoss()
                }

                3 -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, AssetManagementFragment.newInstance())
                        .commitAllowingStateLoss()
                }

                4 -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, StoryboardDesignFragment.newInstance())
                        .commitAllowingStateLoss()
                }
            }
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.tvWorkName, R.id.btnModify -> {
                val isEdit =
                    (mRealVM.step.value == 1 || (mRealVM.step.value == 2 && mRealVM.model == DramaGenerateConstants.MODEL_STORY))
                if (!isEdit) return
                val dialog = DramaTitleEditDialog.newInstance(mRealVM.title.value)
                dialog.onSave = {
                    mRealVM.title.value = it
                }
                dialog.show(supportFragmentManager)
            }

            R.id.btnModels -> {
                mRealVM.switchModelEvent.value = System.currentTimeMillis()
            }
        }
    }
}
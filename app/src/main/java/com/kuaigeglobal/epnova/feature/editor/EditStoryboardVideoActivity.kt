package com.kuaigeglobal.epnova.feature.editor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.applySmartFocusManager
import com.kuaigeglobal.epnova.common.ext.showCommonDialog
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.permissions.requestRead2WritePermissions
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.manager.AppEvent
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.databinding.ActivityEditStoryboardVideoBinding
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorActivity
import com.kuaigeglobal.epnova.feature.mine.child.MyCreationsFragment
import com.kuaigeglobal.epnova.feature.series.SeriesActivity
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.showToast

/**
 * 编辑分镜视频
 */
class EditStoryboardVideoActivity : BaseViewModelActivity<ActivityEditStoryboardVideoBinding, EditStoryboardVideoViewModel>() {
    companion object {
        @JvmStatic
        fun launchIntent(
            activity: Activity,
            storyTitle: String?,
            workId: String?,
            storyId: String?,
            workRatio: String?,
            story: EpisodeInfo?,
            from: String? = null,
            model: Int? = null
        ): Intent {
            val intent = Intent(activity, EditStoryboardVideoActivity::class.java)
            intent.putExtra(Constants.Extra.TITLE, storyTitle)
            intent.putExtra(Constants.Extra.WORK_ID, workId)
            intent.putExtra(Constants.Extra.STORY_ID, storyId)
            intent.putExtra(Constants.Extra.VIDEO_RATIO, workRatio)
            intent.putExtra(Constants.Extra.DATA, story)
            intent.putExtra(Constants.Extra.FROM_PAGE, from)
            intent.putExtra(Constants.Extra.MODE, model)
            return intent
        }

        @JvmStatic
        fun start(storyTitle: String?, workId: String?, storyId: String?, workRatio: String?, story: EpisodeInfo?, from: String? = null, model: Int? = null) {
            topActivity?.let { it.startActivity(launchIntent(it, storyTitle, workId, storyId, workRatio, story, from, model)) }
        }
    }

    val from by lazy { intent?.getStringExtra(Constants.Extra.FROM_PAGE) }

    override fun onBackPressed() {
        mRealVM.getStoryInfo {
            //重绘已完成
            if (it?.stepProgress == EpisodeInfo.EPISODE_REPAINT_COMPLETED) {
                showCommonDialog(
                    title = getString(R.string.confirm_exit_redraw),
                    tip = getString(R.string.exit_redraw_warning_msg),
                    cancelText = getString(R.string.confirm_exit),
                    cancel = {
                        mRealVM.dropRepaint()
                    },
                    confirmText = getString(R.string.continue_redraw),
                    confirm = {})
            } else {
                if (mRealVM.anyStoryboardGenerating()) {
                    getString(R.string.ai_generating_auto_saved_2).showToast()
                } else {
                    getString(R.string.auto_saved).showToast()
                }

                if (mRealVM.model == DramaGenerateConstants.MODEL_STORY_SINGLE) {
                    if (from != StoryEditorActivity.FROM_MY_WORKS) {
                        MyCreationsFragment.start()
                    } else {
                        finish()
                    }
                } else {
                    if (from != StoryEditorActivity.FROM_SERIES) {
                        SeriesActivity.start(mRealVM.workId)
                    }
                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.loadState.setNoDataIcon(0)
        binding.root.applySmartFocusManager()
        binding.loadState.setNoDataButton(getString(R.string.generate_storyboard_video)) {
            mRealVM.regenerateStoryboards()
        }
        mRealVM.scrollToItem.observe(this) { itemPosition ->
            if (itemPosition > -1 && isActive) {
                mRealVM.scrollToItem.value = -1
                bindingNullable?.rvStoryboard?.apply {
                    val itemView = getChildAt(0) ?: return@apply
                    val scrollX = computeHorizontalScrollOffset()
                    val itemWidth = itemView.width
                    val itemDividerWidth = itemView.marginStart + itemView.marginEnd
                    val left = paddingLeft + itemPosition * (itemWidth + itemDividerWidth) + itemView.marginStart - (width - itemWidth) / 2
                    smoothScrollBy(left - scrollX, 0)
                }
            }
        }
        observeEvents {
            on<Events.StoryboardSavedEvent> {
                mRealVM.updateStoryboard(it.storyboardInfo)
            }
            on<Events.StoryboardVideoRegenerateEvent> {
                mRealVM.getStoryboardList()
            }
            on<Events.StoryboardImageRegenerateEvent> {
                mRealVM.getStoryboardList()
            }
        }
        EventManager.onAppTrack(AppEvent.SF_VIEW_VIDEO_GENERATION_PAGE)
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnRegenerate -> {
                val current = mRealVM.currentStoryboard.get()?.data?.get() ?: return
                if (current.videoGenerating) {
                    getString(R.string.video_generating_please_wait).showToast()
                    return
                }
                EditStoryboardActivity.start(current, mRealVM.workRatio.get(), mRealVM.storyInfo?.stepProgress)
                EventManager.onAppTrack(AppEvent.SF_CLICK_REDRAW_STORYBOARD_BUTTON)
            }

            R.id.btnDownload -> {
                val current = mRealVM.currentStoryboard.get()?.data?.get() ?: return
                if (current.videoGenerating) {
                    getString(R.string.video_generating_please_wait).showToast()
                    return
                }
                if (current.videoGenerateFailed) {
                    getString(R.string.video_generating_please_wait).showToast()
                    return
                }
                if (current.videoGenerated) {
                    requestRead2WritePermissions { granted ->
                        if (granted) {
                            current.videoUrl?.let { mRealVM.download(current, it) }
                        }
                    }
                }
            }

            R.id.btnDelete -> {
                showCommonDialog(tip = getString(R.string.confirm_delete_scene_warning), confirmText = getString(R.string.confirm_del)) {
                    mRealVM.deleteCurrentStoryboard()
                }
            }

            R.id.btnGenerate -> {
                mRealVM.synthesis()
            }
        }
    }

}
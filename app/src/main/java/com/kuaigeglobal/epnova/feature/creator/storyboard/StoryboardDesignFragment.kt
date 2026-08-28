package com.kuaigeglobal.epnova.feature.creator.storyboard

import android.view.View
import androidx.databinding.Observable
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.applySmartFocusManager
import com.kuaigeglobal.epnova.common.ext.showCommonDialog
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.databinding.FragmentStoryboardDesignBinding
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.creator.generate.OnBackPressedHandler
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorActivity
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorViewModel
import com.kuaigeglobal.epnova.feature.creator.set.ModelChooseDialog
import com.kuaigeglobal.epnova.feature.editor.EditStoryboardVideoActivity
import com.kuaigeglobal.epnova.feature.works.MyCreationsFragment
import com.kuaigeglobal.epnova.feature.series.SeriesActivity
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getParentViewModel
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.launch

/**
 * 分镜设计
 */
class StoryboardDesignFragment :
    BaseViewModelFragment<FragmentStoryboardDesignBinding, StoryboardDesignViewModel>(), OnBackPressedHandler {

    companion object {
        fun newInstance() = StoryboardDesignFragment()
    }

    private val hostVm by lazy { getParentViewModel<StoryEditorViewModel>() }
    val from by lazy { hostVm?.from }

    override fun onPrepareView() {
        super.onPrepareView()
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.CREATE,
            FirebaseEvent.Action.STORYBOARD_SHOW
        )
        mRealVM.currentCreationModel.observe(this) {
            hostVm?.currentCreationModel?.value = it
        }
        hostVm?.switchModelEvent?.observe(this) {
            if (it == null || it == 0L) return@observe
            if (mRealVM.generating) {
                R.string.storyboard_description_generating_please_wait.getResString().showToast()
                return@observe
            }
            ModelChooseDialog.newInstance(mRealVM.currentCreationModel.value?.configCode, mRealVM.creationModels.value).apply {
                onSuccess = { model ->
                    this@StoryboardDesignFragment.mRealVM.let { vm ->
                        vm.switchModel(model) {
                            vm.step.set(StoryboardDesignViewModel.STEP_INIT)
                            vm.retry()
                            dismissAllowingStateLoss()
                        }
                    }
                }
            }
                .show(childFragmentManager)
        }
        binding.root.applySmartFocusManager()
        mRealVM.workId = hostVm?.workId
        mRealVM.storyId = hostVm?.storyId
        mRealVM.taskId = hostVm?.taskId
        mRealVM.step.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                Global.refreshCoinsAndPoints()
            }
        })
        lifecycleScope.launch {
            mRealVM.getModels()
        }
        mRealVM.retry()
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnNext -> {
                EventManager.onFirebaseTrack(
                    FirebaseEvent.Category.CREATE,
                    FirebaseEvent.Action.STORYBOARD_GENERATE_CLICK,
                    if (mRealVM.currentCreationModel.value?.isNarration() == true) {
                        FirebaseEvent.Value.MODEL_NARRATION_COMIC
                    } else {
                        FirebaseEvent.Value.MODEL_DYNAMIC_COMIC
                    }
                )
                mRealVM.clickNext { workId, storyId, taskId ->
                    EditStoryboardVideoActivity.start(
                        hostVm?.title?.value?.let {
                            "$it：${R.string.episode_number_format.getResString(hostVm?.chapterIndex ?: 1)}"
                        },
                        workId,
                        storyId,
                        hostVm?.videoRatio,
                        null,
                        from = from,
                        model = hostVm?.model
                    )
                    activity?.finish()
                }
            }
        }
    }

    override fun handleBackPressed(): Boolean {
        if (mRealVM.notSaved()) {
            activity?.showCommonDialog(
                tip = getString(R.string.save_storyboard_changes_prompt),
                cancelText = R.string.discard.getResString(),
                cancel = {
                    toSeries()
                },
                confirmText = R.string.save_and_exit.getResString(),
                confirm = {
                    mRealVM.saveAndFinish {
                        toSeries()
                    }
                })
            return true
        }
        if (mRealVM.generating) {
            getString(R.string.ai_generating_auto_saved).showToast()
        } else {
            getString(R.string.auto_saved_as_draft).showToast()
        }
        toSeries()
        return true
    }

    fun toSeries() {
        if (hostVm?.model == DramaGenerateConstants.MODEL_STORY_SINGLE) {
            if (from != StoryEditorActivity.FROM_MY_WORKS) {
                MyCreationsFragment.start()
            } else {
                mActivity.finish()
            }
        } else {
            if (from != StoryEditorActivity.FROM_SERIES) {
                SeriesActivity.start(mRealVM.workId)
            }
            mActivity.finish()
        }
    }

}

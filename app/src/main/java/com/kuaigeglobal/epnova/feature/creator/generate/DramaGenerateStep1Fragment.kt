package com.kuaigeglobal.epnova.feature.creator.generate

import android.os.Bundle
import com.blankj.utilcode.util.ActivityUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.adapter.QuickAdapter
import com.kuaigeglobal.epnova.core.manager.AppEvent
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.databinding.FragmentDramaGenerateStep1Binding
import com.kuaigeglobal.epnova.feature.creator.CreationRootActivity
import com.kuaigeglobal.epnova.feature.creator.StoryReuploadDialog
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.works.MyWorksFragment
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast


/**
 * 故事梗概生成
 */
class DramaGenerateStep1Fragment :
    BaseViewModelFragment<FragmentDramaGenerateStep1Binding, DramaGenerateStep1ViewModel>(), OnBackPressedHandler {

    companion object {
        const val TAG = "DramaGenerateStep1Fragment"
        fun newInstance(): DramaGenerateStep1Fragment {
            val fragment = DramaGenerateStep1Fragment()
            val bundle = Bundle()
            fragment.arguments = bundle
            return fragment
        }
    }

    val hostVm by lazy { getViewModelOfActivity<StoryEditorViewModel>() }
    var episodeAdapter = QuickAdapter<ItemStorySummaryGenerateVm>(R.layout.item_story_summary_generate, emptyList())
    private var showBackToast = false
    val from by lazy { hostVm?.from }

    override fun onPrepareView() {
        super.onPrepareView()
        EventManager.onAppTrack(AppEvent.SF_VIEW_STORY_OUTLINE_PAGE)
        activity?.intent?.let {
            mRealVM.workId = it.getStringExtra(Constants.Extra.WORK_ID)
            mRealVM.model = it.getIntExtra(Constants.Extra.MODE, 0)
            mRealVM.refreshDisplayContent()
            mRealVM.pollSummary()
            showBackToast = true
        }

        binding.recyclerView.setAdapter(episodeAdapter)
        mRealVM.storySummaryResult.observe(this) {
            if (it == null) return@observe
            showBackToast = false
            if (it.isSuccess()) {
                episodeAdapter.submitList(it.result?.structureList?.map { info ->
                    return@map ItemStorySummaryGenerateVm(info)
                })
            } else if (it.isSensitive()) {
                StoryReuploadDialog.show(it.novelFileName, it.novelCharCount, it.toastTextAuditFailed) {
                    reuploadStory()
                }
            }
        }

        mRealVM.mWorkName.observe(this) {
            if (hostVm?.title?.value.isNullOrEmpty()) {
                hostVm?.title?.value = it
            }
        }
        hostVm?.title?.observe(this) {
            if (it != mRealVM.mWorkName.value) {
                mRealVM.mWorkName.value = it
                mRealVM.saveTitle()
            }
        }
        binding.btnRegenerate.setOnSingleClickListener {
            if (mRealVM.model == DramaGenerateConstants.MODEL_STORY_DOCUMENTS) {
                reuploadStory()
            } else {
                mRealVM.retrySummary {
                    mRealVM.pollSummary()
                    showBackToast = true
                }
            }
        }
        binding.btnNextDisable.setOnSingleClickListener {
            getString(R.string.story_generating_please_wait).showToast()
        }
        binding.btnNext.setOnSingleClickListener {
            mRealVM.saveTitle {
                hostVm?.nextStep(2, mRealVM.workId, mRealVM.storySummaryResult.value?.result?.getFirstStoryId())
            }
        }
    }


    /**
     * 重新上传/重试  删除本地草稿，打开小说文档上传界面重新上传
     */
    fun reuploadStory() {
        mRealVM.reuploadStory {
            if (!ActivityUtils.isActivityExistsInStack(CreationRootActivity::class.java)) {
                CreationRootActivity.start(mActivity, 1)
            }
            mActivity.finish()
        }
    }


    override fun handleBackPressed(): Boolean {
        if (mRealVM.isSummaryLoading.get()) {
            getString(R.string.ai_generating_auto_saved).showToast()
        } else {
            getString(R.string.auto_saved_as_draft).showToast()
        }
        if (from != StoryEditorActivity.FROM_MY_WORKS) {
            MyWorksFragment.start()
        } else {
            mActivity.finish()
        }
        return true
    }
}
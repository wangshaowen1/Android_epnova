package com.kuaigeglobal.epnova.feature.creator.generate

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ActivityUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.adapter.QuickAdapter
import com.kuaigeglobal.epnova.common.ext.showConfirmPrompt
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.databinding.FragmentDramaGenerateStep1Binding
import com.kuaigeglobal.epnova.feature.creator.CreationRootActivity
import com.kuaigeglobal.epnova.feature.creator.StoryReuploadDialog
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.editor.info.InputDialog
import com.kuaigeglobal.epnova.feature.mine.child.MyCreationsFragment
import com.kuaigeglobal.epnova.feature.recharge.PowerRechargeDialog
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
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.CREATE,
            FirebaseEvent.Action.SYNOPSIS_SHOW
        )
        activity?.intent?.let {
            mRealVM.workId = it.getStringExtra(Constants.Extra.WORK_ID)
            mRealVM.model = it.getIntExtra(Constants.Extra.MODE, 0)
            mRealVM.refreshDisplayContent()
            mRealVM.pollSummary()
            showBackToast = true
        }
        binding.recyclerView.setAdapter(episodeAdapter)
        episodeAdapter.setOnItemClickListener { _, _, position ->
            editWarning {
                mRealVM.storySummaryResult.value?.result?.structureList?.get(position)?.let {
                    InputDialog.show(InputDialog.TYPE_EPISODE_SUMMARY, mRealVM.workId, it.storyId, it.storySummary, it.storyTitle) { text ->
                        it.storySummary = text
                        episodeAdapter.getItem(position)?.updateData(it.copy())
                    }
                }
            }
        }
        mRealVM.storySummaryResult.observe(this) {
            if (it == null) return@observe
            showBackToast = false
            if (it.isSuccess()) {
                setTextWithEditIcon(binding.tvStoryDesc, it.result?.workDesc ?: "")
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
            if (!it.isNullOrEmpty() && it != mRealVM.mWorkName.value) {
                mRealVM.mWorkName.value = it
                mRealVM.saveTitle()
            }
        }
        mRealVM.showRechargeDialogEvent.observe(this) {
            PowerRechargeDialog.show(
                showToast = true,
                toastMessage = getString(R.string.insufficient_balance_recharge_to_continue),
                rechargeTab = PowerRechargeDialog.TAB_CREDITS,
                onDismiss = { recharged ->
                    mRealVM.onRecharged(recharged)
                }
            )
        }
        binding.btnRecharge.setOnSingleClickListener {
            PowerRechargeDialog.show(
                rechargeTab = PowerRechargeDialog.TAB_CREDITS,
                onDismiss = { recharged ->
                    mRealVM.onRecharged(recharged)
                }
            )
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
        binding.tvStoryDesc.setOnSingleClickListener {
            editWarning {
                InputDialog.show(InputDialog.TYPE_WORK_DESC, mRealVM.workId, content = mRealVM.storySummaryResult.value?.result?.workDesc) {
                    mRealVM.storySummaryResult.value?.result?.workDesc = it
                    setTextWithEditIcon(binding.tvStoryDesc, it)
                }
            }
        }
        binding.btnNext.setOnSingleClickListener {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.CREATE,
                FirebaseEvent.Action.SYNOPSIS_NEXT_CLICK
            )
            mRealVM.saveTitle {
                hostVm?.nextStep(2, mRealVM.workId, mRealVM.storySummaryResult.value?.result?.getFirstStoryId())
            }
        }
    }

    var hasEditWarning = false

    fun editWarning(callback: () -> Unit) {
        if (hasEditWarning) {
            callback()
            return
        }
        hasEditWarning = true
        mActivity.showConfirmPrompt(
            getString(R.string.edit_text_warning),
            confirmText = getString(R.string.continue_editing)
        ) {
            callback()
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

    fun setTextWithEditIcon(textView: TextView, content: String) {
        val editIcon = ContextCompat.getDrawable(textView.context, R.drawable.icon_edit_full)!!

        // 设置图标大小（与文字大小匹配）
        val textSizePx = textView.textSize.toInt()
        editIcon.setBounds(0, 0, textSizePx, textSizePx)

        // 构建 SpannableString
        // 末尾加空格占位 + 图标
        val spaceAndIcon = "  " // 两个空格：一个间距 + 一个给ImageSpan占位
        val fullText = content + spaceAndIcon

        val spannable = SpannableStringBuilder(fullText)

        // 将最后一个字符替换为图片Span
        val imageSpan = ImageSpan(editIcon, ImageSpan.ALIGN_BOTTOM)
        spannable.setSpan(
            imageSpan,
            fullText.length - 1,  // 最后一个字符位置
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textView.text = spannable
    }

    override fun handleBackPressed(): Boolean {
        if (mRealVM.isSummaryLoading.get()) {
            getString(R.string.ai_generating_auto_saved).showToast()
        } else {
            getString(R.string.auto_saved_as_draft).showToast()
        }
        if (from != StoryEditorActivity.FROM_MY_WORKS) {
            MyCreationsFragment.start()
        } else {
            mActivity.finish()
        }
        return true
    }
}

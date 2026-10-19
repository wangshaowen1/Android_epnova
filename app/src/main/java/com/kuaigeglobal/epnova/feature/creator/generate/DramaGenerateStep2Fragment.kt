package com.kuaigeglobal.epnova.feature.creator.generate

import android.os.Bundle
import android.text.Editable
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.SimpleTextWatcher
import com.kuaigeglobal.epnova.common.ext.applySmartFocusManager
import com.kuaigeglobal.epnova.common.ext.showConfirmPrompt
import com.kuaigeglobal.epnova.core.databinding.setTextWithEditIcon
import com.kuaigeglobal.epnova.core.ext.sensitiveWordCheck
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.request.RatingEnterEnum
import com.kuaigeglobal.epnova.databinding.FragmentDramaGenerateStep2Binding
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.editor.info.InputDialog
import com.kuaigeglobal.epnova.feature.rating.AppRatingDialog
import com.kuaigeglobal.epnova.feature.works.MyCreationsFragment
import com.kuaigeglobal.epnova.feature.series.SeriesActivity
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.launch

/**
 *  故事剧本生成
 */
class DramaGenerateStep2Fragment :
    BaseViewModelFragment<FragmentDramaGenerateStep2Binding, DramaGenerateStep2ViewModel>(), OnBackPressedHandler {

    companion object {
        fun newInstance(): DramaGenerateStep2Fragment {
            val fragment = DramaGenerateStep2Fragment()
            val bundle = Bundle()
            fragment.arguments = bundle
            return fragment
        }
    }

    val hostVm by lazy { getViewModelOfActivity<StoryEditorViewModel>() }
    val from by lazy { hostVm?.from }

    private var showBackToast = false
    private var sensitiveScrollRunnable: Runnable? = null
    private var pendingScriptAutoScroll = false

    override fun onPrepareView() {
        super.onPrepareView()
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.CREATE,
            FirebaseEvent.Action.SCRIPT_SHOW
        )
        binding.root.applySmartFocusManager()
        parseIntentData()
        initView()
        //数据更新
        dataUpdate()
    }

    override fun onResume() {
        super.onResume()
        // 创作完第一集，进入第二集剧本生成页时判断，不等待生成完成；仅限第二集
        if (mRealVM.chapterIndex == 2) {
            AppRatingDialog.show(parentFragmentManager, RatingEnterEnum.SCRIPT)
        }
    }

    private fun parseIntentData() {
        activity?.intent?.let { intent ->
            mRealVM.workId.value = hostVm?.workId ?: intent.getStringExtra(Constants.Extra.WORK_ID)
            mRealVM.episodeId = hostVm?.storyId ?: intent.getStringExtra(Constants.Extra.STORY_ID)
            mRealVM.workType = intent.getIntExtra(Constants.Extra.WORK_TYPE, 0)
            mRealVM.previousStoryId = hostVm?.previousStoryId
            mRealVM.continueInputContent = intent.getStringExtra(Constants.Extra.CONTENT)
            mRealVM.model = intent.getIntExtra(Constants.Extra.MODE, 0)
            mRealVM.chapterIndex = intent.getIntExtra(Constants.Extra.CHAPTER_INDEX, 1)

            scriptGenerate()
        }
    }

    private fun scriptGenerate(retry: Boolean = false) {
        if (mRealVM.useOldScriptBusiness.get()) {
            if (mRealVM.isNovelDocuments()) {
                if (!retry) {
                    startPolling()
                    return
                }
                mRealVM.createScriptTask { startPolling() }
            } else {
                mRealVM.createScriptTask(retry) { startPolling() }
            }
        } else {
            mRealVM.createScriptTask(retry) {
                mRealVM.startScriptSession(resetContent = true)
                showBackToast = true
            }
        }
    }

    private fun startPolling() {
        mRealVM.pollOldScript()
        showBackToast = true
    }

    private fun initView() {
        binding.inputView.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mRealVM.input.value = s.toString()
            }
        })
        binding.btnRegenerate.setOnSingleClickListener {
            scriptGenerate(true)
        }
        binding.retryBtn.setOnSingleClickListener {
            scriptGenerate(true)
        }
        binding.btnDelete.setOnSingleClickListener {
            if (mRealVM.isScriptLoading.get() || mRealVM.isScriptStreaming.get()) {
                return@setOnSingleClickListener
            }
            mRealVM.input.value = ""
        }

        binding.btnNext.setOnSingleClickListener {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.CREATE,
                FirebaseEvent.Action.SCRIPT_NEXT_CLICK
            )
            handleNextStep()
        }
        setupTouchInterceptor()

        //这一集简纲编辑
        binding.tvStoryIntroduction.setOnSingleClickListener {
            if (mRealVM.isScriptLoading.get() || mRealVM.isScriptStreaming.get()) {
                return@setOnSingleClickListener
            }
            editWarning {
                mRealVM.storyScriptResult.value?.storySummary?.let {
                    InputDialog.show(
                        InputDialog.TYPE_SCRIPT_OUTLINE,
                        workId = mRealVM.workId.value,
                        storyId = mRealVM.episodeId,
                        content = it,
                        title = mRealVM.storyScriptResult.value?.storyTilte
                    ) { text ->
                        mRealVM.storyScriptResult.value?.storySummary = text
                        setTextWithEditIcon(binding.tvStoryIntroduction, text)
                        scriptGenerate(retry = true)
                    }
                }
            }
        }
    }

    var isStoryChange = false
    private fun editWarning(callback: () -> Unit) {
        if (isStoryChange) {
            callback()
            return
        }
        mActivity.showConfirmPrompt(
            getString(R.string.edit_episode_script_refresh_warning),
            confirmText = getString(R.string.continue_editing)
        ) {
            isStoryChange = true
            callback()
        }
    }



    private fun handleNextStep() {
        viewLifecycleOwner.lifecycleScope.launch {
            sensitiveWordCheck(mRealVM.input.value, {
                sensitiveHighlightedText = null
                mRealVM.confirmGenerate { workId, storyId, taskId ->
                    hostVm?.nextStep(3, workId, storyId, taskId)
                }
            }, {
                sensitiveHighlightedText = it
                it?.let { spanned ->
                    binding.inputView.text = spanned
                    scrollToFirstSensitiveWord(spanned)
                }
            })
        }
    }

    private fun scrollToFirstSensitiveWord(spannedText: CharSequence) {
        val spanned = spannedText as? Spanned ?: return
        val inputView = binding.inputView
        val scrollContainer = binding.scrollContainer

        sensitiveScrollRunnable?.let { inputView.removeCallbacks(it) }
        val scrollRunnable = Runnable {
            if (!inputView.isAttachedToWindow || !scrollContainer.isAttachedToWindow) return@Runnable

            val layout = inputView.layout ?: return@Runnable
            val spans = spanned.getSpans(0, spanned.length, ForegroundColorSpan::class.java)
            if (spans.isEmpty()) return@Runnable

            val firstSpanStart = spans.minOf { spanned.getSpanStart(it) }
            val line = layout.getLineForOffset(firstSpanStart)
            val lineTop = layout.getLineTop(line)

            // 通过屏幕坐标计算 inputView 相对于 scrollContainer 内容区域的偏移
            val scrollViewOffset = IntArray(2)
            scrollContainer.getLocationOnScreen(scrollViewOffset)
            val inputViewOffset = IntArray(2)
            inputView.getLocationOnScreen(inputViewOffset)

            val inputViewRelativeTop = inputViewOffset[1] - scrollViewOffset[1] + scrollContainer.scrollY
            val lineAbsoluteY = inputViewRelativeTop + inputView.paddingTop + lineTop

            // 将标红行居中显示
            val lineHeight = layout.getLineBottom(line) - lineTop
            val centeredY = lineAbsoluteY + lineHeight / 2 - scrollContainer.height / 2
            scrollContainer.smoothScrollTo(0, maxOf(0, centeredY))
        }
        sensitiveScrollRunnable = scrollRunnable
        inputView.post(scrollRunnable)
    }

    private var sensitiveHighlightedText: CharSequence? = null

    var isNotified = false

    private fun setupTouchInterceptor() {
        binding.inputView.setOnSingleClickListener {
            if (mRealVM.isScriptLoading.get() || mRealVM.isScriptStreaming.get()) {
                return@setOnSingleClickListener
            }

            if (mRealVM.input.value?.trim().isNullOrEmpty() || isNotified) {
                enterEditMode()
                return@setOnSingleClickListener
            }
            isNotified = true
            mActivity.showConfirmPrompt(
                getString(R.string.edit_text_warning),
                confirmText = getString(R.string.continue_editing)
            ) {
                enterEditMode()
            }
        }
    }

    private fun enterEditMode() {
        mRealVM.input.value.let {
            InputDialog.show(
                type = InputDialog.TYPE_EPISODE_CONTENT,
                content = it,
                highlightedContent = sensitiveHighlightedText
            ) { text ->
                sensitiveHighlightedText = null
                mRealVM.input.value = text
            }
        }
    }

    private fun dataUpdate() {
        mRealVM.input.observe(viewLifecycleOwner) { text ->
            val currentBinding = bindingNullable ?: return@observe
            val newText = text ?: ""
            val currentText = currentBinding.inputView.text.toString()

            if (newText != currentText) {
                updateInputViewContent(currentBinding, newText, currentText)
                handleTextScrollAnimation(currentBinding)
            }
        }

        mRealVM.storyScriptResult.observe(viewLifecycleOwner) {
            showBackToast = false
            updateEpisodeTitle(hostVm?.title?.value)
        }

        mRealVM.mWorkName.observe(viewLifecycleOwner) {
            if (hostVm?.title?.value.isNullOrEmpty()) {
                hostVm?.title?.value = it
            }
        }
        hostVm?.title?.observe(viewLifecycleOwner) {
            if (it != mRealVM.mWorkName.value) {
                mRealVM.mWorkName.value = it
                updateEpisodeTitle(it)
            }
        }
    }

    private fun updateEpisodeTitle(workTitle: String?) {
        if (workTitle.isNullOrEmpty()) return
        val title = if (mRealVM.model == DramaGenerateConstants.MODEL_STORY_SINGLE) {
            "《$workTitle》"
        } else {
            "《$workTitle》${getString(R.string.episode_number_format, mRealVM.chapterIndex ?: 1)}"
        }
        mRealVM.episodeTitle.set(title)
    }

    private fun updateInputViewContent(
        currentBinding: FragmentDramaGenerateStep2Binding,
        newText: String,
        currentText: String
    ) {
        if (newText.startsWith(currentText)) {
            val appendText = newText.substring(currentText.length)
            currentBinding.inputView.append(appendText)
        } else {
            currentBinding.inputView.setText(newText)
        }
    }

    private fun handleTextScrollAnimation(currentBinding: FragmentDramaGenerateStep2Binding) {
        if (pendingScriptAutoScroll) return
        pendingScriptAutoScroll = true
        val inputView = currentBinding.inputView
        val scrollContainer = currentBinding.scrollContainer
        scrollContainer.postOnAnimation {
            pendingScriptAutoScroll = false
            if (bindingNullable !== currentBinding) return@postOnAnimation
            val layout = inputView.layout ?: return@postOnAnimation
            val lastLine = (inputView.lineCount - 1).coerceAtLeast(0)
            val lastLineBottom = layout.getLineBottom(lastLine)
            val targetScrollY = inputView.top +
                    inputView.paddingTop +
                    lastLineBottom +
                    inputView.paddingBottom -
                    scrollContainer.height
            scrollContainer.scrollTo(0, maxOf(0, targetScrollY))
        }
    }

    override fun onDestroyView() {
        pendingScriptAutoScroll = false
        sensitiveScrollRunnable?.let { binding.inputView.removeCallbacks(it) }
        sensitiveScrollRunnable = null
        super.onDestroyView()
    }

    override fun handleBackPressed(): Boolean {
        if (mRealVM.isScriptLoading.get() || mRealVM.isScriptStreaming.get()) {
            getString(R.string.ai_generating_auto_saved).showToast()
        } else {
            getString(R.string.auto_saved_as_draft).showToast()
        }
        mRealVM.cancelScriptStream()
        if (mRealVM.model == DramaGenerateConstants.MODEL_STORY_SINGLE) {
            if (from != StoryEditorActivity.FROM_MY_WORKS) {
                MyCreationsFragment.start()
            } else {
                mActivity.finish()
            }
        } else {
            if (from != StoryEditorActivity.FROM_SERIES) {
                SeriesActivity.start(mRealVM.workId.value)
            }
            mActivity.finish()
        }
        return true
    }
}

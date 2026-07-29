package com.kuaigeglobal.epnova.feature.creator.generate

import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.Editable
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.animation.LinearInterpolator
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.SimpleTextWatcher
import com.kuaigeglobal.epnova.common.ext.applySmartFocusManager
import com.kuaigeglobal.epnova.common.ext.showConfirmPrompt
import com.kuaigeglobal.epnova.core.ext.sensitiveWordCheck
import com.kuaigeglobal.epnova.databinding.FragmentDramaGenerateStep2Binding
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.editor.info.InputDialog
import com.kuaigeglobal.epnova.feature.mine.child.MyCreationsFragment
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
    private var scrollAnimator: ObjectAnimator? = null

    override fun onPrepareView() {
        super.onPrepareView()
        binding.root.applySmartFocusManager()
        parseIntentData()
        initView()
        //数据更新
        dataUpdate()
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
        if (mRealVM.isNovelDocuments()) {
            if (retry) {
                mRealVM.createScriptTask { startPolling() }
                return
            }
            startPolling()
        } else {
            mRealVM.createScriptTask(retry) { startPolling() }
        }
    }

    private fun startPolling() {
        mRealVM.pollNovelScript()
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
            if (mRealVM.isScriptLoading.get()) return@setOnSingleClickListener
            mRealVM.input.value = ""
        }

        binding.btnNext.setOnSingleClickListener {
            handleNextStep()
        }
        setupTouchInterceptor()
    }

    private fun handleNextStep() {
        lifecycleScope.launch {
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
        binding.inputView.post {
            val layout = binding.inputView.layout ?: return@post
            val spanned = spannedText as? Spanned ?: return@post
            val spans = spanned.getSpans(0, spanned.length, ForegroundColorSpan::class.java)
            if (spans.isEmpty()) return@post

            val firstSpanStart = spans.minOf { spanned.getSpanStart(it) }
            val line = layout.getLineForOffset(firstSpanStart)
            val lineTop = layout.getLineTop(line)

            // 通过屏幕坐标计算 inputView 相对于 scrollContainer 内容区域的偏移
            val scrollViewOffset = IntArray(2)
            binding.scrollContainer.getLocationOnScreen(scrollViewOffset)
            val inputViewOffset = IntArray(2)
            binding.inputView.getLocationOnScreen(inputViewOffset)

            val inputViewRelativeTop = inputViewOffset[1] - scrollViewOffset[1] + binding.scrollContainer.scrollY
            val lineAbsoluteY = inputViewRelativeTop + binding.inputView.paddingTop + lineTop

            // 将标红行居中显示
            val lineHeight = layout.getLineBottom(line) - lineTop
            val centeredY = lineAbsoluteY + lineHeight / 2 - binding.scrollContainer.height / 2
            binding.scrollContainer.smoothScrollTo(0, maxOf(0, centeredY))
        }
    }

    private var sensitiveHighlightedText: CharSequence? = null

    var isNotified = false

    private fun setupTouchInterceptor() {
        binding.inputView.setOnSingleClickListener {
            if (mRealVM.isScriptLoading.get()) return@setOnSingleClickListener

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
        mRealVM.input.observe(this) { text ->
            val newText = text ?: ""
            val currentText = binding.inputView.text.toString()

            if (newText != currentText) {
                updateInputViewContent(newText, currentText)
                handleTextScrollAnimation()
            }
        }

        mRealVM.storyScriptResult.observe(this) {
            showBackToast = false
        }

        mRealVM.mWorkName.observe(this) {
            if (hostVm?.title?.value.isNullOrEmpty()) {
                hostVm?.title?.value = it
            }
        }
        hostVm?.title?.observe(this) {
            if (it != mRealVM.mWorkName.value) {
                mRealVM.mWorkName.value = it
                mRealVM.episodeTitle.set("《$it》${getString(R.string.episode_number_format, mRealVM.chapterIndex ?: 1)}")
            }
        }
    }

    private fun updateInputViewContent(newText: String, currentText: String) {
        // 增量追加优化：避免全量重绘引起的闪烁
        if (newText.startsWith(currentText)) {
            val appendText = newText.substring(currentText.length)
            binding.inputView.append(appendText)
        } else {
            binding.inputView.setText(newText)
        }
    }

    private fun handleTextScrollAnimation() {
        binding.inputView.post {
            val layout = binding.inputView.layout ?: return@post

            val contentHeight = layout.getLineTop(binding.inputView.lineCount)
            val targetScrollY = contentHeight + binding.inputView.paddingTop +
                    binding.inputView.paddingBottom - binding.inputView.height

            if (targetScrollY > 0) {
                // 取消上一次未完成的动画，保持流式输出平滑
                scrollAnimator?.cancel()
                scrollAnimator = ObjectAnimator.ofInt(
                    binding.inputView, "scrollY", binding.inputView.scrollY, targetScrollY
                ).apply {
                    duration = 60
                    interpolator = LinearInterpolator()
                    start()
                }
            } else {
                binding.inputView.scrollTo(0, 0)
            }
        }
    }

    override fun onDestroy() {
        scrollAnimator?.cancel()
        scrollAnimator = null
        super.onDestroy()
    }

    override fun handleBackPressed(): Boolean {
        if (mRealVM.isScriptLoading.get()) {
            getString(R.string.ai_generating_auto_saved).showToast()
        } else {
            getString(R.string.auto_saved_as_draft).showToast()
        }
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

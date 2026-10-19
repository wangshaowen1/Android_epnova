package com.kuaigeglobal.epnova.feature.creator.generate

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ActivityUtils
import com.fluid.afm.markdown.widget.PrinterMarkDownTextView
import com.fluid.afm.styles.BlockQuoteStyle
import com.fluid.afm.styles.CodeStyle
import com.fluid.afm.styles.MarkdownStyles
import com.fluid.afm.styles.ParagraphStyle
import com.fluid.afm.styles.TableStyle
import com.fluid.afm.styles.TitleStyle
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.adapter.QuickAdapter
import com.kuaigeglobal.epnova.common.ext.showConfirmPrompt
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.request.StoryOutlineInfo
import com.kuaigeglobal.epnova.databinding.FragmentDramaGenerateStep1Binding
import com.kuaigeglobal.epnova.feature.creator.CreationRootActivity
import com.kuaigeglobal.epnova.feature.creator.StoryReuploadDialog
import com.kuaigeglobal.epnova.feature.editor.info.InputDialog
import com.kuaigeglobal.epnova.feature.works.MyCreationsFragment
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
        private const val SUMMARY_MARKDOWN_RENDER_DELAY_MS = 64L
        private const val SUMMARY_MARKDOWN_NORMAL_INTERVAL_MS = 2
        private const val SUMMARY_MARKDOWN_NORMAL_CHUNK_SIZE = 6
        private const val SUMMARY_MARKDOWN_FAST_INTERVAL_MS = 1
        private const val SUMMARY_MARKDOWN_LAG_FAST_CHARS = 240
        private const val SUMMARY_MARKDOWN_LAG_CATCH_UP_CHARS = 900
        private const val SUMMARY_MARKDOWN_APPEND_CATCH_UP_CHARS = 240

        fun newInstance(): DramaGenerateStep1Fragment {
            val fragment = DramaGenerateStep1Fragment()
            val bundle = Bundle()
            fragment.arguments = bundle
            return fragment
        }
    }

    val hostVm by lazy { getViewModelOfActivity<StoryEditorViewModel>() }
    private val markDownTextView by lazy { PrinterMarkDownTextView(mActivity) }
    var episodeAdapter = QuickAdapter<ItemStorySummaryGenerateVm>(R.layout.item_story_summary_generate, emptyList())
    //故事大纲 故事简纲列表
    private val storyOutlineAdapter = QuickAdapter<ItemStoryOutlineGenerateVm>(R.layout.item_story_outline_generate, emptyList())
    private var showBackToast = false
    val from by lazy { hostVm?.from }
    private var pendingSummaryMarkdown: String? = null
    private var summaryMarkdownDirectRender = false
    private val summaryMarkdownRenderRunnable = Runnable {
        pendingSummaryMarkdown?.let { renderSummaryMarkdownNow(it) }
        pendingSummaryMarkdown = null
    }

    override fun onPrepareView() {
        super.onPrepareView()
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.CREATE,
            FirebaseEvent.Action.SYNOPSIS_SHOW
        )
        activity?.intent?.let {
            mRealVM.workId = it.getStringExtra(Constants.Extra.WORK_ID)
            mRealVM.model = it.getIntExtra(Constants.Extra.MODE, 0)
            if (mRealVM.useOldSummaryBusiness()) {
                mRealVM.pollSummary()
            } else {
                mRealVM.startSummarySession()
            }
            showBackToast = true
        }
        setupSummaryMarkdownView()
        binding.recyclerView.setAdapter(episodeAdapter)
        episodeAdapter.setOnItemClickListener { _, _, position ->
            if (mRealVM.isSummaryLoading.get() || mRealVM.isSummaryStreaming.get()) {
                getString(R.string.story_generating_please_wait).showToast()
                return@setOnItemClickListener
            }
            editWarning {
                mRealVM.storySummaryResult.value?.result?.structureList?.get(position)?.let {
                    InputDialog.show(InputDialog.TYPE_EPISODE_SUMMARY, mRealVM.workId, it.storyId, it.storySummary, it.storyTitle) { text ->
                        it.storySummary = text
                        episodeAdapter.getItem(position)?.updateData(it.copy())
                    }
                }
            }
        }
        //故事大纲 故事简纲列表设置
        binding.recyclerViewStoryOutline.setAdapter(storyOutlineAdapter)
        storyOutlineAdapter.setOnItemClickListener { _, _, position ->
            if (mRealVM.isSummaryLoading.get() || mRealVM.isSummaryStreaming.get()) {
                getString(R.string.story_generating_please_wait).showToast()
                return@setOnItemClickListener
            }
            editWarning {
                mRealVM.storySummaryResult.value?.result?.stages?.get(position)?.let {
                    InputDialog.show(
                        type = InputDialog.TYPE_STORY_OUTLINE,
                        workId = mRealVM.workId,
                        settingId = it.settingId,
                        content = it.content,
                        extra = it.name
                    ) { text ->
                        it.content = text
                        storyOutlineAdapter.getItem(position)?.updateData(it.copy())
                    }
                }
            }
        }

        mRealVM.summaryStreamingText.observe(viewLifecycleOwner) {
            if (mRealVM.isSummaryStreaming.get()) {
                renderSummaryMarkdown(
                    content = it.orEmpty(),
                    renderInstantly = mRealVM.consumeSummaryStreamCacheRestored()
                )
            } else if (it.isNullOrEmpty()) {
                stopSummaryMarkdown()
            }
        }
        mRealVM.storySummaryResult.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            showBackToast = false
            if (it.isSuccess()) {
                stopSummaryMarkdown()
                setTextWithEditIcon(binding.tvStoryDesc, it.result?.workDesc ?: "")
                episodeAdapter.submitList(it.result?.structureList?.map { info ->
                    return@map ItemStorySummaryGenerateVm(info)
                })
                storyOutlineAdapter.submitList( it.result?.stages?.map { info ->
                    return@map ItemStoryOutlineGenerateVm(info)
                })
            } else if (it.isSensitive()) {
                StoryReuploadDialog.show(it.novelFileName, it.novelCharCount, it.toastTextAuditFailed) {
                    reuploadStory()
                }
            }
        }

        mRealVM.mWorkName.observe(viewLifecycleOwner) {
            if (hostVm?.title?.value.isNullOrEmpty()) {
                hostVm?.title?.value = it
            }
        }
        hostVm?.title?.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty() && it != mRealVM.mWorkName.value) {
                mRealVM.mWorkName.value = it
                mRealVM.saveTitle()
            }
        }
        mRealVM.showRechargeDialogEvent.observe(viewLifecycleOwner) {
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
            stopSummaryMarkdown()
            if (mRealVM.isStoryDocumentsUploadMode()) {
                reuploadStory()
            } else if (mRealVM.useOldSummaryBusiness()) {
                mRealVM.pollSummary()
                showBackToast = true
            } else {
                mRealVM.retrySummary {
                    mRealVM.startSummarySession(resetContent = true)
                    showBackToast = true
                }
            }
        }
        binding.btnNextDisable.setOnSingleClickListener {
            getString(R.string.story_generating_please_wait).showToast()
        }
        binding.tvStoryDesc.setOnSingleClickListener {
            if (mRealVM.isSummaryLoading.get() || mRealVM.isSummaryStreaming.get()) {
                getString(R.string.story_generating_please_wait).showToast()
                return@setOnSingleClickListener
            }
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

    private fun setupSummaryMarkdownView() {
        val layoutManager = LinearLayoutManager(
            mActivity,
            LinearLayoutManager.VERTICAL,
            false
        ).apply {
            stackFromEnd = true
        }
        binding.streamMarkdownContainer.layoutManager = layoutManager
        binding.streamMarkdownContainer.adapter =
            object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int
                ): RecyclerView.ViewHolder {
                    (markDownTextView.parent as? ViewGroup)?.removeView(markDownTextView)
                    return object : RecyclerView.ViewHolder(markDownTextView) {}
                }

                override fun onBindViewHolder(
                    holder: RecyclerView.ViewHolder,
                    position: Int
                ) = Unit

                override fun getItemCount(): Int = 1
            }
        markDownTextView.init(getSummaryMarkdownStyles(), null)
        markDownTextView.setPrintParams(
            SUMMARY_MARKDOWN_NORMAL_INTERVAL_MS,
            SUMMARY_MARKDOWN_NORMAL_CHUNK_SIZE
        )
    }

    private fun renderSummaryMarkdown(content: String, renderInstantly: Boolean = false) {
        if (content.isEmpty() || (!summaryMarkdownDirectRender && !markDownTextView.isStarted)) {
            pendingSummaryMarkdown = null
            binding.streamMarkdownContainer.removeCallbacks(summaryMarkdownRenderRunnable)
            renderSummaryMarkdownNow(content, renderInstantly)
            return
        }
        pendingSummaryMarkdown = content
        binding.streamMarkdownContainer.removeCallbacks(summaryMarkdownRenderRunnable)
        binding.streamMarkdownContainer.postDelayed(
            summaryMarkdownRenderRunnable,
            SUMMARY_MARKDOWN_RENDER_DELAY_MS
        )
    }

    private fun renderSummaryMarkdownNow(content: String, renderInstantly: Boolean = false) {
        val fullText = content.replace("\\n", "\n")
        if (fullText.isEmpty()) {
            stopSummaryMarkdown()
            return
        }

        if (renderInstantly) {
            stopSummaryMarkdownPrinting()
            summaryMarkdownDirectRender = true
            markDownTextView.setMarkdownText(fullText)
            return
        }
        if (summaryMarkdownDirectRender) {
            markDownTextView.setMarkdownText(fullText)
            return
        }

        val currentText = markDownTextView.originText ?: ""
        updateSummaryMarkdownPrintParams(fullText.length)
        if (fullText == currentText) return

        if (currentText.isNotEmpty() &&
            fullText.length > currentText.length &&
            fullText.startsWith(currentText)
        ) {
            val appendText = fullText.substring(currentText.length)
            if (markDownTextView.isStarted) {
                if (shouldCatchUpSummaryMarkdown(fullText.length, currentText.length)) {
                    renderSummaryMarkdownSnapshot(fullText)
                } else {
                    markDownTextView.appendPrinting(appendText)
                }
            } else {
                markDownTextView.setMarkdownText(fullText)
            }
            return
        }

        if (!markDownTextView.isStarted) {
            summaryMarkdownDirectRender = false
            markDownTextView.startPrinting(fullText)
            return
        }

        if (fullText != currentText) {
            stopSummaryMarkdownPrinting()
            summaryMarkdownDirectRender = false
            markDownTextView.startPrinting(fullText)
        }
    }

    private fun updateSummaryMarkdownPrintParams(targetLength: Int) {
        val lagChars = (targetLength - markDownTextView.printIndex).coerceAtLeast(0)
        val chunkSize = when {
            lagChars >= SUMMARY_MARKDOWN_LAG_CATCH_UP_CHARS -> 72
            lagChars >= SUMMARY_MARKDOWN_LAG_FAST_CHARS -> 24
            else -> SUMMARY_MARKDOWN_NORMAL_CHUNK_SIZE
        }
        val interval = if (lagChars >= SUMMARY_MARKDOWN_LAG_FAST_CHARS) {
            SUMMARY_MARKDOWN_FAST_INTERVAL_MS
        } else {
            SUMMARY_MARKDOWN_NORMAL_INTERVAL_MS
        }
        markDownTextView.setPrintParams(interval, chunkSize)
    }

    private fun shouldCatchUpSummaryMarkdown(
        targetLength: Int,
        currentTargetLength: Int
    ): Boolean {
        val lagChars = (targetLength - markDownTextView.printIndex).coerceAtLeast(0)
        val appendChars = (targetLength - currentTargetLength).coerceAtLeast(0)
        return lagChars >= SUMMARY_MARKDOWN_LAG_CATCH_UP_CHARS &&
                appendChars >= SUMMARY_MARKDOWN_APPEND_CATCH_UP_CHARS
    }

    private fun renderSummaryMarkdownSnapshot(fullText: String) {
        stopSummaryMarkdownPrinting()
        summaryMarkdownDirectRender = true
        markDownTextView.setMarkdownText(fullText)
    }

    private fun stopSummaryMarkdown() {
        pendingSummaryMarkdown = null
        summaryMarkdownDirectRender = false
        binding.streamMarkdownContainer.removeCallbacks(summaryMarkdownRenderRunnable)
        stopSummaryMarkdownPrinting()
    }

    private fun stopSummaryMarkdownPrinting() {
        if (markDownTextView.isStarted) {
            markDownTextView.stopPrinting("")
        }
    }

    private fun getSummaryMarkdownStyles(): MarkdownStyles {
        return MarkdownStyles().apply {
            paragraphStyle(
                ParagraphStyle.create()
                    .fontColor(-2829100)
            )
            blockQuoteStyle(
                BlockQuoteStyle.create()
                    .lineColor(-12303292)
                    .fontColor(-4473925)
            )
            tableStyle(
                TableStyle.create()
                    .fontColor(-2829100)
                    .titleFontColor(-1)
                    .titleBackgroundColor(-14540254)
                    .bodyBackgroundColor(-13619152)
                    .drawBorder(false)
            )
            setTitleStyle(0, TitleStyle.create(1.5F))
                .setTitleStyle(1, TitleStyle.create(1.3F))
                .setTitleStyle(2, TitleStyle.create(1.1F))
            codeStyle(
                CodeStyle.create()
                    .inlineFontColor(-2171170)
                    .inlineCodeBackgroundColor(855638016)
                    .codeFontColor(-2829100)
                    .drawBorder(false)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        markDownTextView.resume()
    }

    override fun onPause() {
        super.onPause()
        markDownTextView.pause()
    }

    override fun onDestroyView() {
        stopSummaryMarkdown()
        super.onDestroyView()
    }

    override fun handleBackPressed(): Boolean {
        if (mRealVM.isSummaryLoading.get() || mRealVM.isSummaryStreaming.get()) {
            getString(R.string.ai_generating_auto_saved).showToast()
        } else {
            getString(R.string.auto_saved_as_draft).showToast()
        }
        mRealVM.cancelSummaryStream()
        if (from != StoryEditorActivity.FROM_MY_WORKS) {
            MyCreationsFragment.start()
        } else {
            mActivity.finish()
        }
        return true
    }
}

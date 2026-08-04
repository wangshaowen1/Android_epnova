package com.kuaigeglobal.epnova.feature.creator

import android.text.Editable
import android.view.MotionEvent
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ClipboardUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.SimpleTextWatcher
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.onUserSignOut
import com.kuaigeglobal.epnova.core.ext.sensitiveWordCheck
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.entity.UnusedPackageInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoConfigInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.core.network.apis.request.CreateTaskBody
import com.kuaigeglobal.epnova.core.network.apis.request.SSEContentBody
import com.kuaigeglobal.epnova.core.network.apis.request.UploadStoryBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkTaskData
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorActivity
import com.kuaigeglobal.epnova.feature.creator.hotcase.CreationHotCaseInfo
import com.kuaigeglobal.epnova.feature.creator.set.StoryGenerateConfirmDialog
import com.kuaigeglobal.epnova.feature.creator.set.VideoSetDialog
import com.kuaigeglobal.epnova.feature.editor.info.InputDialog
import com.kuaigeglobal.epnova.feature.splash.newer.FirstRechargeCreditsDialog
import com.kuaigeglobal.epnova.router.AppNavigator
import com.kuaigeglobal.epnova.widget.CounterInputLayout
import com.kuaigeglobal.epnova.widget.GeneratingDialog
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class CreationPageActionDelegate(
    private val fragment: Fragment,
    private val viewModel: CreationViewModel,
    private val isInspirationPage: Boolean,
    private val inputViewProvider: () -> CounterInputLayout?,
    private val onInputChanged: () -> Unit
) {
    private var dialog: GeneratingDialog? = null
    private var suppressInputWatcher = false
    private var pendingFirstRechargeContinuation: (() -> Unit)? = null

    fun bindInput(inputView: CounterInputLayout) {
        inputView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> view.parent.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    view.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
        inputView.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                if (!suppressInputWatcher) viewModel.updateInputFromUser(s.toString())
            }
        })
        updateInputProgrammatically { inputView.setText(viewModel.input.value) }
    }

    fun observeCommon(owner: LifecycleOwner) {
        viewModel.promptGenerating.observe(owner) { state ->
            if (state.first) {
                dialog?.dismiss()
                dialog = fragment.activity?.let { GeneratingDialog(it, state.second) }
                dialog?.show()
            } else {
                dismissGeneratingDialog()
            }
        }
        viewModel.workCreating.observe(owner) { show ->
            if (show) {
                dialog?.dismiss()
                dialog = fragment.activity?.let {
                    GeneratingDialog(it, fragment.getString(R.string.work_creating))
                }
                dialog?.show()
            } else {
                dismissGeneratingDialog()
            }
        }
        viewModel.input.observe(owner) { content ->
            val inputView = inputViewProvider()
            val showFromStart = viewModel.consumeShouldShowInputFromStart()
            if (inputView != null && (showFromStart || inputView.text != content)) {
                updateInputProgrammatically { inputView.setText(content) }
                if (showFromStart) inputView.input.setSelection(0)
            }
            if (isInspirationPage) Global.inspirationContent = content
            else Global.customizedContent = content
            onInputChanged()
        }
        viewModel.firstRechargeCache.observe(owner) { data ->
            data ?: return@observe
            val continuation = pendingFirstRechargeContinuation
            pendingFirstRechargeContinuation = null
            val rechargeDialog = FirstRechargeCreditsDialog.show(
                FirstRechargeCreditsDialog.SCENE_HOME,
                data
            )
            if (rechargeDialog == null) continuation?.invoke()
            Config.get().markFirstRechargeShown(FirstRechargeCreditsDialog.SCENE_HOME)
        }
        fragment.observeEvents {
            onUserSignOut { viewModel.input.value = "" }
            on<Events.ScriptContentGenerateSuccessEvent> {
                val expected = if (isInspirationPage) {
                    Events.ScriptContentGenerateSuccessEvent.TYPE_INSPIRATION
                } else {
                    Events.ScriptContentGenerateSuccessEvent.TYPE_CUSTOMIZED
                }
                if (it.type == expected) viewModel.input.value = ""
            }
        }
    }

    fun optimizeInspiration() {
        if (AppNavigator.checkLoginIntercepted(true) || viewModel.isStreaming()) return
        if (!hasEnoughInput()) return
        fragment.lifecycleScope.launch {
            sensitiveWordCheck(viewModel.input.value, {
                viewModel.sseGenerate(
                    SSEContentBody.TYPE_AI_OPTIMIZE_INSPIRATION,
                    fragment.getString(R.string.optimizing)
                )
            }, { it?.let(::showSensitiveContent) })
        }
    }

    fun randomPrompt() {
        if (!viewModel.isStreaming()) {
            viewModel.sseGenerate(
                SSEContentBody.TYPE_RANDOM_INSPIRATION,
                fragment.getString(R.string.generating_inspiration)
            )
        }
    }

    fun deleteInput() {
        if (viewModel.isStreaming()) return
        viewModel.clearInspirationPromptUsage()
        viewModel.input.value = ""
    }

    fun pasteInput() {
//        if (!viewModel.isStreaming()) viewModel.input.value = ClipboardUtils.getText().toString()
        if (viewModel.isStreaming()) return
        val clipboardContent = ClipboardUtils.getText().toString()
        inputViewProvider()?.setText(clipboardContent)
    }

    fun uploadStory() {
        if (AppNavigator.checkLoginIntercepted(true)) return
        StoryUploadDialog.show(viewModel.fileName, viewModel.fileUrl, viewModel.analysisResult) {
                fileName, fileUrl, analysisResult ->
            viewModel.fileName = fileName
            viewModel.fileUrl = fileUrl
            viewModel.analysisResult = analysisResult
            showVideoSetDialog(fileName?.substringBeforeLast("."), fileUrl = fileUrl)
        }
    }

    fun nextStep() {
        if (AppNavigator.checkLoginIntercepted(true) || viewModel.isStreaming()) return
        if (!hasEnoughInput()) return
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.CREATE,
            if (isInspirationPage) {
                FirebaseEvent.Action.INSPIRATION_NEXT_CLICK
            } else {
                FirebaseEvent.Action.NOVEL_NEXT_CLICK
            }
        )
        fragment.lifecycleScope.launch {
            sensitiveWordCheck(viewModel.input.value, {
                val title = viewModel.extractStoryName(viewModel.input.value)
                showVideoSetDialog(title)
//                if (isInspirationPage) showPackageDialogIfNeeded(title)
//                else showVideoSetDialog(title)
            }, { it?.let(::showSensitiveContent) })
        }
    }

    fun createSame(info: CreationHotCaseInfo) {
        if (viewModel.isStreaming() || AppNavigator.checkLoginIntercepted(true)) return
        viewModel.sameStylePrefill(info.id) { prefill ->
            val story = prefill?.inspirationContent
            if (story.isNullOrBlank()) {
                fragment.getString(R.string.page_load_error).showToast()
                return@sameStylePrefill
            }
            InputDialog.show(type = InputDialog.TYPE_SAME_STORY, content = story) { editedStory ->
                viewModel.videoSetConfig(true) {
//                    showPackageDialogIfNeeded(
//                        workTitle = prefill.workTitle?.takeIf { it.isNotBlank() },
//                        contentOverride = editedStory,
//                        hotCaseId = info.id
//                    )
                }
            }
        }
    }

    private fun showPackageDialogIfNeeded(
        workTitle: String?,
        contentOverride: String? = null,
        hotCaseId: String? = null
    ) {
        viewModel.getUnusedCreationPackages { packages, subtitle ->
            if (packages.isEmpty()) {
                showVideoSetDialog(workTitle, contentOverride = contentOverride, hotCaseId = hotCaseId)
                return@getUnusedCreationPackages
            }
            ShortDramaCreationPackageDialog.newInstance(packages, displaySubtitle = subtitle).apply {
                onCancelClick = {
                    showVideoSetDialog(workTitle, contentOverride = contentOverride, hotCaseId = hotCaseId)
                }
                onUseClick = { packageInfo ->
                    showVideoSetDialog(
                        workTitle,
                        contentOverride = contentOverride,
                        hotCaseId = hotCaseId,
                        creationPackage = packageInfo
                    )
                }
            }.show(fragment.childFragmentManager)
        }
    }

    private fun showVideoSetDialog(
        workTitle: String? = null,
        fileUrl: String? = null,
        contentOverride: String? = null,
        hotCaseId: String? = null,
        creationPackage: UnusedPackageInfo? = null
    ) {
        viewModel.videoSetConfig(true) {
            VideoSetDialog.newInstance(
                workTitle,
                viewModel.videoSetConfig.value,
                if (!fileUrl.isNullOrEmpty()) viewModel.analysisResult?.charCount else null
            ).apply {
                onSuccess = { videoDialog, range, custom, ratio, style, name ->
                    EventManager.onFirebaseTrack(
                        FirebaseEvent.Category.CREATE,
                        FirebaseEvent.Action.START_CREATING_CLICK,
                        if (isInspirationPage) {
                            FirebaseEvent.Value.CREATION_INSPIRATION
                        } else {
                            FirebaseEvent.Value.CREATION_NOVEL
                        },
                        videoDialog?.selectedEpisodeCount(),
                        ratio,
                        style
                    )
                    val createWork = {
                        videoDialog?.dismissAllowingStateLoss()
                        val isFileModel = !fileUrl.isNullOrEmpty()
                        val finalRange = if (creationPackage != null) VideoConfigInfo.MODE_CUSTOM else range
                        val finalCustom = creationPackage?.episodeCount?.toString() ?: custom
                        val singleModel = creationPackage == null &&
                            (finalRange == VideoConfigInfo.MODE_SINGLE ||
                                (finalRange == VideoConfigInfo.MODE_CUSTOM && finalCustom?.toIntOrNull() == 1))
                        val modelType = when {
                            isFileModel -> DramaGenerateConstants.MODEL_STORY_DOCUMENTS
                            singleModel -> DramaGenerateConstants.MODEL_STORY_SINGLE
                            isInspirationPage -> DramaGenerateConstants.MODEL_INSPIRATION
                            else -> DramaGenerateConstants.MODEL_STORY
                        }
                        val workType = when {
                            isFileModel -> WorkInfo.NOVEL_UPLOAD
                            isInspirationPage -> WorkInfo.INSPIRATION_CREATION
                            else -> WorkInfo.NOVEL_CREATION
                        }
                        val startEditor: (WorkTaskData?) -> Unit = { task ->
                            if (isInspirationPage) {
                                viewModel.reportSelectedInspirationUsageIfNeeded()
                                Global.inspirationContent = ""
                            } else {
                                Global.customizedContent = ""
                            }
                            hotCaseId?.let {
                                EventManager.reportCreationUsageCount(
                                    it,
                                    EventManager.REPORT_CREATION_TYPE_HOT_CASE
                                )
                            }
                            viewModel.input.value = ""
                            StoryEditorActivity.start(
                                fragment.activity,
                                task?.workId,
                                name,
                                workType,
                                modelType
                            )
                        }
                        if (isFileModel) {
                            val body = UploadStoryBody(
                                viewModel.analysisResult?.charCount ?: 0,
                                fileUrl,
                                viewModel.fileName,
                                "txt",
                                name,
                                finalRange,
                                finalCustom,
                                ratio,
                                style
                            )
                            if (Config.get().notifyPowerConsume(0)) {
                                StoryGenerateConfirmDialog.newInstance(
                                    body,
                                    viewModel.analysisResult?.estimatedPower?.roundToInt()
                                ).apply { onConfirm = startEditor }
                                    .show(fragment.childFragmentManager)
                            } else {
                                viewModel.confirmPowerConsume(body, startEditor)
                            }
                        } else {
                            val content = contentOverride ?: viewModel.input.value
                            val inspirationId = if (hotCaseId == null && isInspirationPage) {
                                viewModel.getInspirationReportIdForTask()?.takeIf { it.isNotBlank() }
                            } else null
                            viewModel.createTask(
                                modelType,
                                CreateTaskBody(
                                    workInspiration = content,
                                    content = content,
                                    workTitle = name,
                                    totalEpisodeCntCode = finalRange,
                                    totalEpisodeCnt = finalCustom,
                                    workRatio = ratio,
                                    workStyleCode = style,
                                    reportType = when {
                                        hotCaseId != null -> EventManager.REPORT_CREATION_TYPE_HOT_CASE
                                        inspirationId != null -> EventManager.REPORT_CREATION_TYPE_INSPIRATION
                                        else -> null
                                    },
                                    reportId = hotCaseId ?: inspirationId,
//                                    packageInstanceId = creationPackage?.packageInstanceId
                                ),
                                startEditor
                            )
                        }
                    }
                    val showRecharge = !User.get().hasPurchasedPoints() &&
                        !Config.get().isFirstRechargeShown(FirstRechargeCreditsDialog.SCENE_HOME)
                    if (showRecharge) {
                        pendingFirstRechargeContinuation = createWork
                        viewModel.getFirstRechargeInfo {
                            pendingFirstRechargeContinuation = null
                            createWork()
                        }
                    } else {
                        createWork()
                    }
                }
            }.show(fragment.childFragmentManager)
        }
    }

    private fun hasEnoughInput(): Boolean {
        if ((viewModel.input.value?.length ?: 0) >= viewModel.minInputLength) return true
        fragment.getString(R.string.input_min_length, viewModel.minInputLength).showToast()
        return false
    }

    private fun showSensitiveContent(content: CharSequence) {
        val inputView = inputViewProvider() ?: return
        updateInputProgrammatically { inputView.setText(content) }
        inputView.input.setSelection(0)
    }

    private inline fun updateInputProgrammatically(action: () -> Unit) {
        suppressInputWatcher = true
        try {
            action()
        } finally {
            suppressInputWatcher = false
        }
    }

    private fun dismissGeneratingDialog() {
        dialog?.dismiss()
        dialog = null
    }

    fun clear() {
        dismissGeneratingDialog()
        pendingFirstRechargeContinuation = null
    }
}

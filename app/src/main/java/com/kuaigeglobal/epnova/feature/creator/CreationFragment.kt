package com.kuaigeglobal.epnova.feature.creator

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.view.MotionEvent
import android.view.View
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
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoConfigInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.core.network.apis.request.CreateTaskBody
import com.kuaigeglobal.epnova.core.network.apis.request.SSEContentBody
import com.kuaigeglobal.epnova.core.network.apis.request.UploadStoryBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkTaskData
import com.kuaigeglobal.epnova.databinding.FragmentCreationBinding
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorActivity
import com.kuaigeglobal.epnova.feature.creator.set.StoryGenerateConfirmDialog
import com.kuaigeglobal.epnova.feature.creator.set.VideoSetDialog
import com.kuaigeglobal.epnova.feature.splash.newer.FirstRechargeCreditsDialog
import com.kuaigeglobal.epnova.router.AppNavigator
import com.kuaigeglobal.epnova.widget.GeneratingDialog
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class CreationFragment :
    BaseViewModelFragment<FragmentCreationBinding, CreationViewModel>() {

    companion object {
        const val KEY_MODEL = "model"

        //灵感模式
        const val MODEL_INSPIRATION = 1

        //小说改编
        const val MODEL_CUSTOM = 2
        fun newInstance(model: Int? = null): CreationFragment {
            val fragment = CreationFragment()
            val bundle = Bundle()
            bundle.putInt(KEY_MODEL, model ?: MODEL_INSPIRATION)
            fragment.arguments = bundle
            return fragment
        }
    }

    var dialog: GeneratingDialog? = null
    private var pendingFirstRechargeContinuation: (() -> Unit)? = null
    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        binding.inputView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 请求父容器不拦截触摸事件
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }

                MotionEvent.ACTION_UP -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            // 保持原有事件处理
            false
        }

        binding.lyricPrompts.setOnItemClickListener { _, index ->
            if (mRealVM.isStreaming()) {
                return@setOnItemClickListener true
            }
            val tagPromptsTitle = binding.lyricPrompts.tags.getOrNull(index)
            mRealVM.inspirationPrompts.value?.forEach { inspirationInfo ->
                if (inspirationInfo.inspirationTitle == tagPromptsTitle) {
                    mRealVM.input.value = inspirationInfo.inspirationContent
                    return@forEach
                }
            }
            false
        }

        binding.inputView.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mRealVM.input.value = s.toString()
            }
        })
    }

    private fun dataUpdate() {
        mRealVM.inspirationPrompts.observe(this) {
            binding.lyricPrompts.setTags(it?.mapNotNull { it1 -> it1.inspirationTitle }, true)
        }

        mRealVM.promptGenerating.observe(this) {
            if (it.first) {
                dialog?.dismiss()
                dialog = activity?.let { it1 -> GeneratingDialog(it1, it.second) }
                dialog?.show()
            } else {
                dialog?.dismiss()
            }
        }
        mRealVM.input.observe(this) {
            if (mRealVM.input.value != binding.inputView.text) {
                binding.inputView.setText(mRealVM.input.value)
            }
            if (mRealVM.inspirationModel()) Global.inspirationContent =
                it else Global.customizedContent = it
        }
        mRealVM.firstRechargeCache.observe(this) { data ->
            data?.let {
                val continueCreation = pendingFirstRechargeContinuation
                pendingFirstRechargeContinuation = null
                val firstRechargeDialog = FirstRechargeCreditsDialog.show(
                    FirstRechargeCreditsDialog.SCENE_HOME,
                    it
                )
                if (firstRechargeDialog == null) {
                    continueCreation?.invoke()
                    return@let
                }
                firstRechargeDialog.apply {
                    onPaySuccess = {
//                        continueCreation?.invoke()
                    }
                }
                Config.get().markFirstRechargeShown(FirstRechargeCreditsDialog.SCENE_HOME)
            }
        }

        observeEvents {
            onUserSignOut {
                mRealVM.input.value = ""
            }
            on<Events.ScriptContentGenerateSuccessEvent> {
                if (it.type == Events.ScriptContentGenerateSuccessEvent.TYPE_INSPIRATION || it.type == Events.ScriptContentGenerateSuccessEvent.TYPE_CUSTOMIZED) {
                    mRealVM.input.value = ""
                }
            }
        }
    }

    fun showVideoSetDialog(workTitle: String? = null, fileUrl: String? = null) {
        mRealVM.videoSetConfig(true) {
            val config = mRealVM.videoSetConfig.value
            VideoSetDialog.newInstance(workTitle, config, if (!fileUrl.isNullOrEmpty()) mRealVM.analysisResult?.charCount else null).apply {
                onSuccess = { dialog, range, custom, ratio, style, name ->
                    val progressLogic = {
                        dialog?.dismissAllowingStateLoss()
                        val isFileModel = !fileUrl.isNullOrEmpty()
                        val content = this@CreationFragment.mRealVM.input.value

                        val singleModel =
                            range == VideoConfigInfo.MODE_SINGLE || (range == VideoConfigInfo.MODE_CUSTOM && custom?.toIntOrNull() == 1)
                        val modelType = when {
                            isFileModel -> DramaGenerateConstants.MODEL_STORY_DOCUMENTS
                            singleModel -> DramaGenerateConstants.MODEL_STORY_SINGLE
                            this@CreationFragment.mRealVM.inspirationModel() -> DramaGenerateConstants.MODEL_INSPIRATION
                            else -> DramaGenerateConstants.MODEL_STORY
                        }
                        val workType = when {
                            isFileModel -> WorkInfo.NOVEL_UPLOAD
                            this@CreationFragment.mRealVM.inspirationModel() -> WorkInfo.INSPIRATION_CREATION
                            else -> WorkInfo.NOVEL_CREATION
                        }

                        val startEditor: (WorkTaskData?) -> Unit = { data ->
                            //只有小说文档模式有data
                            if (this@CreationFragment.mRealVM.inspirationModel()) Global.inspirationContent = "" else Global.customizedContent = ""
                            StoryEditorActivity.start(
                                context = mActivity,
                                workId = data?.workId,
                                name = name,
                                workType = workType,
                                model = modelType
                            )
                            FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_MY_CREATIONS))
                            mActivity.finish()
                        }

                        if (isFileModel) {
                            this@CreationFragment.mRealVM
                            val charCount = this@CreationFragment.mRealVM.analysisResult?.charCount ?: 0
                            val body = UploadStoryBody(
                                charCount,
                                fileUrl,
                                this@CreationFragment.mRealVM.fileName,
                                "txt",
                                name,
                                range,
                                custom, ratio, style
                            )
                            if (Config.get().notifyPowerConsume(0)) {
                                StoryGenerateConfirmDialog.newInstance(
                                    body,
                                    this@CreationFragment.mRealVM.analysisResult?.estimatedPower?.roundToInt()
                                )
                                    .apply {
                                        onConfirm = { data -> startEditor(data) }
                                    }.show(this@CreationFragment.childFragmentManager)
                            } else {
                                this@CreationFragment.mRealVM.confirmPowerConsume(body) { data ->
                                    startEditor(data)
                                }
                            }
                        } else {//非小说文档走这里
                            val body = CreateTaskBody(
                                workInspiration = content,
                                content = content,
                                workTitle = name,
                                totalEpisodeCntCode = range,
                                totalEpisodeCnt = custom,
                                workRatio = ratio,
                                workStyleCode = style,
                                workType = workType
                            )
                            this@CreationFragment.mRealVM.createTask(modelType, body) {
                                startEditor(it)
                            }
                        }
                    }

                    val canShowFirstRecharge = !User.get().hasPurchasedPoints() &&
                        !Config.get().isFirstRechargeShown(FirstRechargeCreditsDialog.SCENE_HOME)
                    if (canShowFirstRecharge) {
                        pendingFirstRechargeContinuation = progressLogic
                        this@CreationFragment.mRealVM.getFirstRechargeInfo {
                            pendingFirstRechargeContinuation = null
                            progressLogic()
                        }
                    } else {
                        progressLogic()
                    }
                }
            }.show(childFragmentManager)
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnOptimize -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                if ((mRealVM.input.value?.length ?: 0) < mRealVM.minInputLength) {
                    getString(R.string.input_min_length, mRealVM.minInputLength).showToast()
                    return
                }
                if (mRealVM.isStreaming()) {
                    return
                }
                lifecycleScope.launch {
                    sensitiveWordCheck(mRealVM.input.value, {
                        mRealVM.sseGenerate(SSEContentBody.TYPE_AI_OPTIMIZE_INSPIRATION, getString(R.string.optimizing))
                    }, {
                        it?.let {
                            binding.inputView.setText(it)
                        }
                    })
                }
            }

            R.id.randomInspirations -> {
                mRealVM.randomInspirations(true)
            }

            R.id.btnRandomPrompt -> {
                if (mRealVM.isStreaming()) {
                    return
                }
                mRealVM.sseGenerate(SSEContentBody.TYPE_RANDOM_INSPIRATION, getString(R.string.generating_inspiration))
            }

            R.id.btnDelete -> {
                if (mRealVM.isStreaming()) {
                    return
                }
                mRealVM.input.value = ""
            }

            R.id.btnStoryUpload -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                StoryUploadDialog.show(
                    mRealVM.fileName,
                    mRealVM.fileUrl,
                    mRealVM.analysisResult
                ) { fileName, fileUrl, analysisResult ->
                    mRealVM.fileName = fileName
                    mRealVM.fileUrl = fileUrl
                    mRealVM.analysisResult = analysisResult
                    showVideoSetDialog(fileName?.substringBeforeLast("."), mRealVM.fileUrl)
                }
            }

            R.id.btnPaste -> {
                if (mRealVM.isStreaming()) {
                    return
                }
                mRealVM.input.value = ClipboardUtils.getText().toString()
            }

            R.id.btnNext -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                if (mRealVM.isStreaming()) {
                    return
                }
                if ((mRealVM.input.value?.length ?: 0) < mRealVM.minInputLength) {
                    getString(R.string.input_min_length, mRealVM.minInputLength).showToast()
                    return
                }
                //检查敏感词
                lifecycleScope.launch {
                    sensitiveWordCheck(mRealVM.input.value, {
                        showVideoSetDialog(workTitle = mRealVM.extractStoryName(mRealVM.input.value))
                    }, {
                        it?.let {
                            binding.inputView.setText(it)
                        }
                    })
                }
            }
        }
    }
}

package com.kuaigeglobal.epnova.feature.creator.continuewriting

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.SimpleTextWatcher
import com.kuaigeglobal.epnova.common.utils.SensitiveHighlighter
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.ext.sensitiveWordCheck
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.core.network.apis.request.SSEContentBody
import com.kuaigeglobal.epnova.databinding.ActivityContinueWritingBinding
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorActivity
import com.kuaigeglobal.epnova.router.AppNavigator
import com.kuaigeglobal.epnova.widget.GeneratingDialog
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.launch

class ContinueWritingActivity :
    BaseViewModelActivity<ActivityContinueWritingBinding, ContinueWritingViewModel>() {

    companion object {

        @JvmStatic
        fun start(context: Context?, workId: String?, storyId: String?, workTitle: String?) {
            if (context == null) return
            val starter = Intent(context, ContinueWritingActivity::class.java)
            starter.putExtra(Constants.Extra.WORK_ID, workId)
            starter.putExtra(Constants.Extra.STORY_ID, storyId)
            starter.putExtra(Constants.Extra.NAME, workTitle)
            context.startActivity(starter)
        }
    }

    var dialog: GeneratingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        mRealVM.promptGenerating.observe(this) {
            if (it.first) {
                dialog?.dismiss()
                dialog = GeneratingDialog(this, it.second)
                dialog?.show()
            } else {
                dialog?.dismiss()
            }
        }
        mRealVM.input.observe(this) {
            if (mRealVM.input.value != binding.inputView.text) {
                binding.inputView.setText(mRealVM.input.value)
            }
            Global.continueWritingContent[mRealVM.workId] = mRealVM.input.value
        }
        binding.inputView.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mRealVM.input.value = s.toString()
            }
        })

        observeEvents {
            on<Events.ConfigChangeEvent> { event ->
                //mRealVM.refreshConfig()
            }
            on<Events.ScriptContentGenerateSuccessEvent> {
                if (it.type == Events.ScriptContentGenerateSuccessEvent.TYPE_CONTINUE_WRITE && it.seriesId == mRealVM.workId) {
                    finish()
                }
            }
        }
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
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnRegenerate, R.id.btnAIExpand -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                if (!mRealVM.isSSEStreaming.get()) {
                    lifecycleScope.launch {
                        sensitiveWordCheck(mRealVM.input.value, {
                            mRealVM.sseGenerate()
                        }, {
                            it?.let {
                                binding.inputView.setText(it)
                            }
                        })
                    }
                }
            }

            R.id.btnDelete -> {
                mRealVM.input.value = ""
            }

            R.id.btnNext -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                if ((mRealVM.input.value?.length ?: 0) < mRealVM.minInputLength) {
                    getString(R.string.input_min_length, mRealVM.minInputLength).showToast()
                    return
                }
                val checkJob = {
                    lifecycleScope.launch {
                        sensitiveWordCheck(mRealVM.input.value, {
                            toGenerate()
                        }, {
                            it?.let {
                                binding.inputView.setText(it)
                            }
                        })
                    }
                }
                if (mRealVM.episodeInfo.value == null) {
                    mRealVM.getEpisodeInfo {
                        checkJob.invoke()
                    }
                    return
                }
                if (!mRealVM.isSSEStreaming.get()) {
                    checkJob.invoke()
                }
            }
        }
    }

    fun toGenerate() {
        mRealVM.saveSequelNovelStory { saveSequelNovelStory ->
            StoryEditorActivity.startContinue(
                context = this,
                workId = saveSequelNovelStory.workId,
                workTitle = mRealVM.workName,
                storyId = saveSequelNovelStory.storyId,
                chapterIndex = (mRealVM.episodeInfo.value?.sortNo ?: 0) + 1,
                previousStoryId = mRealVM.episodeInfo.value?.storyId,
                workType = WorkInfo.NOVEL_CREATION,
                model = DramaGenerateConstants.MODEL_CONTINUE_STORY,
                dramaContent = mRealVM.input.value,
                videoRatio = mRealVM.episodeInfo.value?.workRatio
            )
            finish()
        }
    }
}

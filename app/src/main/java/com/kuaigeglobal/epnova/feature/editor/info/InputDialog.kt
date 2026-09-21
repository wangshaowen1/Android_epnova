package com.kuaigeglobal.epnova.feature.editor.info

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDialog
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.ThreadUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.SimpleTextWatcher
import com.kuaigeglobal.epnova.common.ext.applySmartFocusManager
import com.kuaigeglobal.epnova.common.ext.showCommonDialog
import com.kuaigeglobal.epnova.common.ext.showConfirmPrompt
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.ext.sensitiveWordCheck
import com.kuaigeglobal.epnova.databinding.DialogInputBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.launch

/**
 * 修改描述-通用版
 */
class InputDialog :
    BaseBottomPushVMDialogFragment<DialogInputBinding, InputViewModel>() {

    companion object {
        const val TYPE_WORK_DESC = 0
        const val TYPE_EPISODE_SUMMARY = 1
        const val TYPE_EPISODE_CONTENT = 3
        const val TYPE_ASSET_DESC = 4
        const val TYPE_STORYBOARD_DESC = 5
        const val TYPE_SAME_STORY = 6

        /**通用类型，只编辑文案，不需要其他逻辑*/
        const val TYPE_NORMAL = 2

        private const val KEY_HIGHLIGHTED_CONTENT = "key_highlighted_content"

        fun show(
            type: Int? = TYPE_NORMAL,
            workId: String? = null,
            storyId: String? = null,
            content: String? = null,
            extra: String? = null,
            highlightedContent: CharSequence? = null,
            callback: (String) -> Unit
        ) {
            val activity = topActivity as? FragmentActivity ?: return
            InputDialog().apply {
                arguments = bundleOf(
                    Constants.Extra.WORK_ID to workId,
                    Constants.Extra.STORY_ID to storyId,
                    Constants.Extra.TYPE to type,
                    Constants.Extra.CONTENT to content,
                    Constants.Extra.EXTRA to extra,
                    KEY_HIGHLIGHTED_CONTENT to highlightedContent
                )
                this.isCancelable = false
                this.callback = callback
            }.show(activity.supportFragmentManager)
        }
    }

    var callback: ((String) -> Unit)? = null

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            closeConfirm()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AppCompatDialog(requireContext(), com.nhsd.basic.R.style.Dialog_BottomPush).apply {
            setCanceledOnTouchOutside(true)
            onBackPressedDispatcher.addCallback(this@InputDialog, backPressedCallback)
        }.also {
            // 隐藏底部导航栏或手势条
            it.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false) // 设置全屏
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
//                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars()) // 隐藏system bars
                windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars()) // 隐藏navigation bars
                // 上滑显示后自动隐藏行为
                windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.root.applySmartFocusManager()
        binding.inputView.setTextNoSelection("")
        binding.close.setOnClickListener {
            closeConfirm()
        }
        binding.inputView.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mRealVM.input.value = s.toString()
                s?.let { after ->
                    // 是否改变
                    mRealVM.hasChange = after.toString() != mRealVM.content
                }
            }
        })

        mRealVM.input.observe(this) {
            if (it != binding.inputView.text) {
                binding.inputView.setTextNoSelection(it)
            }
        }

        // 若外部传入了带高亮的文本（如敏感词检测结果），在初始文本设置完后覆盖应用
        arguments?.getCharSequence(KEY_HIGHLIGHTED_CONTENT)?.let { highlighted ->
            binding.inputView.post {
                binding.inputView.setTextNoSelection(highlighted)
                scrollToFirstSensitiveWord(highlighted)
            }
        }

        binding.btnOk.setOnClickListener {
            if (binding.inputView.text.trim().isEmpty() && mRealVM.type != TYPE_SAME_STORY) {
                getString(R.string.enter_update_content).showToast()
                return@setOnClickListener
            }
            if (mRealVM.content != binding.inputView.text) {
                save()
            } else {
                dismissAllowingStateLoss()
            }
        }
        binding.inputView.input.postDelayed({
            if (isActive) {
                binding.inputView.input.requestFocus()
                KeyboardUtils.showSoftInput(binding.inputView.input)
            }
        }, 100)

        binding.inputView.setHint(if (mRealVM.type == TYPE_SAME_STORY) R.string.same_story_input_hint.getResString() else R.string.enter_content.getResString())
    }

    fun closeConfirm() {
        if (mRealVM.hasChange && mRealVM.content != binding.inputView.text) {
            if (mRealVM.type == TYPE_SAME_STORY) {
                topActivity?.showCommonDialog(
                    title = getString(R.string.discard_changes_title),
                    tip = getString(R.string.discard_changes_content),
                    cancelText = getString(R.string.discard),
                    confirmText = getString(R.string.save_and_exit),
                    cancel = { dismissAllowingStateLoss() },
                    confirm = { save() }
                )
            } else {
                topActivity?.showConfirmPrompt(
                    tip = getString(R.string.confirm_exit_without_saving),
                    cancelText = getString(R.string.exit_directly),
                    confirmText = getString(R.string.save_and_exit),
                    cancel = {
                        dismissAllowingStateLoss()
                    },
                    confirm = {
                        save()
                    })
            }
        } else {
            dismissAllowingStateLoss()
        }
    }

    private fun save() {
        lifecycleScope.launch {
            sensitiveWordCheck(mRealVM.input.value).let { (isPass, highlightText) ->
                if (!isPass) {
                    highlightText?.let {
                        binding.inputView.setText(it)
                        scrollToFirstSensitiveWord(it)
                    }
                } else {
                    mRealVM.save {
                        callback?.invoke(binding.inputView.text.trim())
                        dismissAllowingStateLoss()
                    }
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        hideSoftInput()
        super.onDismiss(dialog)
    }

    override fun onDestroy() {
        hideSoftInput()
        super.onDestroy()
    }

    private fun hideSoftInput() {
        ThreadUtils.runOnUiThreadDelayed({ KeyboardUtils.hideSoftInput(ActivityUtils.getTopActivity()) }, 100)
    }

    private fun scrollToFirstSensitiveWord(text: CharSequence) {
        if (text !is Spanned) return
        val spans = text.getSpans(0, text.length, ForegroundColorSpan::class.java)
        if (spans.isEmpty()) return
        val firstSpan = spans.minByOrNull { text.getSpanStart(it) } ?: return
        val endPos = text.getSpanEnd(firstSpan)
        binding.inputView.input.setSelection(endPos)
    }
}

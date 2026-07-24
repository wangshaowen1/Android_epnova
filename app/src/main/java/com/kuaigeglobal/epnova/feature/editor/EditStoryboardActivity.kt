package com.kuaigeglobal.epnova.feature.editor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.activity.OnBackPressedCallback
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.applySmartFocusManager
import com.kuaigeglobal.epnova.common.ext.showCommonDialog
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleStoryboardInfo
import com.kuaigeglobal.epnova.databinding.ActivityEditStoryboardBinding
import com.kuaigeglobal.epnova.feature.editor.info.InputDialog
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener

/**
 * 分镜重绘
 */
class EditStoryboardActivity : BaseViewModelActivity<ActivityEditStoryboardBinding, EditStoryboardViewModel>() {

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleBackPressed()
        }
    }

    companion object {
        @JvmStatic
        fun launchIntent(activity: Activity, info: SimpleStoryboardInfo, workRatio: String?, storyStatus: Int?): Intent {
            val intent = Intent(activity, EditStoryboardActivity::class.java)
            intent.putExtra(Constants.Extra.DATA, info)
            intent.putExtra(Constants.Extra.VIDEO_RATIO, workRatio)
            intent.putExtra(Constants.Extra.STATUS, storyStatus)
            return intent
        }

        @JvmStatic
        fun start(info: SimpleStoryboardInfo, workRatio: String?, storyStatus: Int?) {
            topActivity?.let { it.startActivity(launchIntent(it, info, workRatio, storyStatus)) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        binding.root.applySmartFocusManager()
        binding.tvStoryboardDesc.movementMethod = ScrollingMovementMethod()
        binding.tvStoryboardDesc.setOnSingleClickListener {
            InputDialog.show(
                type = InputDialog.TYPE_STORYBOARD_DESC,
                content = mRealVM.storyBoardDesc.value?.toString(),
                callback = { mRealVM.storyBoardDesc.value = it })
        }
    }

    private fun handleBackPressed() {
        if (mRealVM.storyBoardDesc.value != mRealVM.storyboard.get()?.storyBoardDesc) {
            showCommonDialog(
                tip = getString(R.string.save_storyboard_changes_prompt),
                cancelText = R.string.discard.getResString(),
                cancel = { exitPage() },
                confirmText = R.string.save_and_exit.getResString(),
                confirm = { mRealVM.save(true) })
        } else {
            exitPage()
        }
    }

    private fun exitPage() {
        backPressedCallback.isEnabled = false
        onBackPressedDispatcher.onBackPressed()
    }
}
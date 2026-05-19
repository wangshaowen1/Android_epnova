package com.kuaigeglobal.epnova.feature.editor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
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
        binding.root.applySmartFocusManager()
        binding.tvStoryboardDesc.movementMethod = ScrollingMovementMethod()
        binding.tvStoryboardDesc.setOnSingleClickListener {
            InputDialog.show(
                type = InputDialog.TYPE_STORYBOARD_DESC,
                content = mRealVM.storyBoardDesc.value?.toString(),
                callback = { mRealVM.storyBoardDesc.value = it })
        }
    }

    override fun onBackPressed() {
        if (mRealVM.storyBoardDesc.value != mRealVM.storyboard.get()?.storyBoardDesc) {
            showCommonDialog(
                tip = getString(R.string.save_storyboard_changes_prompt),
                cancelText = R.string.discard.getResString(),
                cancel = { super.onBackPressed() },
                confirmText = R.string.save_and_exit.getResString(),
                confirm = { mRealVM.save(true) })
        } else {
            super.onBackPressed()
        }
    }
}
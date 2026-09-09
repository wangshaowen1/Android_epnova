package com.kuaigeglobal.epnova.feature.settings.feedback

import android.app.Activity
import android.content.Intent
import com.kuaigeglobal.epnova.databinding.ActivityMyFeedbackBinding
import com.nhsd.basic.BaseViewModelActivity

class MyFeedbackActivity : BaseViewModelActivity<ActivityMyFeedbackBinding, FeedbackViewModel>() {
    companion object {
        private const val EXTRA_FEEDBACK_ID = "extra_feedback_id"

        fun start(activity: Activity?, feedbackId: String) {
            activity ?: return
            val intent = Intent(activity, MyFeedbackActivity::class.java).apply {
                putExtra(EXTRA_FEEDBACK_ID, feedbackId)
            }
            activity.startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        mRealVM.loadFeedbackDetail(intent.getStringExtra(EXTRA_FEEDBACK_ID))
        mRealVM.feedbackDetail.get()?.let { detail ->
            binding.photo1.setAll(detail.imageUrls)
            binding.photo2.setAll(detail.replyImageUrls)
        }
    }
}


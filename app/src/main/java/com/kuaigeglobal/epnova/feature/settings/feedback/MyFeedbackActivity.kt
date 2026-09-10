package com.kuaigeglobal.epnova.feature.settings.feedback

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.Observable
import com.kuaigeglobal.epnova.databinding.ActivityMyFeedbackBinding
import com.nhsd.basic.BaseViewModelActivity

class MyFeedbackActivity : BaseViewModelActivity<ActivityMyFeedbackBinding, FeedbackViewModel>() {
    private val feedbackDetailObserver = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            val detail = mRealVM.feedbackDetail.get()
            binding.photo1.setAll(detail?.imagesUrl.orEmpty())
            binding.photo2.setAll(detail?.replyContent?.imagesUrl.orEmpty())
        }
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRealVM.feedbackDetail.addOnPropertyChangedCallback(feedbackDetailObserver)
        mRealVM.loadFeedbackDetail(intent.getStringExtra(EXTRA_FEEDBACK_ID))
    }

    override fun onDestroy() {
        mRealVM.feedbackDetail.removeOnPropertyChangedCallback(feedbackDetailObserver)
        super.onDestroy()
    }
}


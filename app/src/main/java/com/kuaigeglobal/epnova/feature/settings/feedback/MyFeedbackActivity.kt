package com.kuaigeglobal.epnova.feature.settings.feedback

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.Observable
import com.kuaigeglobal.epnova.databinding.ActivityMyFeedbackBinding
import com.nhsd.basic.BaseViewModelActivity

/** 单条反馈详情页，展示反馈内容、客服回复及双方图片。 */
class MyFeedbackActivity : BaseViewModelActivity<ActivityMyFeedbackBinding, FeedbackViewModel>() {
    // 详情变化时同步两组图片；详情为空也传入空列表，以清除旧图。
    private val feedbackDetailObserver = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            val detail = mRealVM.feedbackDetail.get()
            binding.photo1.setPhotoUrls(detail?.imagesUrls.orEmpty())
            binding.photo2.setPhotoUrls(detail?.replyContent?.imagesUrls.orEmpty())
        }
    }

    companion object {
        private const val EXTRA_FEEDBACK_ID = "extra_feedback_id"

        /** 携带反馈业务 ID 打开详情，完整内容由详情接口加载。 */
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
        // 先注册图片观察回调，再请求详情，确保数据返回时能够同步图片网格。
        mRealVM.feedbackDetail.addOnPropertyChangedCallback(feedbackDetailObserver)
        mRealVM.loadFeedbackDetail(intent.getStringExtra(EXTRA_FEEDBACK_ID))
    }

    override fun onDestroy() {
        // 手动注册的 Observable 回调需成对移除，避免继续持有 Activity 视图。
        mRealVM.feedbackDetail.removeOnPropertyChangedCallback(feedbackDetailObserver)
        super.onDestroy()
    }
}


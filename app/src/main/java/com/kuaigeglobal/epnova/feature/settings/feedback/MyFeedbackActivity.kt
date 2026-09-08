package com.kuaigeglobal.epnova.feature.settings.feedback

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.picker.AppImagePicker
import com.kuaigeglobal.epnova.common.utils.Utils
import com.kuaigeglobal.epnova.databinding.ActivityFeedbackBinding
import com.kuaigeglobal.epnova.databinding.ActivityMyFeedbackBinding
import com.kuaigeglobal.epnova.feature.settings.customerservice.CustomerServiceDialog
import com.kuaigeglobal.epnova.widget.PhotoGridView
import com.kuaigeglobal.epnova.widget.actionsheet.ActionSheetDialog
import com.kuaigeglobal.epnova.widget.actionsheet.ActionSheetHelper
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast



class MyFeedbackActivity : BaseViewModelActivity<ActivityMyFeedbackBinding, FeedbackViewModel>(){
    companion object {

        fun start(
            activity: Activity?
        ) {
            activity ?: return
            val intent = Intent(activity, MyFeedbackActivity::class.java)
            activity.startActivity(intent)
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


}


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
import com.kuaigeglobal.epnova.feature.settings.customerservice.CustomerServiceDialog
import com.kuaigeglobal.epnova.widget.PhotoGridView
import com.kuaigeglobal.epnova.widget.actionsheet.ActionSheetDialog
import com.kuaigeglobal.epnova.widget.actionsheet.ActionSheetHelper
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast


/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:投诉建议+举报
 * @Data: 2024/7/19 星期五 19:44
 * @Version: 1.0
 * *****************************************
 */
class FeedbackActivity :
    BaseViewModelActivity<ActivityFeedbackBinding, FeedbackViewModel>(), TextWatcher {
    companion object {
        private const val REQUEST_CODE_CHOOSE = 0x110
        private const val MAX_PHOTO_COUNT = 4

        fun start(
            activity: Activity?
        ) {
            activity ?: return
            val intent = Intent(activity, FeedbackActivity::class.java)
            activity.startActivity(intent)
        }
    }

    private val imagePicker = AppImagePicker(
        activity = this,
        requestCode = REQUEST_CODE_CHOOSE,
        maxSelectable = MAX_PHOTO_COUNT,
        onResult = ::handleSelectedImages
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initTitleBar()
        binding.photoGrid.setOnAddButtonClickedListener { requestAlbumsPermission() }
        binding.photoGrid.setOnPhotoChangedListener(object : PhotoGridView.OnPhotoChangedListener {
            override fun onPhotoAdded(photo: Uri?) {
                mRealVM.photoSize.value = binding.photoGrid.originalPhotos.size
            }

            override fun onPhotoRemoved(photo: Uri?) {
                mRealVM.photoSize.value = binding.photoGrid.originalPhotos.size
            }

        })
        binding.feedbackInput.addTextChangedListener(this)
        binding.contact.addTextChangedListener(this)
    }

    private fun initTitleBar() {
        binding.ivBack.setOnClickListener { finish() }
        binding.rlHistory.setOnClickListener {


        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        imagePicker.onActivityResult(requestCode, resultCode, data)
    }

    // 只要包含 @ 且 @ 前后都有字符，且有点号即可
    private fun isEmailValid(email: String?): Boolean {
        email ?: return false
        val pattern = """^.+@.+\..+$""".toRegex()
        return email.isNotBlank() && pattern.matches(email)
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.commit -> {
                if (mRealVM.type.get() == null) {
                    getString(R.string.select_type_please).showToast()
                    return
                }
                if (binding.feedbackInput.text.length < 10) {
                    getString(R.string.input_min_length, 10).showToast()
                    return
                }
                if (mRealVM.email.value.isNullOrEmpty() || !isEmailValid(mRealVM.email.value)) {
                    getString(R.string.error_invalid_email).showToast()
                    return
                }
                mRealVM.feedback(
                    binding.feedbackInput.text.toString(),
                    Utils.toPathList(binding.photoGrid.originalPhotos)
                )
            }

            R.id.typeLayout -> {
                ActionSheetHelper.createDialog(
                    mRealVM.typeNames?.mapNotNull { it.value }?.toTypedArray()
                ) { _: ActionSheetDialog?, position: Int ->
                    if (position >= 0) {
                        mRealVM.type.set(mRealVM.typeNames?.getOrNull(position))
                        updateButtonEnable()
                    }
                }.show(supportFragmentManager)
            }

            R.id.ll_contact_customer -> {
                //联系客服
                //openTheCustomerServiceChat()
                CustomerServiceDialog.show()
            }
        }
    }


    private fun requestAlbumsPermission() {
        imagePicker.pick(binding.photoGrid.copiedPhotos)
    }

    private fun handleSelectedImages(uris: List<Uri>) {
        binding.photoGrid.setAll(ArrayList(uris.distinct().take(MAX_PHOTO_COUNT)), false)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
        updateButtonEnable()
    }

    private fun updateButtonEnable() {
        binding.commit.alpha =
            if (mRealVM.type.get() != null && (binding.feedbackInput.text?.length ?: 0) >= 10) {
                1f
            } else {
                0.5f
            }
    }
}


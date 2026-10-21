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
import com.kuaigeglobal.epnova.widget.actionsheet.ActionSheetDialog
import com.kuaigeglobal.epnova.widget.actionsheet.ActionSheetHelper
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast


/**
 * *****************************************
 * 项目名称: Epnova
 * @Author: SZYMN
 * @Introduce: 反馈提交页，负责反馈类型、图片选择、输入校验和历史记录入口。
 * @Data: 2024/7/19 星期五 19:44
 * @Version: 1.0
 * *****************************************
 */
class FeedbackActivity :
    BaseViewModelActivity<ActivityFeedbackBinding, FeedbackViewModel>(), TextWatcher {
    companion object {
        private const val REQUEST_CODE_CHOOSE = 0x110
        private const val MAX_PHOTO_COUNT = 4
        private const val EXTRA_IS_UNPROCESSED = "extra_is_unprocessed"

        /** 打开反馈页，isUnprocessed 用于初始化历史入口的未读提醒红点。 */
        fun start(
            activity: Activity?,
            isUnprocessed: Boolean = false
        ) {
            activity ?: return
            val intent = Intent(activity, FeedbackActivity::class.java).apply {
                putExtra(EXTRA_IS_UNPROCESSED, isUnprocessed)
            }
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
        mRealVM.isUnprocessed.value = intent.getBooleanExtra(EXTRA_IS_UNPROCESSED, false)
        initTitleBar()
        binding.photoGrid.setOnAddButtonClickedListener { requestAlbumsPermission() }
        // 添加或删除图片后同步数量，更新表单中的已选图片提示。
        binding.photoGrid.setOnPhotosChangedListener { photos ->
            mRealVM.photoSize.value = photos.size
        }
        binding.feedbackInput.addTextChangedListener(this)
        binding.contact.addTextChangedListener(this)
    }

    private fun initTitleBar() {
        binding.ivBack.setOnClickListener { finish() }
        binding.rlHistory.setOnClickListener {
            FeedbackHistoryActivity.start(this)
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
            // 提交时校验类型、至少 10 个字符的描述和邮箱，校验通过后再上传图片。
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
                    Utils.toPathList(binding.photoGrid.photos)
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


    /** 将已选图片交给选择器回显，权限申请和选择结果由 AppImagePicker 处理。 */
    private fun requestAlbumsPermission() {
        imagePicker.pick(binding.photoGrid.photos)
    }

    /** 追加新选择的图片，去重并限制为最多 4 张，再同步到图片网格。 */
    private fun handleSelectedImages(uris: List<Uri>) {
        binding.photoGrid.setPhotos(
            (binding.photoGrid.photos + uris).distinct().take(MAX_PHOTO_COUNT)
        )
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
        updateButtonEnable()
    }

    /** 仅用透明度提示类型和描述是否满足要求，完整校验仍在提交点击时执行。 */
    private fun updateButtonEnable() {
        binding.commit.alpha =
            if (mRealVM.type.get() != null && (binding.feedbackInput.text?.length ?: 0) >= 10) {
                1f
            } else {
                0.5f
            }
    }
}


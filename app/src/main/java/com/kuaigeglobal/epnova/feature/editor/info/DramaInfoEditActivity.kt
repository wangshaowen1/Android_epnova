package com.kuaigeglobal.epnova.feature.editor.info

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.Editable
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.SimpleTextWatcher
import com.kuaigeglobal.epnova.common.ext.applySmartFocusManager
import com.kuaigeglobal.epnova.common.ext.getTempFilePathFromUri
import com.kuaigeglobal.epnova.common.picker.AppImagePicker
import com.kuaigeglobal.epnova.common.utils.CropUtils
import com.kuaigeglobal.epnova.common.utils.FileUtils
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.ext.sensitiveWordCheck
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.databinding.ActivityWorkInfoEditBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.setupTranslucentStatus
import com.nhsd.basic.ext.showToast
import com.nhsd.crop.Crop
import kotlinx.coroutines.launch

/**
 * @Introduce:专辑-编辑详情，简介之类
 */
class DramaInfoEditActivity :
    BaseViewModelActivity<ActivityWorkInfoEditBinding, DramaInfoEditViewModel>() {

    companion object {
        private const val REQUEST_CODE_CHOOSE = 0x14

        @JvmStatic
        fun start(context: Context, workInfo: WorkInfo?) {
            val starter = Intent(context, DramaInfoEditActivity::class.java)
            starter.putExtra(Constants.Extra.DATA, workInfo)
            context.startActivity(starter)
        }
    }

    private var pendingCoverCropUri: Uri? = null

    private val coverPicker = AppImagePicker(
        activity = this,
        requestCode = REQUEST_CODE_CHOOSE,
        maxSelectable = 1,
        onResult = { uris -> uris.firstOrNull()?.let(::startCoverCrop) }
    )

    override fun setTranslucentStatus() {
        // 导航栏图标浅色、背景透明
        setupTranslucentStatus(false, Color.TRANSPARENT)
    }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        binding.nameInputer.applySmartFocusManager()
        binding.nameInputer.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != mRealVM.name.value) {
                    mRealVM.isModify = true
                }
                mRealVM.name.value = s.toString()
            }
        })
        binding.descInputer.applySmartFocusManager()
        binding.descInputer.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != mRealVM.desc.value) {
                    mRealVM.isModify = true
                }
                mRealVM.desc.value = s.toString()
            }
        })
    }

    private fun dataUpdate() {
        mRealVM.name.observe(this) {
            if (it != binding.nameInputer.text) {
                binding.nameInputer.setText(it)
            }
        }
        mRealVM.desc.observe(this) {
            if (it != binding.descInputer.text) {
                binding.descInputer.setText(it)
            }
        }

        mRealVM.editAndModify.observe(this) {
            if (!it) return@observe
            showToast(R.string.toast_save_success)
            FlowEventBus.postSync(Events.SeriesUpdatedEvent(mRealVM.workInfo?.workId ?: ""))
            finish()
        }
    }


    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnChangeCover -> {
                requestAlbumsPermission()
            }

            R.id.btnConfirm -> {
                mRealVM.saveInfo()
            }

            R.id.btnOptimize -> {
                lifecycleScope.launch {
                    sensitiveWordCheck(mRealVM.desc.value.toString(), {
                        mRealVM.sseGenerate()
                    }, {
                        it?.let {
                            binding.descInputer.setText(it)
                        }
                    })
                }
            }

            R.id.btnDelete -> {
                mRealVM.desc.value = ""
            }
        }
    }

    private fun requestAlbumsPermission() {
        coverPicker.pick()
    }

    private fun startCoverCrop(source: Uri) {
        val destination = Uri.fromFile(FileUtils.newTempImageFile())
        pendingCoverCropUri = destination
        CropUtils.start(
            this,
            source,
            destination,
            CropUtils.getColorPrimary(this),
            getColor(R.color.textColorDark),
            1f to 1f
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (coverPicker.onActivityResult(requestCode, resultCode, data)) return
        if (resultCode == RESULT_OK && requestCode == Crop.REQUEST_CROP) {
            val cropped = data?.getParcelableExtra<Uri>(Crop.EXTRA_OUTPUT_URI) ?: pendingCoverCropUri
            val path = if (cropped?.scheme == "file") {
                cropped.path
            } else {
                cropped?.getTempFilePathFromUri(this)
            }
            pendingCoverCropUri = null
            path?.let { mRealVM.changeTargetPicture(it) }
        }
    }
}

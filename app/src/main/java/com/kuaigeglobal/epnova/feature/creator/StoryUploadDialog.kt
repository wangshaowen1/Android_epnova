package com.kuaigeglobal.epnova.feature.creator

import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.os.Build
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.getFileMeta
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.permissions.requestRead2WritePermissions
import com.kuaigeglobal.epnova.common.utils.TextEncodingUtil
import com.kuaigeglobal.epnova.databinding.DialogStoryUploadBinding
import com.kuaigeglobal.epnova.feature.creator.StoryUploadViewModel.Companion.STATE_UPLOAD_FAIL
import com.nhsd.basic.BaseVMDialogFragment
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream

/**
 * 微信客服二维码
 */
class StoryUploadDialog : BaseVMDialogFragment<DialogStoryUploadBinding, StoryUploadViewModel>() {

    companion object {
        fun show(
            fileName: String?,
            fileUrl: String?,
            result: TextEncodingUtil.AnalysisResult?,
            callback: (String?, String?, TextEncodingUtil.AnalysisResult?) -> Unit
        ) {
            val activity = topActivity as? FragmentActivity ?: return
            StoryUploadDialog().apply {
                arguments = bundleOf(Constants.Extra.NAME to fileName, Constants.Extra.URL to fileUrl, Constants.Extra.RESULT to result)
                this.uploadCallback = callback
            }.show(activity.supportFragmentManager)
        }
    }

    var uploadCallback: (String?, String?, TextEncodingUtil.AnalysisResult?) -> Unit = { _, _, _ -> }
    var analyzeResult: TextEncodingUtil.AnalysisResult? = null

    override fun onPrepareView() {
        super.onPrepareView()
        analyzeResult = arguments?.getParcelableValue(Constants.Extra.RESULT, TextEncodingUtil.AnalysisResult::class.java)
        binding.btnChoose.paintFlags = binding.btnChoose.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.btnChoose.setOnSingleClickListener {
            openFilePicker()
        }
        binding.cancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.confirm.setOnSingleClickListener {
            mRealVM.powerCalculate(analyzeResult?.charCount){
                analyzeResult?.estimatedPower = it?.toDouble() ?: 0.0
                uploadCallback?.invoke(mRealVM.fileName.get(), mRealVM.fileUrl, analyzeResult)
                dismissAllowingStateLoss()
            }
        }
    }

    /**
     * 申请权限并保存图片
     */
    private fun requestReadPermission(callback: () -> Unit) {
        requestRead2WritePermissions { granted ->
            if (granted) {
                //权限申请成功
                callback.invoke()
            } else {
                //权限申请失败
            }
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnClose -> dismissAllowingStateLoss()
        }
    }

    // 1. 定义 Launcher
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                // 在协程中处理，防止卡顿
                lifecycleScope.launch(Dispatchers.IO) {
                    it.getFileMeta(context).let { meta ->
                        mRealVM.fileName.set(meta.first + "." + meta.second)
                        val allowedExtensions = listOf("txt")//, "doc", "docx"
                        if (meta.second !in allowedExtensions) {
                            withContext(Dispatchers.Main) {
                                getString(R.string.unsupported_file_format_please_select_txt).showToast()
                            }
                            return@launch
                        }
                    }
                    val result = TextEncodingUtil.createUtf8TempFileAndAnalyze(mActivity, uri)
                    analyzeResult = result.analysisResult

                    // 增强型就绪检查：最多尝试 5 次，总计 1.5 秒
                    var isFileReady = false
                    val tempFile = result.tempFile
                    if (tempFile != null && tempFile.exists()) {
                        repeat(5) {
                            try {
                                // 只要这行不报错，说明文件已经物理闭合且可读
                                FileInputStream(tempFile).use { stream -> stream.read() }
                                isFileReady = true
                                return@repeat // 成功了就跳出循环，不用死等
                            } catch (e: Exception) {
                                delay(300) // 没好就再等 300ms
                                e.printStackTrace()
                            }
                        }
                    }

//                    val pathFromUri = uri.getTempFilePathFromUri(mActivity)
                    withContext(Dispatchers.Main) {
                        if ((analyzeResult?.charCount ?: 0) > TextEncodingUtil.MAX_CHAR_COUNT) {
                            mRealVM.uploadErrorType.set(StoryUploadViewModel.UPLOAD_ERROR_TYPE_FILE_CHAT_LIMIT)
                            mRealVM.uploadStatus.set(STATE_UPLOAD_FAIL)
                        } else if ((analyzeResult?.charCount ?: 0) < TextEncodingUtil.MIN_CHAR_COUNT) {
                            mRealVM.uploadErrorType.set(StoryUploadViewModel.UPLOAD_ERROR_TYPE_FILE_CHAT_LIMIT)
                            mRealVM.uploadStatus.set(STATE_UPLOAD_FAIL)
                        } else {
                            mRealVM.startUpload(tempFile?.absolutePath)
                        }
                    }
                }
            }
        }
    }

    // 2. 触发选择（包含权限检查）
    fun openFilePicker() {
        val pickerLauncher = {
            //, "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain" // 设置一个基础类型
                val mimeTypes = arrayOf(
                    "text/plain",
                )
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
            filePickerLauncher.launch(intent)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            requestReadPermission {
                pickerLauncher.invoke()
            }
        } else {
            pickerLauncher.invoke()
        }
    }
}
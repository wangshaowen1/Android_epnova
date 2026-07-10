package com.kuaigeglobal.epnova.feature.creation.childs

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.databinding.FragmentCreationNovelBinding
import com.kuaigeglobal.epnova.feature.creation.CreationFragment.Companion.KEY_MODEL
import com.kuaigeglobal.epnova.feature.creation.CreationFragment.Companion.MODEL_CUSTOM
import com.kuaigeglobal.epnova.feature.creator.CreationPageActionDelegate
import com.kuaigeglobal.epnova.feature.creator.CreationViewModel
import com.nhsd.basic.BaseViewModelFragment

class CreationNovelFragment :
    BaseViewModelFragment<FragmentCreationNovelBinding, CreationViewModel>() {

    companion object {
        fun newInstance() = CreationNovelFragment().apply {
            arguments = Bundle().apply { putInt(KEY_MODEL, MODEL_CUSTOM) }
        }
    }

    private val pageDelegate by lazy {
        CreationPageActionDelegate(
            fragment = this,
            viewModel = mRealVM,
            isInspirationPage = false,
            inputViewProvider = { bindingNullable?.inputView },
            onInputChanged = ::updateInputActions
        )
    }
    override fun onPrepareView() {
        super.onPrepareView()
        mRealVM.enableCreationTabUi()
        pageDelegate.bindInput(binding.inputView)
        pageDelegate.observeCommon(viewLifecycleOwner)
        updateInputActions()
    }

    private fun updateInputActions() {
        bindingNullable?.btnDelete?.isVisible = !mRealVM.input.value.isNullOrEmpty()
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnStoryUpload -> pageDelegate.uploadStory()
            R.id.btnPaste -> pageDelegate.pasteInput()
            R.id.btnDelete -> pageDelegate.deleteInput()
            R.id.btnNext -> pageDelegate.nextStep()
        }
    }

    override fun onDestroy() {
        pageDelegate.clear()
        super.onDestroy()
    }
}
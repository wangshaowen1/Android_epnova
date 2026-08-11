package com.kuaigeglobal.epnova.base

import android.os.Bundle
import android.view.View
import androidx.databinding.ViewDataBinding
import com.nhsd.basic.DialogFragmentViewModelOwner
import com.nhsd.basic.ext.handleDialogUiEvent
import com.nhsd.basic.ext.handleDialogUiState
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.setupDialogFragmentViewModel
import com.nhsd.basic.viewmodel.BaseLayoutViewModel
import com.nhsd.basic.viewmodel.ClickHandler
import com.nhsd.basic.viewmodel.UiEvent
import com.nhsd.basic.viewmodel.UiState
import com.nhsd.basic.viewmodel.ViewModelFactory

//
//onAttach
//onCreate
//onCreateDialog
//setupDialog
//onCreateView
//onViewCreated
//onActivityCreated
//onStart
// onStop
//onDestroyView
//onDestroy
//onDetach
abstract class BaseBlurVMDialogFragment<VB : ViewDataBinding, VM : BaseLayoutViewModel>(
    private var useSharedViewModel: Boolean = false
) : BaseBlurDialogFragment<VB>(), DialogFragmentViewModelOwner {

    val mRealVM: VM by lazy {
        createViewModel()
    }

    private fun handleViewModelLifecycle(event: (VM) -> Unit) {
        if (!useSharedViewModel) {
            event(mRealVM)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!useSharedViewModel) {
            mRealVM.setBundle(arguments ?: Bundle())
        }
        handleViewModelLifecycle { it.onCreate() }
    }


    override fun onResume() {
        super.onResume()
        handleViewModelLifecycle { it.onResume() }
    }

    override fun onPause() {
        super.onPause()
        handleViewModelLifecycle { it.onPause() }
    }

    override fun onStart() {
        super.onStart()
        handleViewModelLifecycle { it.onStart() }
    }

    override fun onStop() {
        super.onStop()
        handleViewModelLifecycle { it.onStop() }
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
        if (isAdded && isActive && !useSharedViewModel) {
            mRealVM.setBundle(arguments ?: Bundle())
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()

        // 使用扩展函数设置 ViewModel
        setupDialogFragmentViewModel(
            useSharedViewModel = useSharedViewModel,
            binding = binding,
            viewModel = mRealVM,
            onViewModelCreated = { onViewModelCreated() },
            onUiStateChanged = { state -> onDialogUiStateChanged(state) },
            onUiEventReceived = { event -> onDialogUiEventReceived(event) },
            onClick = { view, data -> onClick(view, data) }
        )
    }

    // 接口默认实现
    override fun onViewModelCreated() {}

    override fun onDialogUiStateChanged(state: UiState) {
        handleDialogUiState(useSharedViewModel,state)
    }

    override fun onDialogUiEventReceived(event: UiEvent) {
        handleDialogUiEvent(useSharedViewModel,event)
    }


    override fun onDestroy() {
        if (useSharedViewModel) {
            mRealVM.clickHandler?.removeListeners(clickListener)
        }
        binding.unbind()
        super.onDestroy()
    }

    private val clickListener = object : ClickHandler.OnClickListener {
        override fun <Data> onClick(view: View, data: Data?) {
            onClick(view, data)
        }
    }

    protected open fun createViewModel(): VM {
        return when {
            useSharedViewModel && parentFragment != null -> {
                ViewModelFactory.DEFAULT.create(parentFragment)
                    ?: throw IllegalStateException("ViewModel creation failed")
            }

            useSharedViewModel -> {
                ViewModelFactory.DEFAULT.create(mActivity)
                    ?: throw IllegalStateException("ViewModel creation failed")
            }

            else -> ViewModelFactory.DEFAULT.create(this)
                ?: throw IllegalStateException("ViewModel creation failed")
        }
    }
}

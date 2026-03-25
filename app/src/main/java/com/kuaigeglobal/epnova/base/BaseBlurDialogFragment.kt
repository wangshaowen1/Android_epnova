package com.kuaigeglobal.epnova.base

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.nhsd.basic.BaseActivity
import com.nhsd.basic.PageHandler
import com.nhsd.basic.R
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import com.nhsd.basic.viewmodel.BindingFactory
import com.kuaigeglobal.epnova.widget.blurdialog.SupportBlurDialogFragment

//
//onAttach
//onCreate
//onCreateDialog
//setupDialog
//onCreateView
//onViewCreated
//onActivityCreated
//onStart
//onResume
// onStop
//onDestroyView
//onDestroy
//onDetach
open class BaseBlurDialogFragment<V : ViewBinding> : SupportBlurDialogFragment(), PageHandler {
    private var onDismissListener: DialogInterface.OnDismissListener? = null

    protected lateinit var binding: V

    private var mLayoutView: View? = null
    protected var mActivity: BaseActivity? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BaseActivity) {
            mActivity = context
        }
        Logger.i("fragment onAttach:$this")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), R.style.Dialog_Common).apply {
            setCanceledOnTouchOutside(false)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = createBinding()
    }

    protected open fun createBinding(): V {
        return BindingFactory.DEFAULT.createBinding(this, layoutInflater) as V
    }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        if (mLayoutView == null) {
            mLayoutView = binding.root
            onPrepareView()
        } else {
            val parent = mLayoutView!!.parent as ViewGroup
            parent.removeView(mLayoutView)
            onRefreshData()
        }
        return mLayoutView
    }

    /**
     * 只运行一次
     */
    open fun onPrepareView() {}
    open fun onRefreshData() {}
    open val isShowing: Boolean
        get() = dialog != null && dialog!!.isShowing

    open fun show(manager: FragmentManager) {
        if (!isAdded) {
            val ft = manager.beginTransaction()
            ft.add(this, javaClass.name)
            ft.commitAllowingStateLoss()
        } else {
            dialog?.show()
        }
    }

    fun setOnDismissListener(onDismissListener: DialogInterface.OnDismissListener?) {
        this.onDismissListener = onDismissListener
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (onDismissListener != null) {
            onDismissListener!!.onDismiss(dialog)
        }
    }

    fun hide() {
        dialog?.hide()
    }

    override fun showPageError(message: String, retryAction: (() -> Unit)?) {

    }

    override fun hidePageError() {
    }

    override fun showLoadingDialog() {
        mActivity?.showLoadingDialog()
    }

    override fun hideLoadingDialog() {
        mActivity?.hideLoadingDialog()
    }

    override fun showMessage(message: String?) {
        message?.showToast()
    }

}
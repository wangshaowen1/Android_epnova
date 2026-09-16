package com.kuaigeglobal.epnova.feature.creator.set.allstyle

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoStyleConfigInfo
import com.kuaigeglobal.epnova.databinding.DialogCreationStyleChooseBinding
import com.kuaigeglobal.epnova.widget.decoration.BottomSpaceDecoration
import com.kuaigeglobal.epnova.widget.decoration.GridSpacingItemDecoration
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.toArrayList


class CreationStyleChooseDialog : BaseBottomPushVMDialogFragment<DialogCreationStyleChooseBinding, CreationStyleChooseViewModel>() {

    companion object {
        fun newInstance(lastChooseStyleId: String? = null, list: List<VideoStyleConfigInfo>?): CreationStyleChooseDialog {
            val fragment = CreationStyleChooseDialog()
            fragment.arguments = bundleOf(
                Constants.Extra.ID to lastChooseStyleId,
                Constants.Extra.DATA to list?.toArrayList()
            )
            return fragment
        }
    }

    var onSuccess: (VideoStyleConfigInfo?) -> Unit = {}
    val adapter by lazy { AllCreationStyleAdapter(mRealVM.lastStyleId) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also {
            // 隐藏底部导航栏或手势条
            it.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false) // 设置全屏
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
//                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars()) // 隐藏system bars
                windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars()) // 隐藏navigation bars
                // 上滑显示后自动隐藏行为
                windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.loadState.showContent()
        binding.recyclerView.addItemDecoration(
            GridSpacingItemDecoration(
                4,
                4.dp,
                6.dp
            )
        )
//        binding.recyclerView.layoutManager = GridLayoutManager(mActivity, 4).apply {
//            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
//                override fun getSpanSize(position: Int): Int {
//                    val adapter = binding.recyclerView.adapter
//                    if (adapter is ConcatAdapter) {
//                        if (adapter.getAdapterByItemPosition(position) is MyMultiStatePagingFooterAdapter) {
//                            return spanCount
//                        }
//                    }
//                    return 1
//                }
//            }
//        }
        binding.recyclerView.addItemDecoration(BottomSpaceDecoration(40.dp))
//        binding.recyclerView.adapter = adapter.withLoadStateFooter(
//            MyMultiStatePagingFooterAdapter(
//                hostAdapter = adapter,
//                loadMoreLoadEnd = "",
//                refresh = {
//                    adapter.retry()
//                },
//            )
//        )
//        binding.loadState.startRefreshWithAdapter(adapter)
        binding.btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.close.setOnClickListener { dismissAllowingStateLoss() }
        binding.btnConfirm.setOnClickListener {
//            onSuccess.invoke(adapter.itemVm?.data?.get())
            onSuccess.invoke(mRealVM.checkedStyle?.data?.get())
            dismissAllowingStateLoss()
        }
    }


}
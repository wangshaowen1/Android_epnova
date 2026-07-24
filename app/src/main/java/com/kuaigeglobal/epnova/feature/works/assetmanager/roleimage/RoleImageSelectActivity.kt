package com.kuaigeglobal.epnova.feature.works.assetmanager.roleimage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.core.network.apis.entity.AssetInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.RoleImageInfo
import com.kuaigeglobal.epnova.databinding.ActivityRoleImageSelectBinding
import com.kuaigeglobal.epnova.widget.decoration.GridSpacingItemDecoration
import com.kuaigeglobal.epnova.widget.decoration.SpaceDecoration
import com.kuaigeglobal.epnova.widget.stateful.applyGridSpanSizeLookup
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.BaseItemEventsViewModel
import kotlinx.coroutines.launch

/**
 * 角色形象图片选择
 */
class RoleImageSelectActivity : BaseViewModelActivity<ActivityRoleImageSelectBinding, RoleImageSelectViewModel>() {


    companion object {
        const val TAG = "RoleImageSelectActivity"

        @JvmStatic
        fun getIntent(context: Context?, workId: String?, storyId: String?, info: AssetInfo?, selectModel: Boolean = false): Intent? {
            if (context == null) return null
            val starter = Intent(context, RoleImageSelectActivity::class.java)
            starter.putExtra(Constants.Extra.DATA, info)
            starter.putExtra(Constants.Extra.WORK_ID, workId)
            starter.putExtra(Constants.Extra.STORY_ID, storyId)
            starter.putExtra(Constants.Extra.SELECT_MODE, selectModel)
            return starter
        }

        @JvmStatic
        fun start(context: Context?, workId: String?, storyId: String?, info: AssetInfo?) {
            context?.startActivity(getIntent(context, workId, storyId, info))
        }
    }


    val itemClickVm by lazy { BaseItemEventsViewModel<RoleImageEvent, RoleImageInfo>() }
    val imageAdapter by lazy { RoleImageAdapter(mRealVM.selectModel, itemClickVm, mRealVM.roleInfo?.assetImageId) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        dataUpdate()
        initListener()
    }

    private fun initView() {
        binding.recyclerView.apply {
            addItemDecoration(SpaceDecoration(14.dp))
            addItemDecoration(GridSpacingItemDecoration(3, 12.dp, 0))
            isFocusable = false
            adapter =
                imageAdapter.withLoadStateFooter(
                    MyMultiStatePagingFooterAdapter(
                        hostAdapter = imageAdapter,
                        noMoreThreshold = 12
                    ) { imageAdapter.retry() })
            applyGridSpanSizeLookup()
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = imageAdapter
        )
    }

    private fun dataUpdate() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mRealVM.getRoleImages().collect {
                    imageAdapter.submitData(it)
                }
            }
        }

        lifecycleScope.launch {
            itemClickVm.events.collect {
                when (it) {
                    is RoleImageEvent.Click -> {
                        //AssetDetailActivity.start(this@RoleImageSelectActivity, mRealVM.workId, mRealVM.roleInfo, it.item.data.get())
                    }

                    is RoleImageEvent.Select -> {
                        mRealVM.mSelectedItem = it.item?.data?.get()
                    }
                }
            }
        }
    }

    fun initListener() {
        binding.btnCancel.setOnSingleClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.btnConfirm.setOnSingleClickListener {
            if (mRealVM.mSelectedItem != null) {
                setResult(RESULT_OK, Intent().apply {
                    putExtra(Constants.Extra.DATA, mRealVM.roleInfo)
                    putExtra(Constants.Extra.ROLE_IMAGE, mRealVM.mSelectedItem)
                })
            }
            finish()
        }
    }
}

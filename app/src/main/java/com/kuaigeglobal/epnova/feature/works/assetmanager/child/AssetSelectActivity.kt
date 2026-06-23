package com.kuaigeglobal.epnova.feature.works.assetmanager.child

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.core.network.apis.entity.AssetInfo
import com.kuaigeglobal.epnova.databinding.ActivityAssetSelectBinding
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
 * 角色，场景，道具图片选择
 */
class AssetSelectActivity : BaseViewModelActivity<ActivityAssetSelectBinding, AssetSelectViewModel>() {


    companion object {
        const val TAG = "AssetSelectActivity"

        @JvmStatic
        fun getIntent(context: Context?, bizType: Int, workId: String?, storyId: String?, assetImageId: String?, assetId: String? = null): Intent? {
            if (context == null) return null
            val starter = Intent(context, AssetSelectActivity::class.java)
            starter.putExtra(Constants.Extra.TYPE, bizType)
            starter.putExtra(Constants.Extra.WORK_ID, workId)
            starter.putExtra(Constants.Extra.STORY_ID, storyId)
            starter.putExtra(Constants.Extra.ASSET_ID, assetId)
            starter.putExtra(Constants.Extra.ASSET_IMAGE_ID, assetImageId)
            return starter
        }
    }

    val assetImageId by lazy { intent.getStringExtra(Constants.Extra.ASSET_IMAGE_ID) }
    val assetId by lazy { intent.getStringExtra(Constants.Extra.ASSET_ID) }
    val itemClickVm by lazy { BaseItemEventsViewModel<AssetsEvent, AssetInfo>() }
    val assetsChildAdapter by lazy { AssetsChildAdapter(true, itemClickVm, assetImageId) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        dataUpdate()
        initListener()
    }

    fun initListener() {
        binding.btnCancel.setOnSingleClickListener {
            onBackPressed()
        }
        binding.btnConfirm.setOnSingleClickListener {
            if (mRealVM.selectedItem != null) {
                setResult(RESULT_OK, Intent().apply {
                    putExtra(Constants.Extra.DATA, mRealVM.selectedItem)
                    putExtra(Constants.Extra.ASSET_ID, assetId)
                })
            }
            finish()
        }
    }

    private fun initView() {
        binding.recyclerView.apply {
            addItemDecoration(SpaceDecoration(14.dp))
            itemAnimator = null
            isFocusable = false
            if (mRealVM.bizType == AssetInfo.IMAGE_BIZ_TYPE_SCENE) {
                layoutManager = GridLayoutManager(this@AssetSelectActivity, 2)
                addItemDecoration(GridSpacingItemDecoration(2, 12.dp, 0))
            } else {
                addItemDecoration(GridSpacingItemDecoration(3, 12.dp, 0))
            }
            assetsChildAdapter.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            adapter =
                assetsChildAdapter.withLoadStateFooter(
                    MyMultiStatePagingFooterAdapter(
                        hostAdapter = assetsChildAdapter,
                        noMoreThreshold = 12
                    ) { assetsChildAdapter.retry() })
            applyGridSpanSizeLookup()
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = assetsChildAdapter
        )
    }

    private fun dataUpdate() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mRealVM.getAssetsPage().collect {
                    assetsChildAdapter.submitData(it)
                }
            }
        }

        lifecycleScope.launch {
            itemClickVm.events.collect {
                when (it) {
                    is AssetsEvent.Click -> {

                    }

                    is AssetsEvent.Select -> {
                        mRealVM.selectedItem = it.item?.data?.get()
                    }
                }
            }
        }
    }
}

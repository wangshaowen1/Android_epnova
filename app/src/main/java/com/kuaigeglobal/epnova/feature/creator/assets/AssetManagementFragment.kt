package com.kuaigeglobal.epnova.feature.creator.assets

import android.app.Activity
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.Observable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.applySmartFocusManager
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.entity.RoleImageInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleAsset
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleRoleInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleSceneInfo
import com.kuaigeglobal.epnova.core.network.apis.request.QueryAssetBody
import com.kuaigeglobal.epnova.databinding.FragmentAssetManagementBinding
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.creator.generate.OnBackPressedHandler
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorActivity
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorViewModel
import com.kuaigeglobal.epnova.feature.mine.child.MyCreationsFragment
import com.kuaigeglobal.epnova.feature.series.SeriesActivity
import com.kuaigeglobal.epnova.feature.works.assetmanager.child.AssetSelectActivity
import com.kuaigeglobal.epnova.feature.works.assetmanager.roleimage.RoleImageSelectActivity
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.getParentViewModel
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.showToast

/**
 * 资产管理
 */
class AssetManagementFragment : BaseViewModelFragment<FragmentAssetManagementBinding, AssetManagementViewModel>(), OnBackPressedHandler {

    companion object {
        fun newInstance() = AssetManagementFragment()
    }

    private var firstItem: Any? = null
        set(value) {
            field = value
            when (value) {
                is ItemRoleVM -> bindingNullable?.rgAssetType?.check(R.id.rbRoles)
                is ItemSceneVM -> bindingNullable?.rgAssetType?.check(R.id.rbScenes)
                is ItemPropVM -> bindingNullable?.rgAssetType?.check(R.id.rbProps)
            }
        }
    private val layoutManager get() = bindingNullable?.rvAssets?.layoutManager as? LinearLayoutManager
    private val hostVm by lazy { getParentViewModel<StoryEditorViewModel>() }
    val from by lazy { hostVm?.from }
    val imageSelectLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val simpleAsset = result.data?.getParcelableValue(Constants.Extra.DATA, SimpleAsset::class.java)
            val assetId = result.data?.getStringExtra(Constants.Extra.ASSET_ID)
            mRealVM.saveImage(simpleAsset, null, assetId)
        }
    }

    val roleImageSelectLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val roleInfo = result.data?.getParcelableValue(Constants.Extra.DATA, SimpleRoleInfo::class.java)
            val roleImageInfo = result.data?.getParcelableValue(Constants.Extra.ROLE_IMAGE, RoleImageInfo::class.java)
            mRealVM.saveImage(roleInfo, roleImageInfo)
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        EventManager.onFirebaseTrack(FirebaseEvent.ASSET_MANAGEMENT_VIEW)
        binding.root.applySmartFocusManager()
        mRealVM.workId = hostVm?.workId
        mRealVM.storyId = hostVm?.storyId
        mRealVM.taskId = hostVm?.taskId
        mRealVM.step.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                Global.refreshCoinsAndPoints()
            }
        })
        mRealVM.retry()
        binding.rvAssets.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                layoutManager?.findFirstVisibleItemPosition()?.let { position ->
                    val item = mRealVM.adapter.getItem(position)
                    if (item?.javaClass != firstItem?.javaClass) {
                        firstItem = item
                    }
                }
            }
        })
        mRealVM.onAssetImageSelectEvent = { vm ->
            val asset = vm.data.get()
            val bizType = when (asset) {
                is SimpleRoleInfo -> QueryAssetBody.BIZ_TYPE_ROLE
                is SimpleSceneInfo -> QueryAssetBody.BIZ_TYPE_SCENE
                else -> QueryAssetBody.BIZ_TYPE_PROP
            }
            if (bizType == QueryAssetBody.BIZ_TYPE_ROLE) {
                roleImageSelectLauncher.launch(RoleImageSelectActivity.getIntent(activity, mRealVM.workId, asset as? SimpleRoleInfo, true))
            } else {
                imageSelectLauncher.launch(AssetSelectActivity.getIntent(activity, bizType, mRealVM.workId, asset?.assetAppearanceId, asset?.assetId))
            }
        }
        observeEvents {
            on<Events.RoleGenerateEvent> { if (isActive) mRealVM.onRoleGenerate(it) }
            on<Events.SceneGenerateEvent> { if (isActive) mRealVM.onSceneGenerate(it) }
            on<Events.PropGenerateEvent> { if (isActive) mRealVM.onPropGenerate(it) }
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.rbRoles -> {
                scrollToRoles()
            }

            R.id.rbScenes -> {
                scrollToScenes()
            }

            R.id.rbProps -> {
                scrollToProps()
            }

            R.id.btnNext -> {
                mRealVM.clickNext { workId, storyId, taskId ->
                    hostVm?.nextStep(4, workId, storyId, taskId)
                }
            }
        }
    }

    private fun scrollToRoles() {
        mRealVM.adapter.items.indexOfFirst { it is ItemRoleVM }.takeIf { it > -1 }?.let {
            layoutManager?.scrollToPositionWithOffset(it, 0)
        }
    }

    private fun scrollToScenes() {
        mRealVM.adapter.items.indexOfFirst { it is ItemSceneVM }.takeIf { it > -1 }?.let {
            layoutManager?.scrollToPositionWithOffset(it, 0)
        }
    }

    private fun scrollToProps() {
        mRealVM.adapter.items.indexOfFirst { it is ItemPropVM }.takeIf { it > -1 }?.let {
            layoutManager?.scrollToPositionWithOffset(it, 0)
        }
    }

    override fun handleBackPressed(): Boolean {
        if (mRealVM.generating || mRealVM.step.get() == AssetManagementViewModel.STEP_IMAGE_GENERATING) {
            getString(R.string.ai_generating_auto_saved).showToast()
        } else {
            getString(R.string.auto_saved_as_draft).showToast()
        }
        if (hostVm?.model == DramaGenerateConstants.MODEL_STORY_SINGLE) {
            if (from != StoryEditorActivity.FROM_MY_WORKS) {
                MyCreationsFragment.start()
            } else {
                mActivity.finish()
            }
        } else {
            if (from != StoryEditorActivity.FROM_SERIES) {
                SeriesActivity.start(mRealVM.workId)
            }
            mActivity.finish()
        }
        return true
    }

}

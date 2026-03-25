package com.kuaigeglobal.epnova.feature.creator.assets

import android.view.View
import androidx.databinding.Observable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.applySmartFocusManager
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.manager.AppEvent
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.databinding.FragmentAssetManagementBinding
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.creator.generate.OnBackPressedHandler
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorActivity
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorViewModel
import com.kuaigeglobal.epnova.feature.series.SeriesActivity
import com.kuaigeglobal.epnova.feature.works.MyWorksFragment
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
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

    override fun onPrepareView() {
        super.onPrepareView()
        binding.root.applySmartFocusManager()
        mRealVM.workId = hostVm?.workId
        mRealVM.storyId = hostVm?.storyId
        mRealVM.taskId = hostVm?.taskId
        mRealVM.step.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                Global.refreshBalance()
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
        EventManager.onAppTrack(AppEvent.SF_VIEW_ASSET_MANAGEMENT_PAGE)
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
                MyWorksFragment.start()
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
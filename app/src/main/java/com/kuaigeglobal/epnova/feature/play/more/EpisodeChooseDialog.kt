package com.kuaigeglobal.epnova.feature.play.more

import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.databinding.DialogEpisodeChooseBinding
import com.kuaigeglobal.epnova.widget.decoration.GridSpacingItemDecoration
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.dp

class EpisodeChooseDialog :
    BaseBottomPushVMDialogFragment<DialogEpisodeChooseBinding, EpisodeChooseViewModel>() {

    companion object {
        fun newInstance(
            title: String?,
            cover: String?,
            browsers: String?,
            totalEpisodes: Int?,
            currentEpisode: Int = 1
        ): EpisodeChooseDialog {
            val fragment = EpisodeChooseDialog()
            fragment.arguments = bundleOf(
                Constants.Extra.TITLE to title,
                Constants.Extra.COVER to cover,
                Constants.Extra.BROWSERS to browsers,
                Constants.Extra.COUNT to totalEpisodes,
                Constants.Extra.POSITION to currentEpisode
            )
            return fragment
        }
    }

    var onEpisodeSelected: (EpisodeInfo?) -> Unit = {}

    override fun onPrepareView() {
        super.onPrepareView()

        setupRangeTabRecyclerView()
        setupEpisodeRecyclerView()
        setupListeners()
        observeScrollEvent()

        // 初始滚动到当前选中集数的位置
        scrollToCurrentEpisode()

        mRealVM.onEpisodeSelected = {
            onEpisodeSelected.invoke(it)
            dismissAllowingStateLoss()
        }
    }

    private fun setupRangeTabRecyclerView() {
        binding.rvRangeTabs.layoutManager = LinearLayoutManager(
            mActivity, LinearLayoutManager.HORIZONTAL, false
        )
        // adapter已经通过databinding绑定
    }

    private fun setupEpisodeRecyclerView() {
        val gridLayoutManager = GridLayoutManager(mActivity, 6)
        binding.recyclerView.layoutManager = gridLayoutManager
        binding.recyclerView.addItemDecoration(
            GridSpacingItemDecoration(6, 8.dp, 10.dp)
        )

        // 监听滚动，更新标签选中状态
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return
                val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                if (firstVisiblePosition != RecyclerView.NO_POSITION) {
                    mRealVM.updateRangeTabByPosition(firstVisiblePosition)
                    // 确保标签可见
                    scrollRangeTabToVisible()
                }
            }
        })
    }

    private fun setupListeners() {
        binding.close.setOnClickListener { dismissAllowingStateLoss() }

//        binding.btnConfirm.setOnClickListener {
//            val selectedEpisode = mRealVM.checkedEpisode?.data?.get()?.episodeNumber
//            if (selectedEpisode != null) {
//                onEpisodeSelected.invoke(selectedEpisode)
//            }
//            dismissAllowingStateLoss()
//        }
    }

    private fun observeScrollEvent() {
        mRealVM.scrollToPosition.observe(this) { position ->
            if (position != null && position >= 0) {
                val layoutManager = binding.recyclerView.layoutManager as? GridLayoutManager
                layoutManager?.scrollToPositionWithOffset(position, 0)
            }
        }
    }

    private fun scrollToCurrentEpisode() {
        binding.recyclerView.post {
            val currentIndex = mRealVM.currentEpisode - 1
            if (currentIndex >= 0) {
                // 定位到当前集所在range的起始位置
                val rangeStartIndex = (currentIndex / EpisodeChooseViewModel.RANGE_SIZE) * EpisodeChooseViewModel.RANGE_SIZE
                val layoutManager = binding.recyclerView.layoutManager as? GridLayoutManager
                layoutManager?.scrollToPositionWithOffset(rangeStartIndex, 0)
            }
        }
    }

    private fun scrollRangeTabToVisible() {
        val checkedIndex = mRealVM.rangeVms.indexOfFirst { it.selected.get() }
        if (checkedIndex >= 0) {
            binding.rvRangeTabs.smoothScrollToPosition(checkedIndex)
        }
    }
}
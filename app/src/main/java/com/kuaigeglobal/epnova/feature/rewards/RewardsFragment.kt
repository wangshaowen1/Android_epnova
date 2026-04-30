package com.kuaigeglobal.epnova.feature.rewards

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.databinding.FragmentRewardsBinding
import com.kuaigeglobal.epnova.widget.decoration.GridSpacingItemDecoration
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp

class RewardsFragment : BaseViewModelFragment<FragmentRewardsBinding, RewardsViewModel>() {

    companion object {
        fun newInstance() = RewardsFragment()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        mRealVM.loadData()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) mRealVM.loadData()
    }

    private fun initView() {
        // 签到格子：7列 Grid
        binding.rvCheckIn.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.rvCheckIn.addItemDecoration(GridSpacingItemDecoration(7, 2.dp, 0))
        binding.rvCheckIn.adapter = mRealVM.checkInAdapter

        // 任务列表：垂直 Linear
        binding.rvEarnRewards.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEarnRewards.adapter = mRealVM.earnAdapter
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnCheckIn -> mRealVM.doCheckIn()
            R.id.btnRules ->
                RewardRulesDialog.newInstance().show(childFragmentManager)
        }
    }
}

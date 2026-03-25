package com.kuaigeglobal.epnova.feature.details

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.databinding.FragmentPointsDetailBinding
import com.kuaigeglobal.epnova.widget.decoration.TopSpaceDecoration
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import kotlinx.coroutines.launch

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:余额明细
 * @Data: 2025/9/10/周三  17:49
 * @Version: 1.0
 * *****************************************
 */
class ComputingPowerDetailsFragment :
    BaseViewModelFragment<FragmentPointsDetailBinding, ComputingPowerDetailsFragmentViewModel>() {

    companion object {
        //类型
        const val TYPE_PROTAGONIST = "typeProtagonist"

        //全部
        const val TYPE_ALL = 0

        //充值明细
        const val TYPE_RECHARGE = 1

        //消耗明细
        const val TYPE_CONSUME = 2
        fun newInstance(typeProtagonist: Int = TYPE_ALL): ComputingPowerDetailsFragment {
            val fragment = ComputingPowerDetailsFragment()
            val bundle = Bundle()
            bundle.putInt(TYPE_PROTAGONIST, typeProtagonist)
            fragment.arguments = bundle
            return fragment
        }
    }


    //明细
    private val computingPowerDetailsAdapter by lazy {
        ComputingPowerDetailsAdapter()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        binding.recyclerView.apply {
            addItemDecoration(TopSpaceDecoration(14.dp))
            isFocusable = false
            adapter =
                computingPowerDetailsAdapter.withLoadStateFooter(
                    MyMultiStatePagingFooterAdapter(
                        hostAdapter = computingPowerDetailsAdapter,
                        noMoreThreshold = 5
                    ) { computingPowerDetailsAdapter.retry() })
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = computingPowerDetailsAdapter
        )
    }


    private fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.getComputingPowerDetailsPage().collect {
                computingPowerDetailsAdapter.submitData(it)
            }
        }
    }
}
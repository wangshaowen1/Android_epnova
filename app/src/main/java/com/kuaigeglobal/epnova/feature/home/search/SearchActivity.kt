package com.kuaigeglobal.epnova.feature.home.search

import android.app.Activity
import android.content.Intent
import android.view.inputmethod.EditorInfo
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.databinding.ActivitySearchBinding
import com.blankj.utilcode.util.KeyboardUtils
import com.nhsd.basic.BaseViewModelActivity

class SearchActivity :
    BaseViewModelActivity<ActivitySearchBinding, SearchViewModel>() {

    companion object {

        fun start(activity: Activity?) {
            val activity = activity ?: topActivity
            val intent = Intent(activity, SearchActivity::class.java)
            activity?.startActivity(intent)
        }
    }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        initView()
        dataUpdate()
    }

    private fun initView() {
        binding.recyclerView.adapter = mRealVM.resultAdapter
        binding.btnSearch.post {
            binding.btnSearch.requestFocus()
            KeyboardUtils.showSoftInput(binding.btnSearch)
        }
        binding.btnSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                mRealVM.search()
                KeyboardUtils.hideSoftInput(binding.btnSearch)
                true
            } else false
        }
    }

    private fun dataUpdate() {

    }
}

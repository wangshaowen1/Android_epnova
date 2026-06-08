package com.kuaigeglobal.epnova.feature.settings.lang

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.kuaigeglobal.epnova.databinding.ActivityLanguagesBinding
import com.nhsd.basic.BaseViewModelActivity


class LanguagesActivity : BaseViewModelActivity<ActivityLanguagesBinding, LanguagesViewModel>() {
    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            activity.startActivity(Intent(activity, LanguagesActivity::class.java))
        }
    }

    private lateinit var adapter: LanguagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = LanguagesAdapter(languagesConfigs, mRealVM.selectedCode) { item ->
            mRealVM.selectLanguage(item.code)
            adapter.updateSelected(item.code)
        }
        binding.recyclerView.adapter = adapter

        mRealVM.doneEvent.observe(this) {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }
}

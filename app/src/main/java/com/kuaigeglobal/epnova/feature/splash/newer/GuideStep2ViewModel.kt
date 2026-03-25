package com.kuaigeglobal.epnova.feature.splash.newer

import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.requestWithLoading
import com.kuaigeglobal.epnova.core.network.apis.entity.ExampleWorkInfo

class GuideStep2ViewModel : AppRepositoryViewModel<AppRepository>() {

    val examples = MutableLiveData<List<ExampleWorkInfo>>()

    override fun onCreate() {
        super.onCreate()
        getExamples()
    }

    fun getExamples() {
        requestWithLoading({ mRepo.api.getNewerExampleWorkList() }) {
            examples.value = data
        }
    }
}
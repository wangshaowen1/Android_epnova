package com.kuaigeglobal.epnova.feature.create

import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.AppRepository

class CreateViewModel : AppRepositoryViewModel<AppRepository>() {

    var editModel = MutableLiveData(false)

    override fun onCreate() {
        super.onCreate()
    }
}

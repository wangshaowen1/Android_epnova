package com.kuaigeglobal.epnova.feature.works

import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.AppRepository

class MyListModel : AppRepositoryViewModel<AppRepository>() {

    var editModel = MutableLiveData(false)
}
package com.kuaigeglobal.epnova.feature.mine

import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.User
import com.nhsd.basic.ext.getResString


class PhoneShowViewModel : AppRepositoryViewModel<AppRepository>() {
    var toolbar = ToolbarViewModel(navigationAction = { back() }, title = R.string.phone_number.getResString())
    val phone = User.get().phoneNum


}
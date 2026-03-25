package com.kuaigeglobal.epnova.base.viewmodel

import com.nhsd.basic.viewmodel.BaseLayoutViewModel
import com.nhsd.basic.viewmodel.VariableId
import com.kuaigeglobal.epnova.BR


open class AppViewModel : BaseLayoutViewModel(), VariableId {

    override fun id(): Int = BR.layout

}
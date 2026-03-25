package com.kuaigeglobal.epnova.base.viewmodel

import com.kuaigeglobal.epnova.BR
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.requestWithLoading
import com.kuaigeglobal.epnova.core.network.apis.request.ContentBody
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.viewmodel.BaseRepositoryViewModel

abstract class AppRepositoryViewModel<T : AppRepository> : BaseRepositoryViewModel<T>(){
    override fun id(): Int = BR.layout

 }
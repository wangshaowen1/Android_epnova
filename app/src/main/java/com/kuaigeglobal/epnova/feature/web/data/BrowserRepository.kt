package com.kuaigeglobal.epnova.feature.web.data


import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.model.H5PayInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BrowserRepository : AppRepository() {

    suspend fun h5PayAlipay(info: H5PayInfo) = withContext(Dispatchers.IO) {
        api.h5PayAlipay(info)
    }

    suspend fun h5PayWx(info: H5PayInfo) = withContext(Dispatchers.IO) {
        api.h5PayWx(info)
    }

}
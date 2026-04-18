package com.kuaigeglobal.epnova.feature.creator.data

import com.kuaigeglobal.epnova.core.data.CommonRepository
import com.kuaigeglobal.epnova.core.network.apis.request.ContentBody
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class CreationRepository : CommonRepository() {

    suspend fun getInspirationQuickList() = withContext(Dispatchers.IO) {
        api.getInspirationQuickList(UserTokenBody())
    }

    suspend fun videoSetConfig() = withContext(Dispatchers.IO) {
        api.videoSetConfig(UserTokenBody())
    }

    /**
     * 敏感词校验
     */
    suspend fun sensitiveWordCheck(content: String?) = withContext(Dispatchers.IO) {
        api.sensitiveWordCheck(ContentBody(content))
    }
}


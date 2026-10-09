package com.kuaigeglobal.epnova.feature.settings.lang.data

import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.request.LanguagesSettingBody
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LanguagesRepository : AppRepository() {
    /**
     * 设置App语言
     * App 切换语言后调用，服务端按当前登录用户和设备保存最新语言，用于后续消息推送模板语言选择。
     */
    suspend fun setAppLanguage(code: String) = withContext(Dispatchers.IO) { api.setAppLanguage(LanguagesSettingBody(languageCode = code)) }
}
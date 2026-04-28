package com.kuaigeglobal.epnova.core

import android.content.Context
import android.text.TextUtils
import com.blankj.utilcode.util.NetworkUtils
import com.google.gson.Gson
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.BuildConfig
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.common.utils.DateUtil
import com.kuaigeglobal.epnova.common.utils.MmkvDelegate
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory.apiService
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.response.AppConfigData
import com.kuaigeglobal.epnova.core.network.apis.response.TotalComposeConfigData
import com.nhsd.basic.Session
import com.nhsd.basic.Session.NetworkChangedListener
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.launchNoCrash
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * App配置信息与角色是否登录或切换无关 (已重构为 MMKV 委托实现)
 */
class Config : NetworkChangedListener {

    private val mmkv: MMKV by lazy { MMKV.mmkvWithID(PREF_ID, MMKV.MULTI_PROCESS_MODE) }
    private val gson = Gson()
    private var isRequesting = false
    private var appConfigData: AppConfigData? = null

    val initialState: Int
        get() = if (BuildConfig.DEBUG) UAT else ONLINE

    private fun ifInitialStatusInReview(): Boolean {
        return Session.getInstance().channel == "huawei"
    }

    // --- 使用 MmkvDelegate 代理所有基础属性 ---

    var isFirstOpenApp: Boolean by MmkvDelegate(FIRST_OPEN_APP, true) { mmkv }
        private set
    var isFirstSetBookVoice: Boolean by MmkvDelegate(FIRST_SET_BOOK_VOICE, true) { mmkv }
        private set
    var isFirstEyeProtectionSetting: Boolean by MmkvDelegate(FIRST_EYE_PROTECTION_SETTING, true) { mmkv }
        private set
    var objectId: String? by MmkvDelegate(OBJECT_ID, "1") { mmkv }
        private set
    var isInReview: Boolean by MmkvDelegate(IN_REVIEW, ifInitialStatusInReview()) { mmkv }
        private set
    var environment: Int by MmkvDelegate(ENVIRONMENT, initialState) { mmkv }
        private set
    var isDevelopmentMode: Boolean by MmkvDelegate(DEVELOPMENT_MODE, false) { mmkv }
        private set
    var lang: String by MmkvDelegate(APP_LANG, "en") { mmkv }
        private set
    var episodeAutoUnlock: Boolean by MmkvDelegate(EPISODE_AUTO_UNLOCK, true) { mmkv }
        private set
    var isNotificationEnable: Boolean by MmkvDelegate(NOTIFICATION_ENABLE, true) { mmkv }
        private set
    var isShowJumpPop: Boolean by MmkvDelegate(SHOW_JUMP_POP, true) { mmkv }
        private set
    var baseUrlCustom: String? by MmkvDelegate(Constants.BASE_URL_CUSTOM, BuildConfig.SERVER_URL) { mmkv }
        private set
    var deviceToken: String? by MmkvDelegate(DEVICE_TOKEN, null) { mmkv }
        private set
    var installData: String? by MmkvDelegate(INSTALL_DATA, null) { mmkv }
        private set
    var opid: String? by MmkvDelegate(OPID, null) { mmkv }
        private set
    var eyeProtectionEnabled: Boolean by MmkvDelegate(EYE_PROTECTION_ENABLE, false) { mmkv }
        private set
    var referrer: String? by MmkvDelegate(GOOGLE_INSTALL_REFERRER, null) { mmkv }
        private set

    /**
     * 数据迁移初始化，建议在 App.onCreate() 中调用: Config.get().init(this)
     */
    fun init(context: Context) {
        val oldSp = context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)

        // 如果旧 SP 还有数据并且 MMKV 还没迁移过（以 FIRST_OPEN_APP 存在为标志），则执行迁移
        if (oldSp.all.isNotEmpty() && !mmkv.containsKey(FIRST_OPEN_APP)) {
            val editor = oldSp.edit()
            for ((key, value) in oldSp.all) {
                when (value) {
                    is String -> mmkv.encode(key, value)
                    is Int -> mmkv.encode(key, value)
                    is Long -> mmkv.encode(key, value)
                    is Float -> mmkv.encode(key, value)
                    is Boolean -> mmkv.encode(key, value)
                }
            }
            editor.clear().apply()
        }
    }

    fun initAppConfig() {
        Session.getInstance().addNetworkChangedListener(this)
        updateConfig()
    }

    fun updateConfig() {
        if (NetworkUtils.isConnected() && !isRequesting) {
            isRequesting = true
            ioScope.launchNoCrash(block = {
                val response = apiService.appConfig(UserTokenBody())
                if (response.isSuccess) {
                    withContext(Dispatchers.Main) {
                        Session.getInstance().removeNetworkChangedListener(this@Config)
                    }
                    storeAppConfig(response.getData())
                }
                val shelfStatusData = apiService.getAppShelfStatus().data
                storeInReview(shelfStatusData?.shelfStatus == 2)
                isRequesting = false
            }, onError = {
                isRequesting = false
            })
        }
    }

    override fun onNetworkStateChanged(networkAvailable: Boolean) {
        updateConfig()
    }

    val appConfig: AppConfigData
        get() {
            if (appConfigData == null) {
                updateConfig()
                val json = mmkv.decodeString(APP_CONFIG, "")
                if (!json.isNullOrEmpty()) {
                    appConfigData = gson.fromJson(json, AppConfigData::class.java)
                }
            }
            if (appConfigData == null) {
                appConfigData = DEFAULT_APP_CONFIG
            }
            return this.appConfigData!!
        }

    fun storeAppConfig(data: AppConfigData?) {
        this.appConfigData = data
        mmkv.encode(APP_CONFIG, gson.toJson(data))
    }

    fun storeComposeConfig(data: TotalComposeConfigData?) {
        mmkv.encode(COMPOSE_CONFIG, gson.toJson(data))
    }

    val totalComposeConfig: TotalComposeConfigData?
        get() {
            val json = mmkv.decodeString(COMPOSE_CONFIG, "")
            if (json.isNullOrEmpty()) return null
            return gson.fromJson(json, TotalComposeConfigData::class.java)
        }

    val serviceUrl: String?
        get() {
            if (appConfigData != null && !TextUtils.isEmpty(appConfigData!!.userProtocolUrl)) {
                return appConfigData!!.userProtocolUrl
            }
            return Constants.SERVICE_URL
        }

    val privacyUrl: String?
        get() {
            if (appConfigData != null && !TextUtils.isEmpty(appConfigData!!.privacyProtocolUrl)) {
                return appConfigData!!.privacyProtocolUrl
            }
            return Constants.PRIVACY_URL
        }

    val userVipProtocolUrl
        get() = appConfigData?.userVipProtocolUrl ?: Constants.DEFAULT_USER_VIP_PROTOCOL_URL

    val algorithmUrl: String?
        get() {
            return appConfigData?.algorithmUrl ?: Constants.DEFAULT_ALGORITHM_ANNOUNCEMENT
        }

    fun notifyChange(vararg keys: String?) {
        FlowEventBus.postSync(Events.ConfigChangeEvent(keys))
    }

    private fun storeInReviewWithNotify(inReview: Boolean) {
        if (this.isInReview != inReview) {
            this.isInReview = inReview
            notifyChange(IN_REVIEW)
        }
    }

    fun markAppOpenedWithNotify() {
        this.isFirstOpenApp = false
        notifyChange(FIRST_OPEN_APP)
    }

    fun markFirstSetBookVoiceWithNotify() {
        this.isFirstSetBookVoice = false
        notifyChange(FIRST_SET_BOOK_VOICE)
    }

    fun markFirstProtectionSettingWithNotify() {
        this.isFirstEyeProtectionSetting = false
        notifyChange(FIRST_EYE_PROTECTION_SETTING)
    }

    fun clear() {
        val keysToRemove = arrayOf(
            FIRST_OPEN_APP, IN_REVIEW, ENVIRONMENT, DEVELOPMENT_MODE,
            INSTALL_DATA, OPID, SHOW_JUMP_POP, EYE_PROTECTION_ENABLE
        )
        mmkv.removeValuesForKeys(keysToRemove)

        // 重置内存变量以匹配逻辑
        this.isFirstOpenApp = true
        this.isInReview = true
        this.environment = this.initialState
        this.isDevelopmentMode = false
        this.isShowJumpPop = true
        this.eyeProtectionEnabled = false
    }

    // --- 保留对外的方法名，由于用了委托，大部分直接赋值即可自动写入 MMKV ---

    fun storeReferrer(referrer: String?) { this.referrer = referrer }
    fun storeObjectId(objectId: String?) { this.objectId = objectId }
    fun storeInReview(inReview: Boolean) { this.isInReview = inReview }
    fun storeDeviceToken(deviceToken: String?) { this.deviceToken = deviceToken }
    fun storeOpid(opid: String?) { this.opid = opid }
    fun storeDevelopmentMode(developMode: Boolean) { this.isDevelopmentMode = developMode }
    fun storeNotificationEnable(notificationEnable: Boolean) { this.isNotificationEnable = notificationEnable }
    fun markJumpPopShown() { this.isShowJumpPop = false }
    fun storeBaseUrlCustom(baseUrlCustom: String?) { this.baseUrlCustom = baseUrlCustom }
    fun storeEyeProtectionEnabled(eyeProtectionEnabled: Boolean) { this.eyeProtectionEnabled = eyeProtectionEnabled }
    fun saveLanguage(langCode: String) { this.lang = langCode }
    fun saveEpisodeAutoUnlock(isAutoUnlock: Boolean) { this.episodeAutoUnlock = isAutoUnlock }
    fun getSavedLanguage(): String = this.lang
    fun isEpisodeAutoUnlock(): Boolean = this.episodeAutoUnlock

    val isDebug: Boolean
        get() = if (this.isDevelopmentMode) true else BuildConfig.DEBUG_MODEL

    /**
     * 通过OpenInstall安装
     */
    fun installFromOpenInstall(): Boolean {
        try {
            val jsonStr = installData
            if (jsonStr.isNullOrEmpty()) return false
            val json = JSONObject(jsonStr)
            val data = JSONObject(json.getString("data"))
            return data.has("op_cc") || data.has("ad_account_id")
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return false
    }

    // --- 以下涉及动态拼接 Key 的方法，直接使用 mmkv.decode/encode ---

    fun activityPopupShowCount(id: String?): Int {
        if (id.isNullOrEmpty()) return 0
        val lastShowTime = mmkv.decodeLong("activity_popup_show_time_$id", 0L)
        return if (DateUtil.isToday(lastShowTime)) {
            mmkv.decodeInt("activity_popup_show_count_$id", 0)
        } else {
            0
        }
    }

    fun activityPopupShowMark(id: String?) {
        if (id.isNullOrEmpty()) return
        val lastShowTime = mmkv.decodeLong("activity_popup_show_time_$id", 0L)
        val currentCount = if (DateUtil.isToday(lastShowTime)) {
            mmkv.decodeInt("activity_popup_show_count_$id", 0)
        } else {
            0
        }
        mmkv.encode("activity_popup_show_count_$id", currentCount + 1)
        mmkv.encode("activity_popup_show_time_$id", System.currentTimeMillis())
    }

    fun activityPopupShowTime(id: String?): Long {
        return mmkv.decodeLong("activity_popup_show_time_$id", 0L)
    }

    fun activityFloatShowCount(id: String?): Int {
        if (id.isNullOrEmpty()) return 0
        val lastShowTime = mmkv.decodeLong("activity_float_show_time_$id", 0L)
        return if (DateUtil.isToday(lastShowTime)) {
            mmkv.decodeInt("activity_float_show_count_$id", 0)
        } else {
            0
        }
    }

    fun activityFloatShowMark(id: String?) {
        if (id.isNullOrEmpty()) return
        val lastShowTime = mmkv.decodeLong("activity_float_show_time_$id", 0L)
        val currentCount = if (DateUtil.isToday(lastShowTime)) {
            mmkv.decodeInt("activity_float_show_count_$id", 0)
        } else {
            0
        }
        mmkv.encode("activity_float_show_count_$id", currentCount + 1)
        mmkv.encode("activity_float_show_time_$id", System.currentTimeMillis())
    }

    fun activityFloatShowTime(id: String?): Long {
        return mmkv.decodeLong("activity_float_show_time_$id", 0L)
    }

    fun notifyPowerConsume(scene: Int): Boolean {
        return mmkv.decodeBool("notifyPowerConsume${User.get().userId}_$scene", true)
    }

    fun notifyPowerConsumeClose(scene: Int) {
        mmkv.encode("notifyPowerConsume${User.get().userId}_$scene", false)
    }

    fun isFirstRechargeShown(scene: Int): Boolean {
        return mmkv.decodeBool("firstRechargeShown${User.get().userId}_$scene", false)
    }

    fun markFirstRechargeShown(scene: Int) {
        mmkv.encode("firstRechargeShown${User.get().userId}_$scene", true)
    }

    companion object {
        private const val PREF_ID = "CONFIG_DATA" // 修正：原来 SP 传入的名称就是这个
        const val FIRST_OPEN_APP: String = "firstOpenApp"
        const val FIRST_SET_BOOK_VOICE: String = "firstSetBookVoice"
        const val FIRST_EYE_PROTECTION_SETTING: String = "firstEyeProtectionSetting"
        private const val OBJECT_ID = "objectId"
        const val IN_REVIEW: String = "inReview"
        private const val DEVELOPMENT_MODE = "developmentMode"
        private const val ENVIRONMENT: String = "environment"
        private const val APP_CONFIG = "app_config"
        private const val COMPOSE_CONFIG: String = "COMPOSE_CONFIG"
        private const val INSTALL_DATA = "install_data"
        private const val OPID = "opid"
        private val DEFAULT_APP_CONFIG: AppConfigData = AppConfigData()
        private const val NOTIFICATION_ENABLE = "NOTIFICATION_ENABLE"
        private const val SHOW_JUMP_POP = "showJumpPop"
        private const val GOOGLE_INSTALL_REFERRER = "google_install_referrer"
        private const val APP_LANG = "app_lang"
        private const val EPISODE_AUTO_UNLOCK = "episodeAutoUnlock"
        private const val EYE_PROTECTION_ENABLE: String = "eyeProtectionEnable"
        const val DAILY: Int = 1
        const val UAT: Int = 2
        const val ONLINE: Int = 3
        const val DEVICE_TOKEN = "DEVICE_TOKEN"

        @JvmStatic
        fun get(): Config {
            return App.instance.config
        }
    }
}
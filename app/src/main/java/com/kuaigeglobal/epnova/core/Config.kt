package com.kuaigeglobal.epnova.core

import android.text.TextUtils
import com.blankj.utilcode.util.NetworkUtils
import com.google.gson.Gson
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.BuildConfig
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.SP
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.common.utils.DateUtil
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory.apiService
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.response.AppConfigData
import com.kuaigeglobal.epnova.core.network.apis.response.TotalComposeConfigData
import com.nhsd.basic.Session
import com.nhsd.basic.Session.NetworkChangedListener
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.launchNoCrash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject


/**
 * App配置信息与角色是否登录或切换无关
 */
class Config : NetworkChangedListener {
    private val sp: SP
    var isFirstOpenApp: Boolean
        private set

    var isFirstSetBookVoice: Boolean
        private set

    var isFirstEyeProtectionSetting: Boolean = false
        private set
    var objectId: String?
        private set
    var isInReview: Boolean
        private set
    var environment: Int
        private set
    var isDevelopmentMode: Boolean = false
        private set


    private var isRequesting = false
    private val gson: Gson

    private var appConfigData: AppConfigData? = null


    var isNotificationEnable: Boolean
        private set

    var isShowJumpPop: Boolean
        private set

    /**
     * 测试环境下 自己输入的ip，供后台测试用
     */
    var baseUrlCustom: String?
        private set

    private var deviceToken: String? = null

    var installData: String?
        private set

    var opid: String?
        private set

    var eyeProtectionEnabled: Boolean? = false
        private set

    /**
     * google 安装来源标记
     */
    var referrer: String? = null

    private fun ifInitialStatusInReview(): Boolean {
        when (Session.getInstance().channel) {
            "huawei" ->
                return true
        }
        return false
    }

    init {
        sp = SP("CONFIG_DATA")
        this.isFirstOpenApp = sp.getBoolean(FIRST_OPEN_APP, true)
        this.isFirstSetBookVoice = sp.getBoolean(FIRST_SET_BOOK_VOICE, true)
        this.isFirstEyeProtectionSetting = sp.getBoolean(FIRST_EYE_PROTECTION_SETTING, true)
        objectId = sp.getString(OBJECT_ID, "1")
        this.isInReview =
            if (ifInReviewStatus()) sp.getBoolean(IN_REVIEW, ifInitialStatusInReview()) else false
        environment = sp.getInt(
            ENVIRONMENT,
            this.initialState
        )
        this.isDevelopmentMode = sp.getBoolean(DEVELOPMENT_MODE, false)
        this.isNotificationEnable = sp.getBoolean(NOTIFICATION_ENABLE, true)
        this.isShowJumpPop = sp.getBoolean(SHOW_JUMP_POP, true)
        baseUrlCustom = sp.getString(Constants.BASE_URL_CUSTOM, BuildConfig.SERVER_URL)
        deviceToken = sp.getString(DEVICE_TOKEN, deviceToken)
        installData = sp.getString(INSTALL_DATA, null)
        opid = sp.getString(OPID, null)
        eyeProtectionEnabled = sp.getBoolean(EYE_PROTECTION_ENABLE, false)
        referrer = sp.getString(GOOGLE_INSTALL_REFERRER, null)
        gson = Gson()
    }

    private fun ifInReviewStatus(): Boolean {
        val buildTime = BuildConfig.BUILD_TIMESTAMP
        val timeDiff = System.currentTimeMillis() - buildTime
        return timeDiff < REVIEW_OUT_TIME
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

    val initialState: Int
        get() = if (BuildConfig.DEBUG) UAT else ONLINE

    val appConfig: AppConfigData
        get() {
            if (appConfigData == null) {
                updateConfig()
                val json =
                    sp.getString(APP_CONFIG, "")
                appConfigData = gson.fromJson(json, AppConfigData::class.java)
            }
            if (appConfigData == null) {
                appConfigData =
                    DEFAULT_APP_CONFIG
            }
            return this.appConfigData!!
        }

    fun storeAppConfig(data: AppConfigData?) {
        this.appConfigData = data
        sp.putString(APP_CONFIG, gson.toJson(data))
    }

    fun storeComposeConfig(data: TotalComposeConfigData?) {
        sp.putString(COMPOSE_CONFIG, gson.toJson(data))
    }

    val totalComposeConfig: TotalComposeConfigData?
        get() {
            val json =
                sp.getString(COMPOSE_CONFIG, "")
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

    fun storeReferrer(referrer: String?) {
        this.referrer = referrer
        sp.putString(GOOGLE_INSTALL_REFERRER, referrer)
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


    fun storeObjectId(objectId: String?) {
        this.objectId = objectId
        sp.putString(OBJECT_ID, objectId)
    }

    private fun storeInReviewWithNotify(inReview: Boolean) {
        if (this.isInReview != inReview) {
            storeInReview(inReview)
            notifyChange(IN_REVIEW)
        }
    }

    fun storeInReview(inReview: Boolean) {
        this.isInReview = inReview
        sp.putBoolean(IN_REVIEW, inReview)
    }


    fun markAppOpenedWithNotify() {
        sp.putBoolean(FIRST_OPEN_APP, false)
        this.isFirstOpenApp = false
        notifyChange(FIRST_OPEN_APP)
    }

    fun markFirstSetBookVoiceWithNotify() {
        sp.putBoolean(FIRST_SET_BOOK_VOICE, false)
        this.isFirstSetBookVoice = false
        notifyChange(FIRST_SET_BOOK_VOICE)
    }

    fun markFirstProtectionSettingWithNotify() {
        sp.putBoolean(FIRST_EYE_PROTECTION_SETTING, false)
        this.isFirstEyeProtectionSetting = false
        notifyChange(FIRST_EYE_PROTECTION_SETTING)
    }

    fun storeDeviceToken(deviceToken: String?) {
        this.deviceToken = deviceToken
        sp.putString(DEVICE_TOKEN, deviceToken)
    }

    fun clear() {
        this.isFirstOpenApp = true
        sp.remove(FIRST_OPEN_APP)


        this.isInReview = true
        sp.remove(IN_REVIEW)

        environment = this.initialState
        sp.remove(ENVIRONMENT)
        this.isDevelopmentMode = false
        sp.remove(DEVELOPMENT_MODE)
        sp.remove(INSTALL_DATA)
        sp.remove(OPID)
        this.isShowJumpPop = true
        sp.remove(SHOW_JUMP_POP)
        this.eyeProtectionEnabled = false
        sp.remove(EYE_PROTECTION_ENABLE)
    }

//    fun storeInstallData(data: AppData?) {
//        if (data == null || data.isEmpty) {
//            return
//        }
//        installData = gson.toJson(data)
//        sp.putString(INSTALL_DATA, installData)
//    }

    fun storeOpid(opid: String?) {
        this.opid = opid
        sp.putString(OPID, opid)
    }


    fun storeDevelopmentMode(developMode: Boolean) {
        this.isDevelopmentMode = developMode
        sp.putBoolean(DEVELOPMENT_MODE, developMode)
    }


    val isDebug: Boolean
        get() = if (this.isDevelopmentMode) true else BuildConfig.DEBUG_MODEL

    fun storeNotificationEnable(notificationEnable: Boolean) {
        this.isNotificationEnable = notificationEnable
        sp.putBoolean(NOTIFICATION_ENABLE, notificationEnable)
    }

    fun markJumpPopShown() {
        this.isShowJumpPop = false
        sp.putBoolean(SHOW_JUMP_POP, false)
    }

    fun storeBaseUrlCustom(baseUrlCustom: String?) {
        this.baseUrlCustom = baseUrlCustom
        sp.putString(Constants.BASE_URL_CUSTOM, baseUrlCustom)
    }

    fun storeEyeProtectionEnabled(eyeProtectionEnabled: Boolean) {
        if (this.eyeProtectionEnabled != eyeProtectionEnabled) {
            this.eyeProtectionEnabled = eyeProtectionEnabled
            sp.putBoolean(EYE_PROTECTION_ENABLE, eyeProtectionEnabled)
        }
    }

    /**
     * 通过OpenInstall安装
     * {"channel":"","data":"{\"i\":\"zRTWqE9g\",\"p\":\"3\"}"}
     */
    fun installFromOpenInstall(): Boolean {
        try {
            val json = JSONObject(installData)
            val data = JSONObject(json.getString("data"))
            return data.has("op_cc") || data.has("ad_account_id")
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return false
    }

    fun activityPopupShowCount(id: String?): Int {
        if (id.isNullOrEmpty()) return 0

        val lastShowTime = sp.getLong("activity_popup_show_time_$id", 0L)
        // 关键点：如果上次展示时间不是今天，之前的计数就没意义了，直接返回 0
        return if (DateUtil.isToday(lastShowTime)) {
            sp.getInt("activity_popup_show_count_$id", 0)
        } else {
            0
        }
    }

    fun activityPopupShowMark(id: String?) {
        if (id.isNullOrEmpty()) return

        val lastShowTime = sp.getLong("activity_popup_show_time_$id", 0L)
        val currentCount = if (DateUtil.isToday(lastShowTime)) {
            sp.getInt("activity_popup_show_count_$id", 0)
        } else {
            0 // 如果是跨天后的第一次展示，重置计数
        }
        sp.putInt("activity_popup_show_count_$id", currentCount + 1)
        sp.putLong("activity_popup_show_time_$id", System.currentTimeMillis())
    }

    fun activityPopupShowTime(id: String?): Long {
        return sp.getLong("activity_popup_show_time_$id", 0L)
    }

    fun activityFloatShowCount(id: String?): Int {
        if (id.isNullOrEmpty()) return 0
        val lastShowTime = sp.getLong("activity_float_show_time_$id", 0L)
        return if (DateUtil.isToday(lastShowTime)) {
            sp.getInt("activity_float_show_count_$id", 0)
        } else {
            0
        }
    }

    fun activityFloatShowMark(id: String?) {
        if (id.isNullOrEmpty()) return
        val lastShowTime = sp.getLong("activity_float_show_time_$id", 0L)
        val currentCount = if (DateUtil.isToday(lastShowTime)) {
            sp.getInt("activity_float_show_count_$id", 0)
        } else {
            0
        }
        sp.putInt("activity_float_show_count_$id", currentCount + 1)
        sp.putLong("activity_float_show_time_$id", System.currentTimeMillis())
    }

    fun activityFloatShowTime(id: String?): Long {
        return sp.getLong("activity_float_show_time_$id", 0L)
    }

    fun notifyPowerConsume(scene: Int): Boolean {
        return sp.getBoolean("notifyPowerConsume${User.get().userId}_$scene", true)
    }

    fun notifyPowerConsumeClose(scene: Int) {
        sp.putBoolean("notifyPowerConsume${User.get().userId}_$scene", false)
    }

    fun isFirstRechargeShown(scene: Int): Boolean {
        return sp.getBoolean("firstRechargeShown${User.get().userId}_$scene", false)
    }

    fun markFirstRechargeShown(scene: Int) {
        sp.putBoolean("firstRechargeShown${User.get().userId}_$scene", true)
    }

    // 建议存储的值： "en", "th", "id"
    fun saveLanguage(langCode: String) {
        sp.putString("app_lang", langCode)
    }

    fun getSavedLanguage(): String {
        return sp.getString("app_lang", "en") ?: "en"
    }


    companion object {
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

        private const val EYE_PROTECTION_ENABLE: String = "eyeProtectionEnable"
        const val DAILY: Int = 1
        const val UAT: Int = 2
        const val ONLINE: Int = 3
        const val DEVICE_TOKEN = "DEVICE_TOKEN"

        private const val REVIEW_OUT_TIME = (5 * 24 * 60 * 60 * 1000).toLong()

        @JvmStatic
        fun get(): Config {
            return App.instance.config
        }
    }
}

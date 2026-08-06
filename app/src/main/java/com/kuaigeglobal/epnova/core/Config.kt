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
import com.kuaigeglobal.epnova.core.manager.LocaleHelper
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.model.StoryOutlineStreamCache
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory.apiService
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.response.AppConfigData
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
    var objectId: String? by MmkvDelegate(OBJECT_ID, "1") { mmkv }
        private set
    var isInReview: Boolean by MmkvDelegate(IN_REVIEW, ifInitialStatusInReview()) { mmkv }
        private set
    var environment: Int by MmkvDelegate(ENVIRONMENT, initialState) { mmkv }
        private set
    var isDevelopmentMode: Boolean by MmkvDelegate(DEVELOPMENT_MODE, false) { mmkv }
        private set
    var lang: String by MmkvDelegate(APP_LANG, LocaleHelper.resolveDefaultLanguage()) { mmkv }
        private set
    var episodeAutoUnlock: Boolean
        get() = episodeAutoUnlockKey()?.let { mmkv.decodeBool(it, true) } ?: true
        private set(value) {
            episodeAutoUnlockKey()?.let { mmkv.encode(it, value) }
        }
    var isNotificationEnable: Boolean by MmkvDelegate(NOTIFICATION_ENABLE, true) { mmkv }
        private set
    var baseUrlCustom: String? by MmkvDelegate(Constants.BASE_URL_CUSTOM, BuildConfig.SERVER_URL) { mmkv }
        private set
    var deviceToken: String? by MmkvDelegate(DEVICE_TOKEN, null) { mmkv }
        private set
    var installData: String? by MmkvDelegate(INSTALL_DATA, null) { mmkv }
        private set
    var opid: String? by MmkvDelegate(OPID, null) { mmkv }
        private set
    var regChannelCode: String? by MmkvDelegate(REG_CHANNEL_CODE, null) { mmkv }
        private set
    var referrer: String? by MmkvDelegate(GOOGLE_INSTALL_REFERRER, null) { mmkv }
        private set


    var loginRewardsDialog: String? by MmkvDelegate(TODAY_LOGIN_REWARDS_DIALOG, null) { mmkv }
    var loginRewards: String? by MmkvDelegate(LOGIN_REWARDS, null) { mmkv }

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
                storeSwitchModelEnable(shelfStatusData?.dramaCategorySwitch == 1)
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
        if (this.isFirstOpenApp) {
            EventManager.onFirebaseTrack(FirebaseEvent.Category.FIRST_OPEN, FirebaseEvent.Action.OPEN)
        }
        this.isFirstOpenApp = false
        notifyChange(FIRST_OPEN_APP)
    }

    // --- 保留对外的方法名，由于用了委托，大部分直接赋值即可自动写入 MMKV ---

    fun storeReferrer(referrer: String?) {
        this.referrer = referrer
    }

    fun storeObjectId(objectId: String?) {
        this.objectId = objectId
    }

    fun storeInReview(inReview: Boolean) {
        this.isInReview = inReview
    }

    fun storeDeviceToken(deviceToken: String?) {
        this.deviceToken = deviceToken
    }

    fun storeOpid(opid: String?) {
        this.opid = opid
    }

    fun storeRegChannelCode(regChannelCode: String?) {
        this.regChannelCode = regChannelCode
    }

    fun storeDevelopmentMode(developMode: Boolean) {
        this.isDevelopmentMode = developMode
    }

    fun storeNotificationEnable(notificationEnable: Boolean) {
        this.isNotificationEnable = notificationEnable
    }

    fun storeBaseUrlCustom(baseUrlCustom: String?) {
        this.baseUrlCustom = baseUrlCustom
    }

    fun saveLanguage(langCode: String) {
        this.lang = LocaleHelper.normalizeLanguageCode(langCode)
    }

    fun saveEpisodeAutoUnlock(isAutoUnlock: Boolean) {
        this.episodeAutoUnlock = isAutoUnlock
    }

    fun getSavedLanguage(): String {
        val normalized = LocaleHelper.normalizeLanguageCode(this.lang)
        if (normalized != this.lang) {
            this.lang = normalized
        }
        return normalized
    }

    fun isEpisodeAutoUnlock(): Boolean = this.episodeAutoUnlock

    private fun episodeAutoUnlockKey(): String? {
        val userId = User.get().userId?.takeIf { it.isNotBlank() } ?: return null
        return "${EPISODE_AUTO_UNLOCK}_$userId"
    }

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

    fun storeSwitchModelEnable(enable: Boolean) {
        mmkv.encode("switch_model_enable", enable)
    }

    fun isSwitchModelEnable(): Boolean {
        return mmkv.decodeBool("switch_model_enable", false)
    }

    /**
     * 保存故事梗概未完成的 SSE 展示状态。
     *
     * 这里使用 Config 的 MMKV，和项目内其它轻量配置保持一致。
     * cache=null 表示生成已结束或需要 reset，直接删除对应 workId 的缓存。
     */
    fun storeStoryOutlineStreamCache(workId: String?, cache: StoryOutlineStreamCache?) {
        if (workId.isNullOrEmpty()) return
        val cacheKey = getStoryOutlineStreamCacheKey(workId)
        if (cache == null) {
            mmkv.removeValueForKey(cacheKey)
        } else {
            mmkv.encode(cacheKey, gson.toJson(cache))
        }
    }

    /**
     * 读取故事梗概流式缓存。
     *
     * 反序列化失败时返回 null，页面会按无缓存处理，重新通过 session 判断后端任务状态。
     */
    fun getStoryOutlineStreamCache(workId: String?): StoryOutlineStreamCache? {
        if (workId.isNullOrEmpty()) return null
        val json = mmkv.decodeString(getStoryOutlineStreamCacheKey(workId), null) ?: return null
        return runCatching {
            gson.fromJson(json, StoryOutlineStreamCache::class.java)
        }.getOrNull()
    }

    /**
     * 清理故事梗概缓存。final 成功、业务失败、reset=true 或用户重新生成都会走这里。
     */
    fun clearStoryOutlineStreamCache(workId: String?) {
        storeStoryOutlineStreamCache(workId, null)
    }

    // workId 参与 key，避免不同作品的未完成梗概流互相覆盖。
    private fun getStoryOutlineStreamCacheKey(workId: String): String {
        return "${STORY_OUTLINE_STREAM_CACHE}_$workId"
    }

    fun clear() {
        val keysToRemove = arrayOf(
            FIRST_OPEN_APP,
            IN_REVIEW, ENVIRONMENT, DEVELOPMENT_MODE,
            INSTALL_DATA, OPID, REG_CHANNEL_CODE, SHOW_JUMP_POP,
            TODAY_LOGIN_REWARDS_DIALOG, LOGIN_REWARDS
        )
        mmkv.removeValuesForKeys(keysToRemove)
    }

    companion object {
        private const val PREF_ID = "CONFIG_DATA" // 修正：原来 SP 传入的名称就是这个
        const val FIRST_OPEN_APP: String = "firstOpenApp"
        private const val OBJECT_ID = "objectId"
        const val IN_REVIEW: String = "inReview"
        private const val DEVELOPMENT_MODE = "developmentMode"
        private const val ENVIRONMENT: String = "environment"
        private const val APP_CONFIG = "app_config"
        private const val INSTALL_DATA = "install_data"
        private const val OPID = "opid"
        private const val REG_CHANNEL_CODE = "reg_channel_code"
        private val DEFAULT_APP_CONFIG: AppConfigData = AppConfigData()
        private const val NOTIFICATION_ENABLE = "NOTIFICATION_ENABLE"
        private const val SHOW_JUMP_POP = "showJumpPop"
        private const val GOOGLE_INSTALL_REFERRER = "google_install_referrer"
        private const val APP_LANG = "app_lang"
        private const val EPISODE_AUTO_UNLOCK = "episodeAutoUnlock"
        private const val TODAY_LOGIN_REWARDS_DIALOG = "today_login_rewards_dialog"
        private const val LOGIN_REWARDS = "login_rewards"
        private const val STORY_OUTLINE_STREAM_CACHE = "story_outline_stream_cache"
        const val DAILY: Int = 1
        const val UAT: Int = 2
        const val ONLINE: Int = 3
        const val DEVICE_TOKEN = "DEVICE_TOKEN"

        /**
         * Application.attachBaseContext 阶段 App.instance.config 还未初始化，
         * BasicLocaleConfig 需要通过 Context 直接读取已保存语言。
         */
        fun readSavedLanguage(context: Context): String {
            val mmkv = MMKV.mmkvWithID(PREF_ID, MMKV.MULTI_PROCESS_MODE)
            val savedLanguage = mmkv.decodeString(APP_LANG, null)
                ?: context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                    .getString(APP_LANG, null)
            return LocaleHelper.normalizeLanguageCode(savedLanguage ?: LocaleHelper.resolveDefaultLanguage())
        }

        @JvmStatic
        fun get(): Config {
            return App.instance.config
        }
    }
}

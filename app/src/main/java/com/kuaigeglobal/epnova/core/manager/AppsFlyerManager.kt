package com.kuaigeglobal.epnova.core.manager

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.deeplink.DeepLinkResult
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.router.AppRouter
import com.nhsd.basic.utils.Logger
import org.json.JSONObject

/**
 * @Introduce: AppsFlyer SDK 管理器，负责初始化、启动及 DeepLink 路由
 *
 * 使用方式：
 * 1. App.onCreate() 中调用 init(app)
 * 2. 用户同意隐私协议后调用 start(context)
 *
 * DeepLink 参数约定（在 AppsFlyer 控制台配置 OneLink 时使用）：
 *   deep_link_value = 路由 host，如 "retoreCreator"、"bookGenerated"
 *   其余参数按正常 query 附加，如 workId、storyId 等
 *   示例链接：https://epnova.onelink.me/xxx?deep_link_value=retoreCreator&workId=xxx&storyId=yyy
 */
object AppsFlyerManager {

    private const val TAG = "AppsFlyerManager"

    private const val AF_DEV_KEY = "xzV4xPhHz7PUq8Ng69BDzW"
    private const val REG_CHANNEL_SOURCE = "appsflyer"

    // AppsFlyer 内部保留字段，构建业务 URI 时过滤掉，避免干扰 AppRouter 的参数解析
    private val AF_RESERVED_KEYS = setOf(
        "deep_link_value", "deep_link_sub1", "deep_link_sub2", "deep_link_sub3",
        "af_sub1", "af_sub2", "af_sub3", "af_sub4", "af_sub5",
        "pid", "c", "af_siteid", "is_retargeting", "af_click_lookback",
        "af_reengagement_window", "media_source", "campaign", "af_channel",
        "af_adset", "af_ad", "af_ad_type", "af_ad_id", "clickid",
        "install_time", "af_status", "af_message", "match_type",
        "orig_cost", "af_cost_model", "af_cost_value", "af_cost_currency",
        "redirect", "is_mobile", "af_dp", "af_web_dp",
        // AF 运行时注入的内部字段
        "scheme", "host", "link", "af_xp", "shortlink", "source_caller", "is_deferred",
        "af_deeplink"
    )

    private var appContext: Context? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 在 App.onCreate() 中调用，完成 SDK 初始化及 DeepLink 订阅。
     * 必须在 start() 之前调用。
     */
    fun init(app: Application) {
        appContext = app.applicationContext

        // 订阅 Unified Deep Link，必须在 start 之前注册
        AppsFlyerLib.getInstance().subscribeForDeepLink { result ->
            when (result.status) {
                DeepLinkResult.Status.FOUND -> {
                    val deepLink = result.deepLink ?: return@subscribeForDeepLink
                    val deepLinkValue = deepLink.deepLinkValue
                    // clickEvent 是 JSONObject，包含 OneLink 上所有 query 参数
                    val clickEvent = deepLink.clickEvent
                    Logger.i(TAG, "DeepLink found: value=$deepLinkValue clickEvent=$clickEvent")
                    // 路由需在主线程操作 UI/Activity
                    mainHandler.post { handleDeepLink(deepLinkValue, clickEvent) }
                }
                DeepLinkResult.Status.NOT_FOUND -> {
                    Logger.i(TAG, "DeepLink not found")
                }
                else -> {
                    Logger.i(TAG, "DeepLink error: ${result.error}")
                }
            }
        }

        AppsFlyerLib.getInstance().init(
            AF_DEV_KEY,
            object : AppsFlyerConversionListener {
                override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
                    // 安装归因数据（媒体来源、广告系列等），可在此上报
                    Logger.i(TAG, "Conversion data success: $data")
                    buildRegChannelCode(data)?.let {
                        Config.get().storeRegChannelCode(it)
                        Logger.i(TAG, "Reg channel code stored: $it")
                    }
                }

                override fun onConversionDataFail(error: String?) {
                    Logger.i(TAG, "Conversion data fail: $error")
                }

                override fun onAppOpenAttribution(data: MutableMap<String, String>?) {
                    // 旧版 App Open Attribution 回调，UDL 模式下由 subscribeForDeepLink 处理
                    Logger.i(TAG, "App open attribution: $data")
                }

                override fun onAttributionFailure(error: String?) {
                    Logger.i(TAG, "Attribution failure: $error")
                }
            },
            app
        )
    }

    /**
     * 用户同意隐私协议后调用，开始数据采集与归因。
     * 可重复调用，SDK 内部会去重。
     */
    fun start(context: Context) {
        AppsFlyerLib.getInstance().start(context)
        val appsFlyerId = AppsFlyerLib.getInstance().getAppsFlyerUID(context) ?: ""
        Config.get().storeAppsFlyerId(appsFlyerId)
        Logger.i(TAG, "AppsFlyer started: $appsFlyerId")
    }

    fun setUserId(userId: String) {
        AppsFlyerLib.getInstance().setCustomerUserId(userId)
    }

    fun logCompleteRegistration() {
        logEvent(AFInAppEventType.COMPLETE_REGISTRATION)
    }

    fun logPurchase(
        productId: String?,
        orderId: String?,
        priceAmountMicros: Long?,
        currency: String?
    ) {
        val params = hashMapOf<String, Any>()
        productId?.takeIf { it.isNotBlank() }?.let { params[AFInAppEventParameterName.CONTENT_ID] = it }
        orderId?.takeIf { it.isNotBlank() }?.let { params[AFInAppEventParameterName.ORDER_ID] = it }
        priceAmountMicros?.takeIf { it > 0 }?.let { params[AFInAppEventParameterName.REVENUE] = it / 1_000_000.0 }
        currency?.takeIf { it.isNotBlank() }?.let { params[AFInAppEventParameterName.CURRENCY] = it }

        logEvent(AFInAppEventType.PURCHASE, params)
    }

    private fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        val context = appContext
        if (context == null) {
            Logger.w(TAG, "Skip AppsFlyer event, context is null: $eventName")
            return
        }
        val safeParams = params?.takeIf { it.isNotEmpty() }
        runCatching {
            AppsFlyerLib.getInstance().logEvent(context, eventName, safeParams)
        }.onSuccess {
            Logger.i(TAG, "AppsFlyer event logged: $eventName params=$safeParams")
        }.onFailure {
            Logger.w(TAG, "AppsFlyer event log failed: $eventName error=${it.message}")
        }
    }

    /**
     * 只透传后台需要的注册渠道信息，明细归因和素材分析仍以 AppsFlyer 后台为准。
     */
    private fun buildRegChannelCode(data: Map<String, Any>?): String? {
        data ?: return null

        val params = mutableListOf("source" to REG_CHANNEL_SOURCE)
        data.optString("af_status")?.let { params.add("af_status" to it) }
        data.optString("media_source")?.let { params.add("media_source" to it) }
        (data.optString("campaign") ?: data.optString("c"))?.let { params.add("campaign" to it) }

        if (params.size == 1) return null
        return params.joinToString("&") { (key, value) ->
            "${Uri.encode(key)}=${Uri.encode(value)}"
        }
    }

    private fun Map<String, Any>.optString(key: String): String? {
        return this[key]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * 将 AppsFlyer DeepLink 路由到应用内对应页面。
     *
     * deep_link_value 与 AppRouter 中的 host 一一对应：
     *   "retoreCreator"  → 恢复创作页（需 workId、storyId 等参数）
     *   "bookGenerated"  → 作品生成结果弹窗
     *   完整 scheme URI  → AppRouter 通用处理
     *   http/https URL   → 内置浏览器
     */
    private fun handleDeepLink(deepLinkValue: String?, clickEvent: JSONObject?) {
        // 统一以 clickEvent 中的 action 作为路由 host，忽略 deepLinkValue
        val host = clickEvent?.optString("action", "")
        if (host.isNullOrEmpty()) {
            Logger.i(TAG, "action is empty, skip routing")
            return
        }
        val schemeUri = buildSchemeUri(host, clickEvent)
        Logger.i(TAG, "routing schemeUri: $schemeUri")
        AppRouter.jump(schemeUri)
    }

    private fun buildSchemeUri(host: String, clickEvent: JSONObject?): String {
        val builder = StringBuilder("${Constants.APP_SCHEME}://$host")
        if (clickEvent == null) return builder.toString()

        val params = mutableListOf<String>()
        clickEvent.keys().forEach { key ->
            if (key in AF_RESERVED_KEYS || key == "action") return@forEach
            val value = clickEvent.optString(key, "")
            if (value.isNotEmpty()) {
                params.add("${Uri.encode(key)}=${Uri.encode(value)}")
            }
        }

        if (params.isNotEmpty()) {
            builder.append("?").append(params.joinToString("&"))
        }
        return builder.toString()
    }
}

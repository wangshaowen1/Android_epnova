package com.kuaigeglobal.epnova.core.google.event

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kuaigeglobal.epnova.BuildConfig
import com.kuaigeglobal.epnova.core.Config
import com.nhsd.basic.utils.Logger
import okhttp3.Request

object CrashlyticsHelper {

    private const val TAG = "Crashlytics"
    private const val UNKNOWN = "unknown"

    private val crashlytics: FirebaseCrashlytics
        get() = FirebaseCrashlytics.getInstance()

    fun init() {
        runCatching {
            crashlytics.setCustomKey("version_name", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)
            crashlytics.setCustomKey("flavor", BuildConfig.FLAVOR)
            crashlytics.setCustomKey("server_host", Config.get().baseUrlCustom.orEmpty())
            crashlytics.setCustomKey("debug_model", BuildConfig.DEBUG_MODEL)
            log("app_start flavor=${BuildConfig.FLAVOR} version=${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})")
        }
    }

    fun setUserId(userId: String?) {
        runCatching {
            crashlytics.setUserId(userId.orEmpty())
            crashlytics.setCustomKey("user_logged_in", !userId.isNullOrBlank())
        }
    }

    fun logScreen(screenName: String, state: String) {
        runCatching {
            crashlytics.setCustomKey("top_activity", screenName)
            log("screen_$state name=$screenName")
        }
    }

    fun logNetworkFailure(request: Request, throwable: Throwable, durationMs: Long) {
        runCatching {
            val route = request.sanitizedRoute()
            crashlytics.setCustomKey("last_http_method", request.method)
            crashlytics.setCustomKey("last_http_route", route)
            crashlytics.setCustomKey("last_http_status", -1)
            crashlytics.setCustomKey("last_http_duration_ms", durationMs)
            crashlytics.setCustomKey("last_http_exception", throwable.javaClass.simpleName)
            val message = "http_failure method=${request.method} error=${throwable.javaClass.simpleName} duration=${durationMs}ms route=$route"
            log(message)
            logLocal(message)
        }
    }

    private fun log(message: String) {
        runCatching {
            crashlytics.log(message)
        }
    }

    private fun logLocal(message: String) {
        Logger.w(TAG, message)
    }

    private fun Request.sanitizedRoute(): String {
        val path = url.encodedPath
            .split("/")
            .joinToString("/") { segment ->
                if (segment.isLikelyIdentifier()) "{id}" else segment
            }
            .ifBlank { "/" }
        return "${url.host.ifBlank { UNKNOWN }}$path"
    }

    private fun String.isLikelyIdentifier(): Boolean {
        if (length < 12) return false
        return any { it.isDigit() } || contains("-") || contains("_")
    }
}

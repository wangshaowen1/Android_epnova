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

    fun setTopActivity(screenName: String) {
        runCatching {
            crashlytics.setCustomKey("top_activity", screenName)
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

    fun recordVideoSynthesisFailure(
        workId: String?,
        storyId: String?,
        stage: String,
        status: Int,
        reason: String?,
        storyboardCount: Int,
        missingVideoCount: Int,
        firstMissingStoryboardId: String?,
        firstMissingStoryboardSortNo: Int?,
        apiCode: Int? = null,
        apiErrorCode: Int? = null,
        apiRespId: String? = null,
        apiMsg: String? = null
    ) {
        runCatching {
            val safeReason = reason.safeCrashlyticsValue()
            crashlytics.setCustomKey("video_synthesis_work_id", workId.safeCrashlyticsValue(120))
            crashlytics.setCustomKey("video_synthesis_story_id", storyId.safeCrashlyticsValue(120))
            crashlytics.setCustomKey("video_synthesis_stage", stage)
            crashlytics.setCustomKey("video_synthesis_status", status)
            crashlytics.setCustomKey("video_synthesis_reason", safeReason)
            crashlytics.setCustomKey("video_synthesis_storyboard_count", storyboardCount)
            crashlytics.setCustomKey("video_synthesis_missing_video_count", missingVideoCount)
            crashlytics.setCustomKey("video_synthesis_first_missing_storyboard_id", firstMissingStoryboardId.safeCrashlyticsValue(120))
            crashlytics.setCustomKey("video_synthesis_first_missing_storyboard_sort_no", firstMissingStoryboardSortNo ?: 0)
            crashlytics.setCustomKey("video_synthesis_api_code", apiCode ?: 0)
            crashlytics.setCustomKey("video_synthesis_api_error_code", apiErrorCode ?: 0)
            crashlytics.setCustomKey("video_synthesis_api_resp_id", apiRespId.safeCrashlyticsValue(120))
            crashlytics.setCustomKey("video_synthesis_api_msg", apiMsg.safeCrashlyticsValue())

            val message = "video_synthesis_failure stage=$stage status=$status"
            val logMessage = "$message reason=$safeReason"
            log(logMessage)
            logLocal(logMessage)
            crashlytics.recordException(VideoSynthesisFailureException(message))
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

    private fun String?.safeCrashlyticsValue(maxLength: Int = 500): String {
        val value = orEmpty()
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (value.length > maxLength) value.take(maxLength) else value
    }

    private class VideoSynthesisFailureException(message: String) : RuntimeException(message)
}

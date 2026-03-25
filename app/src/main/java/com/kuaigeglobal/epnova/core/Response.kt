package com.kuaigeglobal.epnova.core

import com.nhsd.basic.ext.showToast
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiResponse
import com.kuaigeglobal.epnova.router.AppNavigator
import com.nhsd.basic.bus.FlowEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun <T> responseTest(bean: ApiResponse<T>, result: ApiResponse<T>.() -> Unit) {
    if (bean.isSuccess) {
        result.invoke(bean)
    } else {
        bean.msg?.showToast()
    }
}

fun <T> response(bean: ApiResponse<T>?, result: ApiResponse<T>.() -> Unit) {
    response(bean, result) {
        bean?.msg?.showToast()
    }
}

fun <T> response(
    bean: ApiResponse<T>?,
    result: ApiResponse<T>.() -> Unit,
    error: ApiResponse<T>?.() -> Unit
) {
    if (bean?.isSuccess == true) {
        result.invoke(bean)
    } else {
        if (arrayOf(ApiException.AUTHORIZATION_ERROR, ApiException.LOGGED_OUT_USER, ApiException.USER_ADD_TO_BLACK_LIST).contains(bean?.code)) {
            FlowEventBus.postSync(Events.UserChangedEvent(Events.UserChangedEvent.TOKEN_EXPIRED))
        }
        error.invoke(bean)
    }
}

/**
 * 新增：支持自动切换线程的 response 方法
 * @param block 网络请求的挂起函数 (在 IO 线程执行)
 * @param result 成功回调 (在主线程执行)
 * @param error 失败回调 (可选，在主线程执行)
 */
suspend fun <T> response(
    block: suspend () -> ApiResponse<T>?,
    result: ApiResponse<T>.() -> Unit,
    error: (ApiResponse<T>?.() -> Unit)? = null
) {
    // 1. 在 IO 线程执行网络请求
    val bean = withContext(Dispatchers.IO) {
        block.invoke()
    }

    // 2. 回到主线程调用原有的 response 逻辑处理 (自动处理 token 过期等通用逻辑)
    if (error == null) {
        response(bean, result)
    } else {
        response(bean, result, error)
    }
}



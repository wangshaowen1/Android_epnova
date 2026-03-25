package com.kuaigeglobal.epnova.core.ext

import androidx.lifecycle.ViewModel
import com.nhsd.basic.ext.launch
import com.nhsd.basic.ext.launchWithLoading
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.viewmodel.BaseViewModel
import com.kuaigeglobal.epnova.core.network.apis.ApiResponse
import com.kuaigeglobal.epnova.core.response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun ViewModel.launch(
    showToast: Boolean = false,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return launch({
        block.invoke(this)
    }, {
        //do nothing
        if (showToast) {
            parseMessage(it)?.showToast()
        }
    })
}

fun BaseViewModel.launchWithLoading(
    showLoading: Boolean = true,
    block: suspend CoroutineScope.() -> Unit,
) {
    launchWithLoading(showLoading, {
        block.invoke(this)
    }, {
        if (showLoading) {
            parseMessage(it)?.showToast()
        }
    })
}

/**
 * 自动 launch + 自动切线程 + 自动处理 response
 */
fun <T> BaseViewModel.request(
    apiBlock: suspend () -> ApiResponse<T>?,
    showToast: Boolean = false,
    onError: (Throwable?) -> Unit = {},
    onSuccess: ApiResponse<T>.() -> Unit
): Job {
    return launch(showToast) {
        response(apiBlock, onSuccess) {
            // 这里是 error 回调
            onError.invoke(this?.apiException)
        }
    }
}

/**
 * 功能：
 * 1. 自动开启协程 (ViewModelScope)
 * 2. 自动管理 Loading (可选)
 * 3. 自动捕获异常并 Toast (可选)
 * 4. 自动切换 IO 线程 (依赖 response 方法的内部实现)
 * @param apiBlock  网络请求挂起函数
 * @param showLoading 是否显示 Loading 弹窗
 * @param showToastError 是否出错时自动 Toast
 * @param onError   自定义错误回调
 * @param onSuccess 成功回调
 */
fun <T> BaseViewModel.requestWithLoading(
    apiBlock: suspend () -> ApiResponse<T>?,
    showLoading: Boolean = true,
    showToastError: Boolean = true,
    onError: (Throwable?) -> Unit = {},
    onSuccess: ApiResponse<T>.() -> Unit
): Job {
    return launch(
        block = {
            if (showLoading) setLoading(true)
            try {
                // response 方法内部包含了 withContext(Dispatchers.IO)
                response(apiBlock, onSuccess) {
                    if (showToastError) this?.msg?.showToast()
                    onError(this?.apiException)
                }
            } finally {
                if (showLoading) setLoading(false)
            }
        },
        onError = { e ->
            if (showToastError) parseMessage(e)?.showToast()
            onError(e)
            e.printStackTrace()
        }
    )
}

fun ViewModel.launch(
    block: suspend CoroutineScope.() -> Unit,
    onError: (e: Throwable) -> Unit = { _: Throwable -> },
    onComplete: () -> Unit = {}
): Job {
    return launch(block,onError,onComplete)
}

fun BaseViewModel.launchWithLoading(
    showLoading: Boolean = true,
    block: suspend CoroutineScope.() -> Unit,
    onError: ((e: Throwable) -> Unit)? = null,
) {
   launchWithLoading(showLoading, block, onError)
}


/**
 * 轮询结果状态枚举
 */
enum class PollingState {
    SUCCESS,  // 满足条件，成功结束轮询
    FAIL,     // 出现明确错误（如后端返回失败），结束轮询
    CONTINUE  // 仍在处理中，等待后继续下一次请求
}
fun <T> BaseViewModel.launchPolling(
    intervalMs: Long = 3000L,
    retryOnNetworkError: Boolean = false,
    showToastError: Boolean = true,
    apiBlock: suspend () -> ApiResponse<T>?,
    checkState: (ApiResponse<T>?) -> PollingState,
    onSuccess: (ApiResponse<T>?) -> Unit,
    onFail: (response: ApiResponse<T>?, e: Throwable?) -> Unit = { _, _ -> },
    onPolling: (ApiResponse<T>?) -> Unit = {}// 继续轮询之前可进行操作
): Job {
    return viewModelScope.launch {
        var isPolling = true

        while (isActive && isPolling) {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiBlock.invoke()
                }

                when (checkState(response)) {
                    PollingState.SUCCESS -> {
                        isPolling = false
                        onSuccess(response)
                    }
                    PollingState.FAIL -> {
                        isPolling = false
                        onFail(response, null)
                    }
                    PollingState.CONTINUE -> {
                        onPolling(response)
                        delay(intervalMs) // 挂起，等待下一次轮询
                    }
                }
            } catch (e: Exception) {
                // 必须放行 CancellationException，防止内存泄漏
                if (e is CancellationException) throw e
                e.printStackTrace()
                if (retryOnNetworkError) {
                    onPolling(null)
                    delay(intervalMs)
                } else {
                    isPolling = false
                    // 假设 BaseViewModel 中有 parseMessage 方法
                    if (showToastError) parseMessage(e)?.showToast()
                    onFail(null, e)
                }
            }
        }
    }
}
package com.kuaigeglobal.epnova.core.ext

import com.kuaigeglobal.epnova.core.Events
import com.nhsd.basic.bus.EventObserverScope

/**
 * 扩展：仅监听用户登录成功
 * 自动过滤掉 登出 和 Token过期 事件，防止页面错误刷新
 */
fun EventObserverScope.onUserSignIn(action: () -> Unit) {
    // 调用基础库的 on<T> 方法
    on<Events.UserChangedEvent> { event ->
        if (event.eventType == Events.UserChangedEvent.SIGN_IN) {
            action()
        }
    }
}

/**
 * 扩展：仅监听用户登出
 * 用于页面清空数据、重置 UI
 */
fun EventObserverScope.onUserSignOut(action: () -> Unit) {
    on<Events.UserChangedEvent> { event ->
        if (event.eventType == Events.UserChangedEvent.SIGN_OUT) {
            action()
        }
    }
}

/**
 * 同时处理登录和登出
 * 自动忽略 Token过期，避免双重刷新
 */
fun EventObserverScope.onUserStatusChanged(stateEvent: (eventType: Int) -> Unit) {
    on<Events.UserChangedEvent> { event ->
        when (event.eventType) {
            Events.UserChangedEvent.SIGN_IN -> stateEvent(Events.UserChangedEvent.SIGN_IN)
            Events.UserChangedEvent.SIGN_OUT -> stateEvent(Events.UserChangedEvent.SIGN_OUT)
            // TOKEN_EXPIRED 被这里隐式忽略了，完美！
        }
    }
}
package com.kuaigeglobal.epnova.core.manager

import android.content.Context


interface IRouter {
    /**
     * 处理来自【推送通知】的跳转
     *
     * @param context 上下文
     * @param uri 推送携带的跳转链接 (String 格式更通用)
     * @implNote 实现类必须添加 Intent.FLAG_ACTIVITY_NEW_TASK，防止在某些厂商设备上无法启动 Activity
     */
    fun openFromPush(context: Context, uri: String?)

    /**
     * 应用内常规跳转 (Scheme / Http)
     *
     * @param context 上下文
     * @param path 跳转路径或完整链接
     */
    fun navigate(context: Context, path: String?)
}
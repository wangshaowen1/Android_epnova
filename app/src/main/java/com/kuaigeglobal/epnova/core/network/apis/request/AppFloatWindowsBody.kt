package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:获取APP浮动窗口
 * @Data: 2025/10/14/周二  14:34
 * @Version: 1.0
 * *****************************************
 */
data class AppFloatWindowsBody (
    //手机类型 0-IOS 1-安卓
    val mobileType: Int = 1,
)
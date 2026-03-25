package com.kuaigeglobal.epnova.core.network.apis.response

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:获取APP浮动窗口
 * @Data: 2025/10/14/周二  14:35
 * @Version: 1.0
 * *****************************************
 */
data class AppFloatWindowsInfo(
    //浮窗编码
    val floatIconCode: String? = null,
    //是否打开：0-关闭， 1-打开
    val openStatus: Int? = null,
    //浮动图标地址
    val floatIconUrl: String? = null,
    //跳转类型：1-内部，2-h5
    val jumpType: Int? = null,
    //跳转地址
    val jumpUrl: String? = null,
){

    companion object{
        //首页悬浮窗
        const val FLOAT_ICON_CODE_HOME = "FLOAT_ICON_CODE"
    }
    fun isOpen(): Boolean {
        return openStatus == 1
    }
    fun isInnerJump(): Boolean {
        return jumpType == 1
    }
}
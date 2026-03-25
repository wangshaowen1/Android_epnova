package com.kuaigeglobal.epnova.feature.main

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:主要一些弹框的类型
 * @Data: 2025/9/22/周一  14:31
 * @Version: 1.0
 * *****************************************
 */
enum class MainBulletFrameEnum(val value: Int) {
    //空处理
    DEFAULT(-1),
    //绑定手机号弹框
    BIND_PHONE_NUMBER_POP_UP_BOX(1001),
    //完善个人信息
    COMPLETE_PERSONAL_INFO(1002),
}
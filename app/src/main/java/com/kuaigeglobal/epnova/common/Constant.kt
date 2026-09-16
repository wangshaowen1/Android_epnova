package com.kuaigeglobal.epnova.common

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:一些常量
 * @Data: 2025/5/21  14:10
 * @Version: 1.0
 * *****************************************
 */
object Constant {

    object SDKConfiguration {
        //是否显示微信相关的
        const val IS_SHOW_WE_CHAT = true
    }

    object Paged {
        //分页没一页数量
        const val PAGE_SIZE = 20

        //拉取底部预数量
        const val PREFETCH_DISTANCE = 5
    }

    object GoogleGoodsType {
        const val CREDITS = 1
        const val COIN = 2
    }

    object Switch {
        const val SHOW = 1
        const val HIDE = 0
    }
}
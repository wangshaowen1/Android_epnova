package com.kuaigeglobal.epnova.feature.works

import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2026/2/3/周二  9:18
 * @Version: 1.0
 * *****************************************
 */
sealed class MyWorksEvent {
    //查看详情
    class ViewDetails(val workInfo: WorkInfo?):MyWorksEvent()
    //编制状态下 status是已经改变后的状态
    class ComposeState(status: Boolean,val workInfo: WorkInfo?):MyWorksEvent()
}

package com.kuaigeglobal.epnova.feature.mine.child

import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo


sealed class MyWorksVOEvent {
    //查看详情
    class ViewDetails(val workInfo: WorkVOInfo?) : MyWorksVOEvent()

    //编制状态下 status是已经改变后的状态
    class ComposeState(status: Boolean, val workInfo: WorkVOInfo?) : MyWorksVOEvent()
}

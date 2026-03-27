package com.kuaigeglobal.epnova.core.db.mapper

import com.kuaigeglobal.epnova.core.db.entity.WorkInfoEntity
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo

// 1. 将网络/业务对象转为数据库对象
fun WorkVOInfo.toEntity(pageIndex: Int): WorkInfoEntity {
    return WorkInfoEntity(
        workId = this.workId ?: "", // 确保主键不为空
        workTitle = this.workTitle,
        workDesc = this.workDesc,
        workRatio = this.workRatio,
        workCoverUrl = this.workCoverUrl,
        totalEpisodeCnt = this.totalEpisodeCnt,
        playCount = this.playCount,
        userId = this.userId,
        pageIndex = pageIndex
    )
}

// 2. 从数据库取出时转回业务对象 (保持 UI 层代码逻辑不变)
fun WorkInfoEntity.toDomain(): WorkVOInfo {
    return WorkVOInfo(
        workId = this.workId,
        workTitle = this.workTitle,
        workDesc = this.workDesc,
        workRatio = this.workRatio,
        workCoverUrl = this.workCoverUrl,
        totalEpisodeCnt = this.totalEpisodeCnt,
        playCount = this.playCount,
        userId = this.userId,
    )
}

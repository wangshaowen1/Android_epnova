package com.kuaigeglobal.epnova.core.db.mapper

import com.kuaigeglobal.epnova.core.db.entity.WorkInfoEntity
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo

// 1. 将网络/业务对象转为数据库对象
fun WorkInfo.toEntity(pageIndex: Int): WorkInfoEntity {
    return WorkInfoEntity(
        workId = this.workId ?: "", // 确保主键不为空
        workTitle = this.workTitle,
        workDesc = this.workDesc,
        workStyleCode = this.workStyleCode,
        workRatio = this.workRatio,
        workInspiration = this.workInspiration,
        workCoverUrl = this.workCoverUrl,
        workType = this.workType,
        workStatus = this.workStatus,
        totalEpisodeCnt = this.totalEpisodeCnt,
        finishedEpisodeCnt = this.finishedEpisodeCnt,
        lastEpisodeNo = this.lastEpisodeNo,
        lastGenerateTime = this.lastGenerateTime,
        playCount = this.playCount,
        userId = this.userId,
        workStoryItemList = this.workStoryItemList,
        pageIndex = pageIndex
    )
}

// 2. 从数据库取出时转回业务对象 (保持 UI 层代码逻辑不变)
fun WorkInfoEntity.toDomain(): WorkInfo {
    return WorkInfo(
        workId = this.workId,
        workTitle = this.workTitle,
        workDesc = this.workDesc,
        workStyleCode = this.workStyleCode,
        workRatio = this.workRatio,
        workInspiration = this.workInspiration,
        workCoverUrl = this.workCoverUrl,
        workType = this.workType,
        workStatus = this.workStatus,
        totalEpisodeCnt = this.totalEpisodeCnt,
        finishedEpisodeCnt = this.finishedEpisodeCnt,
        lastEpisodeNo = this.lastEpisodeNo,
        lastGenerateTime = this.lastGenerateTime,
        playCount = this.playCount,
        userId = this.userId,
        workStoryItemList = this.workStoryItemList
    )
}

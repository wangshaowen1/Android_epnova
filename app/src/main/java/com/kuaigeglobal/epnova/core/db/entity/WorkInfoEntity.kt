package com.kuaigeglobal.epnova.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.kuaigeglobal.epnova.core.db.convert.Converters
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo

/**
 * 首页 Feed 流数据库实体
 * 作用：用于本地持久化存储，支持 App 冷启动时秒开数据
 */
@Entity(tableName = "work_info")
@TypeConverters(Converters::class) // 复用原有的转换器处理 workStoryItemList
data class WorkInfoEntity(
    @PrimaryKey
    val workId: String, // 数据库主键必须非空

    val workTitle: String? = null,
    val workDesc: String? = null,
    val workStyleCode: String? = null,
    val workRatio: String? = null,
    val workInspiration: String? = null,
    val workCoverUrl: String? = null,
    val workType: Int? = null,
    val workStatus: Int? = null,
    val totalEpisodeCnt: Int? = null,
    val finishedEpisodeCnt: Int? = null,
    val lastEpisodeNo: Int? = null,
    val lastGenerateTime: String? = null,
    val playCount: Int? = null,
    var userId: String? = null,

    // 剧集列表：由 Converters 负责将其转为 String 存入 DB
    var workStoryItemList: List<EpisodeInfo>? = null,

    // --- 数据库管理专用字段 ---

    // 记录该数据属于第几页，方便 RemoteMediator 逻辑处理
    val pageIndex: Int = 0,

    // 记录入库时间，可用于后续缓存清理逻辑
    val createTime: Long = System.currentTimeMillis()
)

// 2. 存储分页信息的实体 (RemoteKey)
@Entity(tableName = "remote_keys")
data class RemoteKeys(
    @PrimaryKey val workId: String,
    val prevKey: Int?,
    val nextKey: Int?
)

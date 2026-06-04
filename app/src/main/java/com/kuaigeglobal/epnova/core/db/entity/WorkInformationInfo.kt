package com.kuaigeglobal.epnova.core.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import kotlinx.parcelize.Parcelize

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:作品信息
 * @Data: 2026/2/3/周二  9:40
 * @Version: 1.0
 * *****************************************
 */
@Parcelize
@Entity(tableName = "WorkInformationInfo")
data class WorkInformationInfo(
    @PrimaryKey
    @ColumnInfo(name = "primary_work_id")
    var workId: String = "",
    var sortIndex: Int = 0,
    // 创作体系作品本身的信息；MyCreations 列表和新创作播放器都从 Creator 模型读取。
    @Embedded
    val work: CreatorWorkInfo,
) : Parcelable

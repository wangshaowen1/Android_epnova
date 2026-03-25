package com.kuaigeglobal.epnova.core.db.entity

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
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
    @PrimaryKey(autoGenerate = true)
    var workInformationID: Long = 0,
    //作品本身的信息
    @Embedded
    val work: WorkInfo,
) : Parcelable
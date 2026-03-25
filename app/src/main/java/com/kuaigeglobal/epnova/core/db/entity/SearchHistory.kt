package com.kuaigeglobal.epnova.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "SearchHistory")
class SearchHistory(
    @PrimaryKey(autoGenerate = false)
    val content: String,
    val createTime: Long = System.currentTimeMillis()
)
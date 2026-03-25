package com.kuaigeglobal.epnova.core.db.dao

import androidx.room.*
import com.kuaigeglobal.epnova.core.db.entity.SearchHistory

@Dao
interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bean: SearchHistory)

    @Query("SELECT * FROM SearchHistory order by createTime desc limit 20")
    fun queryAll(): List<SearchHistory>?

    @Query("Delete from SearchHistory")
    fun clear()
}
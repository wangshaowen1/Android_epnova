package com.kuaigeglobal.epnova.core.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kuaigeglobal.epnova.core.db.entity.RemoteKeys
import com.kuaigeglobal.epnova.core.db.entity.WorkInfoEntity

// 3. DAO 接口
@Dao
interface WorkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<WorkInfoEntity>)

    @Query("SELECT * FROM work_info ORDER BY pageIndex ASC, createTime ASC")
    fun getPagingData(): PagingSource<Int, WorkInfoEntity>

    @Query("DELETE FROM work_info")
    suspend fun clearAll()
}

@Dao
interface RemoteKeysDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteKey: List<RemoteKeys>)

    @Query("SELECT * FROM remote_keys WHERE workId = :id")
    suspend fun getRemoteKeysId(id: String): RemoteKeys?

    @Query("DELETE FROM remote_keys")
    suspend fun clearRemoteKeys()
}
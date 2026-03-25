package com.kuaigeglobal.epnova.core.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kuaigeglobal.epnova.core.network.apis.entity.ShortDramaInfo

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:绘本相关
 * @Data: 2025/9/12/周五  10:59
 * @Version: 1.0
 * *****************************************
 */
@Dao
interface ShortDramaDao {
    // 用于Paging3从数据库分页查询
    @Query("SELECT * FROM ShortDramaInfo")
    fun getShorts(): PagingSource<Int, ShortDramaInfo>

    // 插入帖子列表 如果插入失败（例如由于约束冲突且未使用 REPLACE策略），返回值可能为 -1。通过判断返回值是否大于 0，可以确定插入操作是否成功
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shortDramaInfo: ShortDramaInfo)

    // 插入帖子列表
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shorts: List<ShortDramaInfo>)

    // 根据ID删除单个帖子 - 这就是实现"删除"功能的关键
    @Query("DELETE FROM ShortDramaInfo WHERE  dramasId = :shortDramaId")
    suspend fun deleteById( shortDramaId: String)

    // 清空表（用于刷新）
    @Query("DELETE FROM ShortDramaInfo")
    suspend fun clearAll()
 // 新增：更新单个条目（假设通过主键更新）
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(item: ShortDramaInfo): Int
}
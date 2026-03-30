package com.kuaigeglobal.epnova.core.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kuaigeglobal.epnova.core.db.entity.WorkInformationInfo
import com.kuaigeglobal.epnova.core.db.entity.WorkSortIndex

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:作品
 * @Data: 2026/2/3/周二  9:43
 * @Version: 1.0
 * *****************************************
 */
@Dao
interface WorkInformationDao {
    // 用于Paging3从数据库分页查询
    @Query("SELECT * FROM WorkInformationInfo  ORDER BY sortIndex ASC")
    fun getPosts(): PagingSource<Int, WorkInformationInfo>

    // 2. 批量查询：一次性查出所有命中 workIds 的原有排序号
    @Query("SELECT workId, sortIndex FROM WorkInformationInfo WHERE workId IN (:workIds)")
    suspend fun getSortIndexesByIds(workIds: List<String>): List<WorkSortIndex>

    // 清空表（用于刷新）
    @Query("DELETE FROM WorkInformationInfo")
    suspend fun clearAll()

    // 插入帖子列表 如果插入失败（例如由于约束冲突且未使用 REPLACE策略），返回值可能为 -1。通过判断返回值是否大于 0，可以确定插入操作是否成功
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(complaintWorkOrderID: WorkInformationInfo)

    // 插入帖子列表
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<WorkInformationInfo>)

    @Query("SELECT * FROM WorkInformationInfo ORDER BY workId ASC")
    suspend fun getAll(): List<WorkInformationInfo>


    /**
     * ✅ 新增：根据 workId 列表批量删除
     */
    @Query("DELETE FROM WorkInformationInfo WHERE workId IN (:workIds)")
    suspend fun deleteByWorkIds(workIds: List<String>): Int


    /**
     * 根据 workId，如果存在则更新，不存在则添加。
     * 整个操作在一个事务中完成，以保证数据一致性。
     * @param list 要处理的数据列表
     */
    @Transaction
    suspend fun upsertWorkInformationDao(list: List<WorkInformationInfo>) {
        for (item in list) {
            // 尝试根据 pictureBookId 和 type 查询是否已存在记录
            val existingItem: WorkInformationInfo? = getWorkInformationDao(item.work.workId)
            if (existingItem == null) {
                // 不存在，则插入新记录
                insert(item)
            } else {
                // 已存在，则更新记录
                // 注意：这里假设 update 方法是通过主键来更新的。
                // 如果你的 PictureBookRoomInfo 有使用 @PrimaryKey 标注的主键（例如一个自增id），并且希望更新所有字段，可以直接使用 @Update 方法。
                // 如果更新逻辑更复杂，可以使用 @Query 进行指定字段的更新。
                item.workId = existingItem.workId
                update(item)
            }
        }
    }

    // 新增：根据 type 和 pictureBookId 查询单个条目
    @Query("SELECT * FROM WorkInformationInfo WHERE  workId = :workId")
    suspend fun getWorkInformationDao(workId: String?): WorkInformationInfo?

    // 新增：更新单个条目（假设通过主键更新）
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(item: WorkInformationInfo): Int
}
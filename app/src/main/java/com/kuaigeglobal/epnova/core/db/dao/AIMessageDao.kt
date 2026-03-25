package com.kuaigeglobal.epnova.core.db.dao

import androidx.room.*
import com.kuaigeglobal.epnova.core.db.entity.AIMessage

@Dao
interface AIMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg bean: AIMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(bean: List<AIMessage>)

    @Query("SELECT * FROM AIMessage WHERE ownerId=:ownerId AND createTime<:createTime Order by createTime desc limit :limit")
    fun queryMessages(ownerId:String,limit:Int,createTime:Long):  List<AIMessage>?

    @Query("Delete from AIMessage  WHERE ownerId=:ownerId ")
    fun deleteMessages(ownerId: String)

    @Query("Delete from AIMessage")
    fun clear()
}
package com.kuaigeglobal.epnova.core.db.dao

import androidx.room.*
import com.kuaigeglobal.epnova.core.db.entity.ChatScene

@Dao
interface ChatSceneDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg bean: ChatScene)


    @Query("SELECT * FROM ChatScene WHERE sceneId=:sceneId   ")
    fun queryScene(sceneId: String):  ChatScene?

    @Query("Delete from ChatScene  WHERE sceneId=:sceneId ")
    fun deleteScene(sceneId: String)

    @Query("Delete from ChatScene")
    fun clear()
}
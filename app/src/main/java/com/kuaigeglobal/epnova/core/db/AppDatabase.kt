package com.kuaigeglobal.epnova.core.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kuaigeglobal.epnova.core.db.dao.AIMessageDao
import com.kuaigeglobal.epnova.core.db.dao.ChatSceneDao
import com.kuaigeglobal.epnova.core.db.dao.RemoteKeysDao
import com.kuaigeglobal.epnova.core.db.dao.SearchHistoryDao
import com.kuaigeglobal.epnova.core.db.dao.ShortDramaDao
import com.kuaigeglobal.epnova.core.db.dao.WorkDao
import com.kuaigeglobal.epnova.core.db.dao.WorkInformationDao
import com.kuaigeglobal.epnova.core.db.entity.AIMessage
import com.kuaigeglobal.epnova.core.db.entity.ChatScene
import com.kuaigeglobal.epnova.core.db.entity.RemoteKeys
import com.kuaigeglobal.epnova.core.db.entity.SearchHistory
import com.kuaigeglobal.epnova.core.db.entity.WorkInfoEntity
import com.kuaigeglobal.epnova.core.db.entity.WorkInformationInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.ShortDramaInfo
import com.nhsd.basic.BaseApp

@Database(
    entities = [
        AIMessage::class,
        ChatScene::class,
        SearchHistory::class,
        ShortDramaInfo::class,
        WorkInformationInfo::class,
        WorkInfoEntity::class,
        RemoteKeys::class
    ], version = 4, exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract val aiMessageDao: AIMessageDao
    abstract val chatSceneDao: ChatSceneDao
    abstract val searchHistoryDao: SearchHistoryDao
    abstract val shortDramsDao: ShortDramaDao

    abstract fun workInformationDao(): WorkInformationDao
    abstract val workDao : WorkDao
    abstract val remoteKeysDao: RemoteKeysDao


    companion object {

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance() = instance ?: synchronized(AppDatabase::class.java) {
            instance ?: buildDatabase().also { instance = it }
        }

        private fun buildDatabase(): AppDatabase = Room.databaseBuilder(
            BaseApp.getInstance(),
            AppDatabase::class.java,
            "AiChatDatabase.db"
        )
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration(false)//数据库更新出错即回滚 会丢失数据
            .addMigrations()//更新数据库的Migrations对象
            .build()
    }
}

package com.kuaigeglobal.epnova.core

import com.nhsd.basic.BaseRepository
import com.kuaigeglobal.epnova.core.db.AppDatabase
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.ApiService

open class AppRepository() : BaseRepository<ApiService, AppDatabase>() {
    override val api = ApiFactory.apiService
    override val appDatabase = AppDatabase.Companion.getInstance()
}
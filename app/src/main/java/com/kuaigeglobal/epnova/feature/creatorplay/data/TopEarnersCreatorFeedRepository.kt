package com.kuaigeglobal.epnova.feature.creatorplay.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kuaigeglobal.epnova.common.Constant
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import kotlinx.coroutines.flow.Flow

/**
 * TopEarners 推荐作品 work feed 的共享入口。
 *
 * 列表和后续新播放器必须从这里拿同一个 cached work feed；
 * 播放器需要 CreatorPlayableEpisode 时再从 work feed 派生，不重新请求接口。
 */
object TopEarnersCreatorFeedRepository {

    /**TopEarners 列表和新播放器共享的作品分页流；置空后下一次访问会重新创建 paging source。*/
    private var workFeedFlow: Flow<PagingData<CreatorWorkInfo>>? = null

    /**返回共享 work feed。TopEarners 列表和播放器应复用同一个 Flow。*/
    fun getWorkFeed(): Flow<PagingData<CreatorWorkInfo>> {
        return workFeedFlow ?: createWorkFeed().also { workFeedFlow = it }
    }

    /**清空共享 feed，后续需要强制重建数据源时使用。*/
    fun reset() {
        workFeedFlow = null
    }

    /**创建 TopEarners work feed；paging source 只产出 CreatorWorkInfo，不提前产出播放单元。*/
    private fun createWorkFeed(): Flow<PagingData<CreatorWorkInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = {
                TopEarnersCreatorPagingSource()
            }
        ).flow.cachedIn(ioScope)
    }
}

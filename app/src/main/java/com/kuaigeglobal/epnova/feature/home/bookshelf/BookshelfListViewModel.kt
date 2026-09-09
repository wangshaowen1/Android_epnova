package com.kuaigeglobal.epnova.feature.home.bookshelf

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.common.Constant
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.getResString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest

class BookshelfListViewModel : AppRepositoryViewModel<AppRepository>() {

    private data class Query(
        val bookshelfId: String?,
        val bookshelfName: String?
    )

    val toolbar = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.home_search_tab_new_release.getResString()
    )
    private val query = MutableStateFlow<Query?>(null)

    val works: Flow<PagingData<WorkVOInfo>> = query
        .flatMapLatest { query ->
            if (query == null) {
                kotlinx.coroutines.flow.flowOf(PagingData.empty())
            } else {
                Pager(
                    config = PagingConfig(
                        initialLoadSize = Constant.Paged.PAGE_SIZE,
                        pageSize = Constant.Paged.PAGE_SIZE,
                        prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
                        enablePlaceholders = false
                    ),
                    pagingSourceFactory = {
                        BookshelfPagingSource(mRepo.api, query.bookshelfId, query.bookshelfName)
                    }
                ).flow
            }
        }
        .cachedIn(viewModelScope)

    fun load(bookshelfId: String?, bookshelfName: String?) {
        query.value = Query(bookshelfId, bookshelfName)
    }
}

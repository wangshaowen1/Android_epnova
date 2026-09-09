package com.kuaigeglobal.epnova.feature.message

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.common.Constant
import com.nhsd.basic.ext.getResString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest

class NotificationsViewModel : AppViewModel() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.messages_title.getResString()
    )

    private val notificationItems = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: Flow<PagingData<NotificationItem>> = notificationItems
        .flatMapLatest { items ->
            Pager(
                config = PagingConfig(
                    initialLoadSize = Constant.Paged.PAGE_SIZE,
                    pageSize = Constant.Paged.PAGE_SIZE,
                    prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
                ),
                pagingSourceFactory = { NotificationsPagingSource(items) }
            ).flow
        }
        .cachedIn(viewModelScope)
}

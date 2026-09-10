package com.kuaigeglobal.epnova.feature.message

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.common.Constant
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.request
import com.kuaigeglobal.epnova.core.network.apis.entity.MessagePageInfo
import com.nhsd.basic.ext.getResString
import kotlinx.coroutines.flow.Flow

class NotificationsViewModel : AppRepositoryViewModel<AppRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.messages_title.getResString()
    )

    val notifications: Flow<PagingData<MessagePageInfo>> = Pager(
        config = PagingConfig(
            initialLoadSize = Constant.Paged.PAGE_SIZE,
            pageSize = Constant.Paged.PAGE_SIZE,
            prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
        ),
        pagingSourceFactory = { NotificationsPagingSource(mRepo.api) }
    ).flow.cachedIn(viewModelScope)

    /** 进入消息页时将服务端消息统一标记为已读。 */
    fun readAllMessages() {
        request(
            apiBlock = { mRepo.api.readAllMessages() },
            showToast = false,
            onSuccess = {}
        )
    }
}

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
import com.kuaigeglobal.epnova.core.network.apis.request.MessageReadAllBody
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
        pagingSourceFactory = {
            NotificationsPagingSource(
                api = mRepo.api,
                onReadSnapshotToken = ::readAllMessages
            )
        }
    ).flow.cachedIn(viewModelScope)

    /** 首次加载或刷新成功后，按返回的有效快照标记已读。 */
    private fun readAllMessages(readSnapshotToken: String?) {
        val token = readSnapshotToken?.takeIf(String::isNotBlank) ?: return
        request(
            apiBlock = { mRepo.api.readAllMessages(MessageReadAllBody(token)) },
            showToast = false,
            onSuccess = {}
        )
    }
}

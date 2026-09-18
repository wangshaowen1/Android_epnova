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

/** 消息中心状态持有者，创建分页流并消费服务端返回的已读快照标记。 */
class NotificationsViewModel : AppRepositoryViewModel<AppRepository>() {

    /** 页面标题和返回行为交给公共 ToolbarViewModel 处理。 */
    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.messages_title.getResString()
    )

    /** 消息分页流；缓存绑定在当前 ViewModel 生命周期内，刷新时由 Paging 重建数据源。 */
    val notifications: Flow<PagingData<MessagePageInfo>> = Pager(
        config = PagingConfig(
            initialLoadSize = Constant.Paged.PAGE_SIZE,
            pageSize = Constant.Paged.PAGE_SIZE,
            prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
        ),
        pagingSourceFactory = {
            // 每个 PagingSource 都持有本次分页的快照回调，刷新成功后再触发全部已读。
            NotificationsPagingSource(
                api = mRepo.api,
                onReadSnapshotToken = ::readAllMessages
            )
        }
    ).flow.cachedIn(viewModelScope)

    /** 首次加载或刷新成功后，按接口返回的有效快照标记当时的消息为已读。 */
    private fun readAllMessages(readSnapshotToken: String?) {
        val token = readSnapshotToken?.takeIf(String::isNotBlank) ?: return
        request(
            apiBlock = { mRepo.api.readAllMessages(MessageReadAllBody(token)) },
            showToast = false,
            onSuccess = {}
        )
    }
}

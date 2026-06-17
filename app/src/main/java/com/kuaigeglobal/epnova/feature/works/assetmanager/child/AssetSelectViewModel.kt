package com.kuaigeglobal.epnova.feature.works.assetmanager.child

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.blankj.utilcode.util.StringUtils.getString
import com.kuaigeglobal.epnova.feature.works.data.AssetChildPagingSource
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.common.Constant
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleAsset
import com.kuaigeglobal.epnova.core.network.apis.request.QueryAssetBody
import kotlinx.coroutines.flow.Flow

class AssetSelectViewModel : AppRepositoryViewModel<AppRepository>() {

    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = ""
    )

    val bizType by lazy { mBundle.getInt(Constants.Extra.TYPE) }
    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    val storyId by lazy { mBundle.getString(Constants.Extra.STORY_ID) }
    val assetId by lazy { mBundle.getString(Constants.Extra.ASSET_ID) }
    var selectedItem: SimpleAsset? = null

    val isDataEmpty = ObservableBoolean(true)

    var totalCountBack: (Int) -> Unit = {}

    override fun onCreate() {
        super.onCreate()
        when (bizType) {
            QueryAssetBody.BIZ_TYPE_PROP -> mToolbarVM.mTitle.set(getString(R.string.select_prop))
            QueryAssetBody.BIZ_TYPE_SCENE -> mToolbarVM.mTitle.set(getString(R.string.select_scene))
        }
    }

    fun getAssetsPage(): Flow<PagingData<SimpleAsset>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = {
                AssetChildPagingSource(bizType, workId, storyId, assetId) {
                    totalCountBack.invoke(it)
                    isDataEmpty.set(it == 0)
                }
            }).flow.cachedIn(viewModelScope)
    }
}

package com.kuaigeglobal.epnova.feature.home.search

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.LoadState
import com.kuaigeglobal.epnova.common.adapter.QuickAdapter
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.parseMessage
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.request.KeyworkBody
import com.kuaigeglobal.epnova.feature.play.DramasPlayPagerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class SearchViewModel : AppRepositoryViewModel<AppRepository>() {

    val loadState = ObservableField<LoadState>(LoadState.NotLoading(false))
    val resultAdapter = QuickAdapter<ItemSearchResultVm>(R.layout.item_search_result, emptyList())
    val keyWord = MutableLiveData<String>()

    fun search() {
        loadState.set(LoadState.Loading)
        launch({
            val result = withContext(Dispatchers.IO) { mRepo.api.homeSearch(KeyworkBody(keyWord.value)) }
            if (result.isSuccess) {
                val keyword = keyWord.value
                resultAdapter.submitList(result.data?.map {
                    ItemSearchResultVm(it).apply {
                        keyWord = keyword
                        onItemClick.value = {
                            DramasPlayPagerActivity.start(
                                workId = data.get()?.workId,
                                position = 0,
                                isSeriesMode = false,
                                source = FirebaseEvent.Value.SOURCE_SEARCH_PAGE
                            )
                        }
                    }
                })
                val empty = result.data.isNullOrEmpty()
                loadState.set(if (empty) LoadState.Empty else LoadState.NotLoading(true))
            } else {
                loadState.set(LoadState.Error(Exception(result.msg ?: ""), false))
            }
        }, onError = {
            loadState.set(LoadState.Error(Exception(parseMessage(it)), false))
        })
    }

    fun clearKeyword() {
        keyWord.value = ""
    }
}

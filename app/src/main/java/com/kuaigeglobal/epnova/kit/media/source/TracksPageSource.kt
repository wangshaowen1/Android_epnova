package com.kuaigeglobal.epnova.kit.media.source

import com.kuaigeglobal.epnova.core.network.apis.ApiResponse
import com.kuaigeglobal.epnova.core.network.apis.PagedList
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo

/**
 * Interface defining the contract for a paginated data source of tracks.
 * Implementations of this interface must provide a method to load the next page of track information.
 */
interface TracksPageSource {

    /**
     * Loads the next page of track information.
     *
     * @param page The page number to load. Page numbers start at 1.
     * @param pageSize The number of items to load per page.
     * @return An [ApiResponse] containing a [PagedList] of [WorkInfo] or null if the data is not available.
     *         The [ApiResponse] will also indicate whether the request was successful or if an error occurred.
     */
    suspend fun nextPage(page: Int, pageSize: Int): ApiResponse<PagedList<WorkInfo?>?>

    val sourceId: String get() = this.javaClass.simpleName

    fun makeSourceId(vararg id: String?): String {
        return "${this.javaClass.simpleName}_${id.joinToString("_")}"
    }
}

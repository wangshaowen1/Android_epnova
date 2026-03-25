package com.kuaigeglobal.epnova.core.network.apis

import com.kuaigeglobal.epnova.core.network.apis.request.EventTrackingBody
import retrofit2.http.Body
import retrofit2.http.POST

interface EventService {

    /**
     * 创建数据埋点
     */
    @POST(ApiConstants.USER_REVENUE_INFORMATION)
    suspend fun eventTracking(@Body body: EventTrackingBody): ApiResponse<Any?>

}
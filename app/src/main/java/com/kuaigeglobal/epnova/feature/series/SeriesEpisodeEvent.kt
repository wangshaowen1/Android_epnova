package com.kuaigeglobal.epnova.feature.series

sealed class SeriesEpisodeEvent {
    class Click(val position: Int, val item: ItemSeriesEpisodeVM) : SeriesEpisodeEvent()

    class Download(val item: ItemSeriesEpisodeVM) : SeriesEpisodeEvent()

    //编制状态下 status是已经改变后的状态
    class ComposeState(val status: Boolean,val item: ItemSeriesEpisodeVM?):SeriesEpisodeEvent()
}
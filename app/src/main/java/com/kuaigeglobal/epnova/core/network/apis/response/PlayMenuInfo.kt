package com.kuaigeglobal.epnova.core.network.apis.response

import android.os.Parcelable
import com.kuaigeglobal.epnova.feature.play.menu.SelectableItem
import kotlinx.parcelize.Parcelize

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:播放页右上角的菜单信息
 * @Data: 2026/2/5/周四  16:44
 * @Version: 1.0
 * *****************************************
 */
@Parcelize
data class PlayMenuInfo(
    //菜单类型
    val reportTypeId: String? = null,
    //菜单文本
    val reportText: String? = null,
    //菜单图标
    val reportIcon: Int? = null,
) : Parcelable, SelectableItem {

    companion object {
        //举报
        const val REPORT = "1"
        //下载
        const val DOWNLOAD = "2"
    }

    override fun getImageResId(): Int? =reportIcon?:0

    override fun getDisplayText(): String = reportText ?: ""
    override fun getItemId(): String = reportTypeId ?: ""
}
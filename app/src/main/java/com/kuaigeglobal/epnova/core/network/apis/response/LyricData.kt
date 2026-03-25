package com.kuaigeglobal.epnova.core.network.apis.response

data class LyricData(
    val musicTitle:String?,
    val musicLyric: String?,
    val status: Int?,//状态 0-生成中，1-成功，2-失败
){
    fun isSuccess():Boolean{

        return status==1
    }

    fun isFailed():Boolean{
        return status==2
    }
}
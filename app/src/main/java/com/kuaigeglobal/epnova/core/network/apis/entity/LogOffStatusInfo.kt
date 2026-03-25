package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * @param removeAccountState 注销状态，0未申请，1提交申请
 */
data class LogOffStatusInfo(val removeAccountState: Int?){
    companion object{
        //未申请
        const val NOT_APPLY = 0
        //提交申请
        const val SUBMIT_APPLY = 1
    }
}
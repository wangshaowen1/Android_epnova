package com.kuaigeglobal.epnova.core.network.apis.response

import com.kuaigeglobal.epnova.core.User

data class SceneCostData(
    val feeNum: Int?,//扣费数量(次/个)
    val feeType: Int?,//扣费类型:1=免费 2=积分 3=能力包（对终端来说看作是类型1免费就行）
    val remainingAttempts: Int?,//剩余次数
    var remainingPoints: Int?,//剩余积分
    val sceneType: Int?,//场景类型:1-生成绘本 2-音色克隆 3-生成配音 4-绘本重绘
    val userStory: String?//新用户免费，有值就显示这个，没有正常显示
) {

    /**
     * 余额是否充足
     */
    fun isBalanceEnough(): Boolean {
        return true//if (feeType == 2) User.get().scoreBalance >= (feeNum ?: 0) else (remainingAttempts ?: 0) >= 0
    }


    /**
     * 新用户免费
     */
    fun isSignExist(): Boolean {
        return !userStory.isNullOrEmpty()
    }

    fun isFree(): Boolean {
        return (feeType == 1 || feeType == 3) && (remainingAttempts ?: 0) > 0
    }
}
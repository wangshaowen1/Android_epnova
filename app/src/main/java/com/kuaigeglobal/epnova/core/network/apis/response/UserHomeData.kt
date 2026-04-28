package com.kuaigeglobal.epnova.core.network.apis.response


import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 获取用户主页信息接口
 */
@Parcelize
data class UserHomeData(
    //用户ID
    val userId: String? = null,
    //仅展示用
    val userUid: String? = null,
    //手机号
    val phoneNum: String? = null,
    //用户名
    val userName: String? = null,
    //用户昵称
    val nickName: String? = null,
    //性别 0-未知 1-男 2-女
    val gender: Int? = null,
    //用户头像URL
    val avatarUrl: String? = null,
    //用户来源 1-用户自己注册 6=游客
    val userSource: Int? = null,
    //注册方式 1-短信验证码 2-闪验 3-微信登录 4-qq登录 5-苹果登录 6-微信小程序 7-账号密码
    val registerWay: Int? = null,
    /**
     * 购买状态
     * 0-未购买  1-已购买
     */
    val purchaseCourseStatus: Int? = null,
    //最后登录时间
    val lastLoginTime: String? = null,
    //是否是注册，0-登录，1-是注册
    val registerStatus: Int? = null,
    /**
     * 1-新客（未购买过用户）2-老客（已购买过用户）
     */
    val userTag: Int? = null,
    /**
     * 260416新增是否触发新手礼包弹窗状态 0-否，1-是1-是 触发新手礼包弹窗 /gift/newUser/popup/info
     */
    val giftNewUserTriggerFlag: Int? = null,
    /**
     * 260416新增登录奖励状态 0-无需弹窗 1=可以弹窗 2=登录成功未领取奖励 3=登录已领取奖励1-可以弹窗（弹窗接口/reward/onceTask/popup/info）2-已登录未领取奖励（触发奖励接口/reward/onceTask/record/save） 3-已登录已领取奖励
     */
    val rewardLoginStatus: Int? = null,
    //访问令牌
    val token: String? = null,
) : Parcelable {

    //新注册
    fun isNewRegistration(): Boolean {
        return registerStatus == 1
    }
}
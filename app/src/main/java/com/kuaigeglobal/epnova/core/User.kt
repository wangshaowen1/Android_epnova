package com.kuaigeglobal.epnova.core

import android.text.TextUtils
import com.google.gson.Gson
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.common.SP
import com.kuaigeglobal.epnova.core.network.apis.entity.CookieUserInfo
import com.kuaigeglobal.epnova.core.network.apis.request.CompleteUserDataInfoBody
import com.kuaigeglobal.epnova.core.network.apis.response.UserHomeData
import com.nhsd.basic.Session
import com.nhsd.basic.bus.FlowEventBus

/**
 * 当前登录角色信息
 */
class User : IUser {
    val sp: SP

    //用户ID
    var userId: String?

    //手机号
    var phoneNum: String?

    //uid
    var userUid: String?

    //用户昵称
    var nickName: String?

    //性别 0-未知 1-男 2-女
    var gender: Int?

    //用户头像URL
    var avatarUrl: String?

    //用户来源 1-用户自己注册 2-平台直接生成 3-平台通过app注册的假用户 4-平台测试用户 5-小程序注册
    var userSource: Int?
        private set

    //注册方式 1-短信验证码 2-闪验 3-微信登录 4-qq登录 5-苹果登录 6-微信小程序 7-账号密码
    var registerWay: Int?
        private set

    //最后登录时间
    var lastLoginTime: String?
        private set

    //是否是注册，0-登录，1-是注册
    var registerStatus: Int?
        private set

    //访问令牌
    var token: String?
        private set

    /**算力余额*/
    var computingPower: Int
        private set

    /**课程购买状态*/
    var purchaseCourseStatus: Int?
        private set

    /**
     * 1-新客（未购买过用户）2-老客（已购买过用户）
     */
    var isPaid: Int?
        private set

    var requestNotificationsPermissionEnable = true

    val userInfoStr: String?
        get() {
            val userBasicInfo = CookieUserInfo(id, Session.getInstance().versionName)
            return Gson().toJson(userBasicInfo)
        }

    val isLogged: Boolean
        /**
         * 检查登录状态
         *
         * @return true:已登录 false:未登录
         */
        get() = !TextUtils.isEmpty(id)

    /**
     * google推送的token
     */
    var fcmToken: String?
        private set


    init {
        sp = SP("USER_DATA")
        userId = sp.getString(USER_ID, null)
        phoneNum = sp.getString(PHONE_NUM, null)
        userUid = sp.getString(U_ID, null)
        nickName = sp.getString(NICK_NAME, null)
        gender = sp.getInt(GENDER, 0)
        avatarUrl = sp.getString(AVATAR_URL, null)
        userSource = sp.getInt(USER_SOURCE, 0)
        registerWay = sp.getInt(REGISTER_WAY, 0)
        lastLoginTime = sp.getString(LAST_LOGIN_TIME, null)
        registerStatus = sp.getInt(REGISTER_STATUS, 0)
        token = sp.getString(TOKEN, null)
        computingPower = sp.getInt(COMPUTING_POWER, 0)
        purchaseCourseStatus = sp.getInt(PURCHASE_COURSE_STATUS, 0)
        isPaid = sp.getInt(IS_PAID, 0)
        fcmToken = sp.getString(FCM_TOKEN, "")
        requestNotificationsPermissionEnable = sp.getBoolean(REQUEST_NOTIFICATIONS_PERMISSION, true)
    }

    fun updateFrom(userData: UserHomeData?) {
        if (userData == null) return
        userData.userId?.apply {
            storeUserId(this)
        }
        storePhoneNum(userData.phoneNum)
        storeUid(userData.userUid)
        storeNickname(userData.nickName)
        storeNickname(userData.gender)
        storeAvatar(userData.avatarUrl)
        storeUserSource(userData.userSource)
        storeRegisterWay(userData.registerWay)
        storeLastLoginTime(userData.lastLoginTime)
        storePurchaseCourseStatus(userData.purchaseCourseStatus)
        storePaid(userData.userTag)
        userData.registerStatus?.apply {
            storeRegisterStatus(this)
        }
        userData.token?.apply {
            storeToken(this)
        }
        notifyChange()
    }


    fun updateFromCompleteUserBody(body: CompleteUserDataInfoBody) {
        body.avatarUrl?.let { storeAvatar(it) }
        body.nickName?.let {
            storeNickname(it)
        }
        body.gender?.let { storeNickname(it) }
        notifyChange()
    }


    private fun clear() {
        userId = null
        sp.remove(USER_ID)
        phoneNum = null
        sp.remove(PHONE_NUM)
        nickName = null
        sp.remove(NICK_NAME)
        gender = null
        sp.remove(GENDER)
        avatarUrl = null
        sp.remove(AVATAR_URL)
        userSource = null
        sp.remove(USER_SOURCE)
        registerWay = null
        sp.remove(REGISTER_WAY)
        lastLoginTime = null
        sp.remove(LAST_LOGIN_TIME)
        registerStatus = null
        sp.remove(REGISTER_STATUS)
        purchaseCourseStatus = 0
        sp.remove(PURCHASE_COURSE_STATUS)
        isPaid = null
        sp.remove(IS_PAID)
        token = null
        sp.remove(TOKEN)
        computingPower = 0
        sp.remove(COMPUTING_POWER)
    }


    fun notifyChange(vararg keys: String?) {
        FlowEventBus.postSync(Events.UserDataEvent(*keys))
    }


    fun loginOut() {
        clear()
        notifyChange()
    }

    val isVisitor: Boolean
        /**
         * 检查登录状态
         *
         * @return true:未登录 false:已登录
         */
        get() = TextUtils.isEmpty(id)


    override fun getId(): String? {
        return token
    }

    /**
     * 用户ID
     * @param userId 用户ID
     */
    fun storeUserId(userId: String?) {
        this.userId = userId
        sp.putString(USER_ID, userId)
    }

    /**
     * 手机号
     * @param phoneNum 手机号
     */
    fun storePhoneNum(phoneNum: String?) {
        this.phoneNum = phoneNum
        sp.putString(PHONE_NUM, phoneNum)
    }

    /**
     * 用户名
     * @param userName 用户名
     */
    fun storeUid(uid: String?) {
        this.userUid = uid
        sp.putString(U_ID, uid)
    }

    fun storeFcmToken(fcmToken: String?) {
        sp.putString(FCM_TOKEN, fcmToken)
        this.fcmToken = fcmToken
    }


    /**
     * 用户昵称
     * @param nickName 用户昵称
     */
    fun storeNickname(nickName: String?) {
        this.nickName = nickName
        sp.putString(NICK_NAME, nickName)
    }

    /**
     * 性别
     * @param gender  0-未知 1-男 2-女
     */
    fun storeNickname(gender: Int?) {
        this.gender = gender ?: 0
        sp.putInt(GENDER, gender ?: 0)
    }


    /**
     * 存储头像
     * @param avatar 头像地址
     */
    fun storeAvatar(avatar: String?) {
        this.avatarUrl = avatar
        sp.putString(AVATAR_URL, avatar)
    }

    /**
     * 用户来源
     * @param userSource 1-用户自己注册 2-平台直接生成 3-平台通过app注册的假用户 4-平台测试用户 5-小程序注册
     */
    fun storeUserSource(userSource: Int?) {
        this.userSource = userSource ?: 0
        sp.putInt(USER_SOURCE, userSource ?: 0)
    }

    /**
     * 注册方式
     * @param registerWay 1-短信验证码 2-闪验 3-微信登录 4-qq登录 5-苹果登录 6-微信小程序 7-账号密码
     */
    fun storeRegisterWay(registerWay: Int?) {
        this.registerWay = registerWay ?: 0
        sp.putInt(REGISTER_WAY, registerWay ?: 0)
    }

    /**
     * 最后登录时间
     * @param lastLoginTime 最后登录时间
     */
    fun storeLastLoginTime(lastLoginTime: String?) {
        this.lastLoginTime = lastLoginTime
        sp.putString(LAST_LOGIN_TIME, lastLoginTime)
    }

    /**
     * 是否是注册，0-登录，1-是注册
     * @param registerStatus 0-登录，1-是注册
     */
    fun storeRegisterStatus(registerStatus: Int?) {
        this.registerStatus = registerStatus ?: 0
        sp.putInt(REGISTER_STATUS, registerStatus ?: 0)
    }

    /**
     * 访问令牌
     * @param token 访问令牌
     */
    fun storeToken(token: String?) {
        this.token = token
        sp.putString(TOKEN, token)
    }

    /**
     * 算力余额
     */
    fun storeComputingPower(computingPower: Int) {
        this.computingPower = computingPower
        sp.putInt(COMPUTING_POWER, this.computingPower)
        notifyChange(COMPUTING_POWER)
    }

    fun storePurchaseCourseStatus(purchaseCourseStatus: Int?) {
        this.purchaseCourseStatus = purchaseCourseStatus
        sp.putInt(PURCHASE_COURSE_STATUS, this.purchaseCourseStatus ?: 0)
    }

    fun hasPaid(): Boolean {
        return isPaid == 2
    }

    fun storePaid(isPaid: Int?) {
        this.isPaid = isPaid
        sp.putInt(IS_PAID, this.isPaid ?: 0)
    }

    fun isFemale(): Boolean {
        return gender == FEMALE
    }

    fun isSameId(userId: String?): Boolean {
        if (this.isVisitor) {
            return false
        }
        return TextUtils.equals(userId, this.userId)
    }

    fun isPurchasedCourse(): Boolean {
        return purchaseCourseStatus == 1
    }

    fun setRequestNotificationsPermissionDisable() {
        requestNotificationsPermissionEnable = false
        sp.putBoolean(REQUEST_NOTIFICATIONS_PERMISSION, false)
    }

    companion object {
        private const val FCM_TOKEN = "FCM_TOKEN"
        //用户ID
        val USER_ID = "userId"

        //手机号
        const val PHONE_NUM = "phoneNum"

        //用户名
        const val U_ID = "uid"

        //用户昵称
        const val NICK_NAME = "nickName"

        //性别 0-未知 1-男 2-女
        const val GENDER = "gender"

        //用户头像URL
        const val AVATAR_URL = "avatarUrl"

        //用户来源 1-用户自己注册 2-平台直接生成 3-平台通过app注册的假用户 4-平台测试用户 5-小程序注册
        const val USER_SOURCE = "userSource"

        //注册方式 1-短信验证码 2-闪验 3-微信登录 4-qq登录 5-苹果登录 6-微信小程序 7-账号密码
        const val REGISTER_WAY = "registerWay"

        //最后登录时间
        const val LAST_LOGIN_TIME = "lastLoginTime"

        //是否是注册，0-登录，1-是注册
        const val REGISTER_STATUS = "registerStatus"

        //访问令牌
        const val TOKEN = "token"

        const val COMPUTING_POWER = "computingPower"

        const val PURCHASE_COURSE_STATUS = "purchaseCourseStatus"

        const val IS_PAID = "IS_PAID"

        //性别-男
        const val MALE = 1

        //性别-女
        const val FEMALE = 2

        private const val REQUEST_NOTIFICATIONS_PERMISSION: String =
            "REQUEST_NOTIFICATIONS_PERMISSION"

        @JvmStatic
        fun get(): User {
            return App.instance.user
        }
    }
}
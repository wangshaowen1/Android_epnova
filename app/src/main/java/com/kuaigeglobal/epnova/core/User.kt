package com.kuaigeglobal.epnova.core

import android.content.Context
import android.text.TextUtils
import com.google.gson.Gson
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.common.utils.MmkvDelegate
import com.kuaigeglobal.epnova.core.network.apis.entity.CookieUserInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WalletInfo
import com.kuaigeglobal.epnova.core.network.apis.request.CompleteUserDataInfoBody
import com.kuaigeglobal.epnova.core.network.apis.response.UserHomeData
import com.nhsd.basic.Session
import com.nhsd.basic.bus.FlowEventBus
import com.tencent.mmkv.MMKV

/**
 * 当前登录角色信息 (委托属性极简版)
 */
class User : IUser {

    // 初始化 MMKV
    private val mmkv: MMKV by lazy { MMKV.mmkvWithID(PREF_ID, MMKV.MULTI_PROCESS_MODE) }

    // --- 使用 MmkvDelegate 代理所有属性 ---

    var id: String? by MmkvDelegate(ID, null) { mmkv }

    /**
     * 0-无需弹窗 1=可以弹窗 2=登录成功未领取奖励 3=登录已领取奖励
     */
    var loginRewardsValid: Int? by MmkvDelegate(LOGIN_REWARDS_VALID, 0) { mmkv }
    var firstRechargeDialog: Boolean? by MmkvDelegate("firstRechargeDialog", false) { mmkv }

    var userId: String? by MmkvDelegate(USER_ID, null) { mmkv }
    var phoneNum: String? by MmkvDelegate(PHONE_NUM, null) { mmkv }
    var userUid: String? by MmkvDelegate(U_ID, null) { mmkv }
    var nickName: String? by MmkvDelegate(NICK_NAME, null) { mmkv }
    var gender: Int? by MmkvDelegate(GENDER, 0) { mmkv }
    var avatarUrl: String? by MmkvDelegate(AVATAR_URL, null) { mmkv }

    var userSource: Int? by MmkvDelegate(USER_SOURCE, 0) { mmkv }
        private set
    var registerWay: Int? by MmkvDelegate(REGISTER_WAY, 0) { mmkv }
        private set
    var lastLoginTime: String? by MmkvDelegate(LAST_LOGIN_TIME, null) { mmkv }
        private set
    var registerStatus: Int? by MmkvDelegate(REGISTER_STATUS, 0) { mmkv }
        private set
    var coins: Long by MmkvDelegate(COINS, 0) { mmkv }
        private set
    var giveCoins: Long by MmkvDelegate(GIVE_COINS, 0) { mmkv }
        private set
    var purchaseCourseStatus: Int? by MmkvDelegate(PURCHASE_COURSE_STATUS, 0) { mmkv }
        private set
    var isPaid: Int? by MmkvDelegate(IS_PAID, 0) { mmkv }
        private set
    var fcmToken: String? by MmkvDelegate(FCM_TOKEN, "") { mmkv }
        private set
    var requestNotificationsPermissionEnable: Boolean by MmkvDelegate(REQUEST_NOTIFICATIONS_PERMISSION, true) { mmkv }


    val userInfoStr: String?
        get() {
            val userBasicInfo = CookieUserInfo(id, Session.getInstance().versionName, Config.get().getSavedLanguage())
            return Gson().toJson(userBasicInfo)
        }

    val isLogged: Boolean
        get() = !TextUtils.isEmpty(id)

    val isVisitor: Boolean
        get() = TextUtils.isEmpty(id)

    val isGuest: Boolean
        get() = !TextUtils.isEmpty(id) && userSource == 6

    val isLogin: Boolean
        get() = !TextUtils.isEmpty(id) && userSource == 1

    /**
     * 初始化方法：负责将旧的 SP 数据迁移到 MMKV
     */
    fun init(context: Context) {
        val oldSp = context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)

        // 如果 MMKV 中没有 ID，且旧 SP 中仍有数据，执行迁移
        if (!mmkv.containsKey(ID) && oldSp.all.isNotEmpty()) {
            val allMap = oldSp.all
            val editor = oldSp.edit()

            for ((key, value) in allMap) {
                when (value) {
                    is String -> mmkv.encode(key, value)
                    is Int -> mmkv.encode(key, value)
                    is Long -> mmkv.encode(key, value)
                    is Float -> mmkv.encode(key, value)
                    is Boolean -> mmkv.encode(key, value)
                }
            }
            // 迁移完成后清空旧 SP，节约空间并防止二次误操作
            editor.clear().apply()
        }
    }

    fun updateFrom(userData: UserHomeData?) {
        if (userData == null) return
        userData.userId?.apply { storeUserId(this) }
        storePhoneNum(userData.phoneNum)
        storeUid(userData.userUid)
        storeNickname(userData.nickName)
        storeGender(userData.gender)
        storeAvatar(userData.avatarUrl)
        storeUserSource(userData.userSource)
        storeRegisterWay(userData.registerWay)
        storeLastLoginTime(userData.lastLoginTime)
        storePurchaseCourseStatus(userData.purchaseCourseStatus)
        storePaid(userData.userTag)
        userData.registerStatus?.apply { storeRegisterStatus(this) }
        userData.token?.apply { id = this }
        loginRewardsValid = userData.rewardLoginStatus
        firstRechargeDialog = userData.giftNewUserTriggerFlag == 1
        notifyChange()
    }

    fun updateFromCompleteUserBody(body: CompleteUserDataInfoBody) {
        body.avatarUrl?.let { storeAvatar(it) }
        body.nickName?.let { storeNickname(it) }
        body.gender?.let { storeGender(it) }
        notifyChange()
    }

    private fun clear() {
        val keysToRemove = arrayOf(
            USER_ID, PHONE_NUM, NICK_NAME, GENDER, AVATAR_URL,
            USER_SOURCE, REGISTER_WAY, LAST_LOGIN_TIME, REGISTER_STATUS,
            PURCHASE_COURSE_STATUS, IS_PAID, TOKEN, COINS
        )
        mmkv.removeValuesForKeys(keysToRemove)
    }

    fun notifyChange(vararg keys: String?) {
        FlowEventBus.postSync(Events.UserDataEvent(*keys))
    }

    fun loginOut() {
        clear()
        notifyChange()
    }

    // --- 保留这些 store 方法，防止外部调用报错 ---

    fun storeUserId(userId: String?) {
        this.userId = userId
    }

    fun storePhoneNum(phoneNum: String?) {
        this.phoneNum = phoneNum
    }

    fun storeUid(uid: String?) {
        this.userUid = uid
    }

    fun storeFcmToken(fcmToken: String?) {
        this.fcmToken = fcmToken
    }

    fun storeNickname(nickName: String?) {
        this.nickName = nickName
    }

    fun storeGender(gender: Int?) {
        this.gender = gender ?: 0
    }

    fun storeAvatar(avatar: String?) {
        this.avatarUrl = avatar
    }

    fun storeUserSource(userSource: Int?) {
        this.userSource = userSource ?: 0
    }

    fun storeRegisterWay(registerWay: Int?) {
        this.registerWay = registerWay ?: 0
    }

    fun storeLastLoginTime(lastLoginTime: String?) {
        this.lastLoginTime = lastLoginTime
    }

    fun storeRegisterStatus(registerStatus: Int?) {
        this.registerStatus = registerStatus ?: 0
    }

    fun storeCoins(coins: Long?) {
        this.coins = coins?:0L
        notifyChange(COINS)
    }

    fun storeWallet(info: WalletInfo?) {
        this.coins = info?.coinsTotalNum ?: 0
        this.giveCoins = info?.giveCoionsNum ?: 0
    }

    fun storePurchaseCourseStatus(purchaseCourseStatus: Int?) {
        this.purchaseCourseStatus = purchaseCourseStatus ?: 0
    }

    fun storePaid(isPaid: Int?) {
        this.isPaid = isPaid ?: 0
    }

    fun hasPaid(): Boolean {
        return isPaid == 2
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
    }

    companion object {
        private const val PREF_ID = "USER_DATA"
        private const val FCM_TOKEN = "FCM_TOKEN"
        const val ID = "id"
        const val LOGIN_REWARDS_VALID = "login_rewards_valid"

        const val USER_ID = "userId"
        const val PHONE_NUM = "phoneNum"
        const val U_ID = "uid"
        const val NICK_NAME = "nickName"
        const val GENDER = "gender"
        const val AVATAR_URL = "avatarUrl"
        const val USER_SOURCE = "userSource"
        const val REGISTER_WAY = "registerWay"
        const val LAST_LOGIN_TIME = "lastLoginTime"
        const val REGISTER_STATUS = "registerStatus"
        const val TOKEN = "token"
        const val COINS = "computingPower"
        const val GIVE_COINS = "giveCoins"
        const val PURCHASE_COURSE_STATUS = "purchaseCourseStatus"
        const val IS_PAID = "IS_PAID"

        const val MALE = 1
        const val FEMALE = 2

        private const val REQUEST_NOTIFICATIONS_PERMISSION: String =
            "REQUEST_NOTIFICATIONS_PERMISSION"

        @JvmStatic
        fun get(): User {
            return App.instance.user
        }
    }
}
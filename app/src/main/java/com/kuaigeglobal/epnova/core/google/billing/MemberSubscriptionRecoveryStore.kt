package com.kuaigeglobal.epnova.core.google.billing

import com.android.billingclient.api.Purchase
import com.kuaigeglobal.epnova.core.User
import com.tencent.mmkv.MMKV

/**
 * 会员订阅未完成流程的最小恢复标记。
 *
 * 该标记只回答“当前 EpNova 账号是否有一笔需要重新查询 Google SUBS 的流程”，不是订单、
 * 也不是会员状态。为了避免在本地长期保存交易凭证，这里刻意不持久化 purchaseToken 和
 * purchasePrepareToken；恢复时必须重新调用 Play Billing 查询当前有效购买，再把真实查询结果
 * 交给后端 sync。
 *
 * 标记同时保存目标 productId 和后端下发的 obfuscatedAccountId。Google 账号可能存在其他
 * EpNova 订阅，恢复时只有两个字段都匹配的 Purchase 才能进入 sync，不能只取查询结果首项。
 */
object MemberSubscriptionRecoveryStore {

    private const val PREF_ID = "MEMBER_SUBSCRIPTION_RECOVERY"
    private const val KEY_OWNER_USER_ID = "owner_user_id"
    private const val KEY_PRODUCT_ID = "product_id"
    private const val KEY_OBFUSCATED_ACCOUNT_ID = "obfuscated_account_id"

    private val mmkv: MMKV by lazy {
        MMKV.mmkvWithID(PREF_ID, MMKV.MULTI_PROCESS_MODE)
    }

    /**
     * 保存恢复信号。owner 最后写入，避免进程恰好中断时把一组未写完整的数据读成有效标记。
     *
     * @return 当前账号标识及支付匹配字段均完整时返回 true；否则不创建不可恢复的半成品标记。
     */
    fun save(productId: String, obfuscatedAccountId: String): Boolean {
        val ownerUserId = currentOwnerUserId() ?: return false
        if (productId.isBlank() || obfuscatedAccountId.isBlank()) return false

        // 先让旧标记失效，再替换内容，最后提交 owner；避免账号切换时旧 owner 读到新账号
        // 尚未写完整的 product/obfuscated 组合。
        mmkv.removeValuesForKeys(arrayOf(KEY_OWNER_USER_ID))
        mmkv.encode(KEY_PRODUCT_ID, productId)
        mmkv.encode(KEY_OBFUSCATED_ACCOUNT_ID, obfuscatedAccountId)
        mmkv.encode(KEY_OWNER_USER_ID, ownerUserId)
        return true
    }

    /** 仅返回属于当前 EpNova 账号且字段完整的恢复信号。 */
    fun getForCurrentUser(): Signal? {
        val currentOwnerUserId = currentOwnerUserId() ?: return null
        val storedOwnerUserId = mmkv.decodeString(KEY_OWNER_USER_ID, null)
        if (storedOwnerUserId != currentOwnerUserId) return null

        val productId = mmkv.decodeString(KEY_PRODUCT_ID, null)?.takeIf { it.isNotBlank() }
        val obfuscatedAccountId = mmkv.decodeString(KEY_OBFUSCATED_ACCOUNT_ID, null)
            ?.takeIf { it.isNotBlank() }
        if (productId == null || obfuscatedAccountId == null) {
            // 当前账号的标记不完整时无法安全匹配商店购买，直接清掉损坏数据；绝不降级为
            // “任意 SUBS 首项”，否则可能把其他 EpNova 账号的订阅提交给当前账号。
            clearForCurrentUser()
            return null
        }
        return Signal(productId, obfuscatedAccountId)
    }

    fun hasPendingForCurrentUser(): Boolean = getForCurrentUser() != null

    /** 只清理当前账号自己的标记，切换账号时不会误删另一个账号尚未恢复的状态。 */
    fun clearForCurrentUser() {
        val currentOwnerUserId = currentOwnerUserId() ?: return
        if (mmkv.decodeString(KEY_OWNER_USER_ID, null) != currentOwnerUserId) return
        mmkv.removeValuesForKeys(
            arrayOf(KEY_OWNER_USER_ID, KEY_PRODUCT_ID, KEY_OBFUSCATED_ACCOUNT_ID)
        )
    }

    /**
     * 普通 class 避免自动生成的 toString 展开账号混淆标识。调用方只能用 [matches] 做匹配。
     */
    class Signal internal constructor(
        val productId: String,
        private val obfuscatedAccountId: String,
    ) {
        fun matches(purchase: Purchase): Boolean {
            return productId in purchase.products &&
                purchase.accountIdentifiers?.obfuscatedAccountId == obfuscatedAccountId
        }

        internal fun hasSameTarget(productId: String, obfuscatedAccountId: String): Boolean {
            return this.productId == productId &&
                this.obfuscatedAccountId == obfuscatedAccountId
        }

        internal fun hasSameTarget(other: Signal): Boolean {
            return productId == other.productId &&
                obfuscatedAccountId == other.obfuscatedAccountId
        }
    }

    /**
     * userId 是后端稳定用户标识；不使用 User.id，因为该字段实际承载鉴权 token，禁止落到
     * 会员恢复标记中。少数资料未初始化场景回退 userUid，仍不使用敏感鉴权信息。
     */
    private fun currentOwnerUserId(): String? {
        return User.get().userId?.takeIf { it.isNotBlank() }
            ?: User.get().userUid?.takeIf { it.isNotBlank() }
    }
}

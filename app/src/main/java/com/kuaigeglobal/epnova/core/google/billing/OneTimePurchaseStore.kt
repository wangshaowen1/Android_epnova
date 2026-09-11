package com.kuaigeglobal.epnova.core.google.billing

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import androidx.annotation.Keep
import com.android.billingclient.api.Purchase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.core.User
import com.nhsd.basic.utils.Logger
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 一次性商品未完成购买的加密存储。
 *
 * 文件位于 noBackupFilesDir，内容使用 Android Keystore 中的 AES-GCM 密钥加密。支付凭证、
 * 订单号和账号标识都禁止进入普通日志、Crashlytics message 或未加密的 MMKV。
 */
internal object OneTimePurchaseStore {

    private const val TAG = "OneTimePurchaseStore"
    private const val FILE_NAME = "one_time_purchase_v2.bin"
    private const val KEY_ALIAS = "epnova_one_time_purchase_v2"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val FILE_VERSION = 1
    private const val MAX_IV_SIZE = 32
    private const val MAX_CIPHER_TEXT_SIZE = 1024 * 1024

    private val lock = Any()
    private val gson = Gson()
    private val recordListType = object : TypeToken<List<OneTimePurchaseRecord>>() {}.type
    private val atomicFile: AtomicFile by lazy {
        AtomicFile(File(App.instance.noBackupFilesDir, FILE_NAME))
    }

    /** 预下单成功后立即保存；无法可靠落盘时返回 false，调用方不得继续拉起商店支付。 */
    fun savePrepared(
        businessType: OneTimePurchaseBusinessType,
        orderNo: String,
        productId: String,
        obfuscatedAccountId: String
    ): Boolean = synchronized(lock) {
        val ownerUserId = currentOwnerUserId() ?: return@synchronized false
        if (orderNo.isBlank() || productId.isBlank()) return@synchronized false

        val records = readRecords() ?: return@synchronized false
        val existingIndex = records.indexOfFirst { record ->
            record.orderNo == orderNo
        }
        if (existingIndex >= 0) {
            val existing = records[existingIndex]
            return@synchronized existing.ownerUserId == ownerUserId &&
                existing.businessType == businessType &&
                existing.productId == productId &&
                existing.obfuscatedAccountId == obfuscatedAccountId &&
                existing.purchaseToken == null
        }

        records += OneTimePurchaseRecord(
            ownerUserId = ownerUserId,
            businessType = businessType,
            orderNo = orderNo,
            productId = productId,
            purchaseToken = null,
            createdAt = System.currentTimeMillis(),
            obfuscatedAccountId = obfuscatedAccountId
        )
        writeRecords(records)
    }

    /**
     * 将 Google 已完成购买与预下单记录严格匹配，并在任何验单请求之前同步落盘。
     *
     * 匹配不依赖回调到达瞬间的登录态：用户资料可能在 Google 支付页期间变化，但 orderNo 与
     * productId 已在拉起前绑定到原账号记录。后续验单仍只会扫描当前账号的记录。
     */
    fun capturePurchased(purchases: List<Purchase>): List<OneTimePurchaseRecord> = synchronized(lock) {
        val records = readRecords() ?: return@synchronized emptyList()
        val captured = linkedMapOf<String, OneTimePurchaseRecord>()
        var changed = false
        val purchased = purchases
            .filter { purchase ->
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.purchaseToken.isNotBlank() &&
                    purchase.accountIdentifiers?.obfuscatedAccountId?.isNotBlank() == true
            }
            .distinctBy { purchase -> purchase.purchaseToken }

        purchased.forEach { purchase ->
            val purchaseToken = purchase.purchaseToken
            val orderNo = purchase.accountIdentifiers?.obfuscatedAccountId ?: return@forEach
            val matchingIndices = records.indices.filter { index ->
                val record = records[index]
                record.obfuscatedAccountId == orderNo &&
                    record.productId in purchase.products
            }
            if (matchingIndices.size != 1) return@forEach

            val index = matchingIndices.single()
            val existing = records[index]
            val matchingPurchases = purchased.count { candidate ->
                candidate.accountIdentifiers?.obfuscatedAccountId == existing.obfuscatedAccountId &&
                    existing.productId in candidate.products
            }
            if (matchingPurchases != 1) {
                reportFailure("ambiguous purchase match", IllegalStateException("multiple tokens"))
                return@forEach
            }
            if (existing.purchaseToken != null && existing.purchaseToken != purchaseToken) {
                reportFailure("purchase token collision", IllegalStateException("token mismatch"))
                return@forEach
            }
            val updated = if (existing.purchaseToken == purchaseToken) {
                existing
            } else {
                changed = true
                existing.copy(purchaseToken = purchaseToken)
            }
            records[index] = updated
            captured[updated.orderNo] = updated
        }

        if (changed && !writeRecords(records)) return@synchronized emptyList()
        captured.values.toList()
    }

    fun findMatchingPurchase(orderNo: String, purchases: List<Purchase>): Purchase? = synchronized(lock) {
        val record = readRecords()
            ?.singleOrNull { it.orderNo == orderNo }
            ?: return@synchronized null
        purchases.distinctBy { purchase -> purchase.purchaseToken }.singleOrNull { purchase ->
            purchase.accountIdentifiers?.obfuscatedAccountId == record.obfuscatedAccountId &&
                record.productId in purchase.products
        }
    }

    fun getForCurrentUser(): List<OneTimePurchaseRecord> = synchronized(lock) {
        val ownerUserId = currentOwnerUserId() ?: return@synchronized emptyList()
        readRecords()?.filter { it.ownerUserId == ownerUserId }.orEmpty()
    }

    fun isForCurrentUser(record: OneTimePurchaseRecord): Boolean = synchronized(lock) {
        currentOwnerUserId() == record.ownerUserId
    }

    /** 明确未进入交易或用户取消时，只允许清理尚未取得 purchaseToken 的预下单记录。 */
    fun removePreparedForCurrentUser(orderNo: String): Boolean = synchronized(lock) {
        val ownerUserId = currentOwnerUserId() ?: return@synchronized false
        val records = readRecords() ?: return@synchronized false
        val removed = records.removeAll { record ->
            record.ownerUserId == ownerUserId &&
                record.orderNo == orderNo &&
                record.purchaseToken == null
        }
        !removed || writeRecords(records)
    }

    /** 服务端明确验单成功后，按账号、订单和 token 三重匹配删除凭证。 */
    fun removeAfterServerSuccess(record: OneTimePurchaseRecord): Boolean = synchronized(lock) {
        val purchaseToken = record.purchaseToken ?: return@synchronized false
        val records = readRecords() ?: return@synchronized false
        val removed = records.removeAll { stored ->
            stored.ownerUserId == record.ownerUserId &&
                stored.orderNo == record.orderNo &&
                stored.obfuscatedAccountId == record.obfuscatedAccountId &&
                stored.purchaseToken == purchaseToken
        }
        !removed || writeRecords(records)
    }

    private fun readRecords(): MutableList<OneTimePurchaseRecord>? {
        if (!atomicFile.baseFile.exists()) return mutableListOf()
        return try {
            val plainText = DataInputStream(atomicFile.openRead()).use { input ->
                val version = input.readInt()
                require(version == FILE_VERSION) { "unsupported store version" }
                val ivSize = input.readInt()
                require(ivSize in 1..MAX_IV_SIZE) { "invalid iv size" }
                val iv = ByteArray(ivSize).also(input::readFully)
                val cipherTextSize = input.readInt()
                require(cipherTextSize in 1..MAX_CIPHER_TEXT_SIZE) { "invalid cipher text size" }
                val cipherText = ByteArray(cipherTextSize).also(input::readFully)

                val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
                cipher.doFinal(cipherText)
            }
            val records = gson.fromJson<List<OneTimePurchaseRecord>>(
                plainText.toString(Charsets.UTF_8),
                recordListType,
            ).orEmpty().toMutableList()
            require(records.all(OneTimePurchaseRecord::isValid)) { "invalid purchase record" }
            require(records.map { record -> record.orderNo }.distinct().size == records.size) {
                "duplicate purchase order"
            }
            records
        } catch (exception: Exception) {
            // 读取失败时保持原文件不动并让后续写入失败关闭，避免用空列表覆盖可能仍可恢复的凭证。
            reportFailure("read", exception)
            null
        }
    }

    private fun writeRecords(records: List<OneTimePurchaseRecord>): Boolean {
        var output: FileOutputStream? = null
        return try {
            val plainText = gson.toJson(records, recordListType).toByteArray(Charsets.UTF_8)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val cipherText = cipher.doFinal(plainText)

            val fileOutput = atomicFile.startWrite()
            output = fileOutput
            DataOutputStream(fileOutput).apply {
                writeInt(FILE_VERSION)
                writeInt(cipher.iv.size)
                write(cipher.iv)
                writeInt(cipherText.size)
                write(cipherText)
                flush()
            }
            atomicFile.finishWrite(fileOutput)
            output = null
            true
        } catch (exception: Exception) {
            runCatching {
                output?.let { fileOutput -> atomicFile.failWrite(fileOutput) }
            }
            reportFailure("write", exception)
            false
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER,
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun currentOwnerUserId(): String? {
        return User.get().userId?.takeIf { it.isNotBlank() }
            ?: User.get().userUid?.takeIf { it.isNotBlank() }
    }

    private fun reportFailure(stage: String, exception: Exception) {
        Logger.e(TAG, "$stage failed: ${exception.javaClass.simpleName}")
        runCatching {
            FirebaseCrashlytics.getInstance().recordException(
                IllegalStateException("One-time purchase secure store $stage failed", exception)
            )
        }
    }
}

/** 不覆写明文凭证到 toString，避免后续调试代码意外输出敏感字段。 */
@Keep
internal data class OneTimePurchaseRecord(
    val ownerUserId: String = "",
    val businessType: OneTimePurchaseBusinessType = OneTimePurchaseBusinessType.COINS,
    val orderNo: String = "",
    val productId: String = "",
    val purchaseToken: String? = null,
    val createdAt: Long = 0L,
    val obfuscatedAccountId: String? = null
) {
    fun isValid(): Boolean {
        return ownerUserId.isNotBlank() &&
            (businessType == OneTimePurchaseBusinessType.COINS ||
                businessType == OneTimePurchaseBusinessType.CREDITS) &&
            orderNo.isNotBlank() &&
            productId.isNotBlank() &&
            purchaseToken?.isNotBlank() != false &&
            obfuscatedAccountId?.isNotBlank() != false &&
            createdAt > 0L
    }

    override fun toString(): String {
        return "OneTimePurchaseRecord(businessType=$businessType, hasToken=${purchaseToken != null})"
    }
}

package com.kuaigeglobal.epnova.core.google.billing

import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kuaigeglobal.epnova.core.User
import com.tencent.mmkv.MMKV
import com.nhsd.basic.utils.Logger

/**
 * 支付失败事件的轻量持久化队列。
 *
 * 队列只保存订单号、事件幂等 ID 和商店错误码，不保存 token、支付凭证或用户鉴权信息。
 * 事件按账号隔离，避免账号切换后把旧账号的订单带到当前账号上报。
 */
internal object PaymentFailureReportStore {

    private const val TAG = "PaymentFailureReportStore"
    private const val PREF_ID = "PAYMENT_FAILURE_REPORT"
    private const val KEY_RECORDS = "records"
    private const val MAX_RECORD_COUNT = 200

    private val lock = Any()
    private val mmkv: MMKV by lazy {
        MMKV.mmkvWithID(PREF_ID, MMKV.MULTI_PROCESS_MODE)
    }
    private val gson = Gson()
    private val recordListType = object : TypeToken<List<PaymentFailureReportRecord>>() {}.type

    /** 保存成功或记录已存在时返回 true。 */
    fun enqueue(record: PaymentFailureReportRecord): Boolean = synchronized(lock) {
        val records = readRecords() ?: return@synchronized false
        if (records.any { it.ownerUserId == record.ownerUserId && it.clientEventId == record.clientEventId }) {
            return@synchronized true
        }

        records += record
        // 防止异常情况下队列无限增长；正常网络恢复时会在很短时间内清空。
        if (records.size > MAX_RECORD_COUNT) {
            records.subList(0, records.size - MAX_RECORD_COUNT).clear()
        }
        writeRecords(records)
    }

    fun getForCurrentUser(): List<PaymentFailureReportRecord> = synchronized(lock) {
        val ownerUserId = currentOwnerUserId() ?: return@synchronized emptyList()
        readRecords()?.filter { it.ownerUserId == ownerUserId }.orEmpty()
    }

    fun remove(record: PaymentFailureReportRecord): Boolean = synchronized(lock) {
        val records = readRecords() ?: return@synchronized false
        val removed = records.removeAll { stored ->
            stored.ownerUserId == record.ownerUserId &&
                stored.clientEventId == record.clientEventId
        }
        !removed || writeRecords(records)
    }

    private fun readRecords(): MutableList<PaymentFailureReportRecord>? {
        val encoded = mmkv.decodeString(KEY_RECORDS, null) ?: return mutableListOf()
        return try {
            gson.fromJson<List<PaymentFailureReportRecord>>(encoded, recordListType)
                .orEmpty()
                .toMutableList()
                .also { records ->
                    require(records.all(PaymentFailureReportRecord::isValid)) {
                        "invalid payment failure report record"
                    }
                }
        } catch (exception: Exception) {
            // 读取失败时不覆盖原始数据，避免把尚未上报的事件静默丢失。
            Logger.e(TAG, "read failed: ${exception.javaClass.simpleName}")
            null
        }
    }

    private fun writeRecords(records: List<PaymentFailureReportRecord>): Boolean {
        return try {
            mmkv.encode(KEY_RECORDS, gson.toJson(records, recordListType))
        } catch (exception: Exception) {
            Logger.e(TAG, "write failed: ${exception.javaClass.simpleName}")
            false
        }
    }

    private fun currentOwnerUserId(): String? {
        return User.get().userId?.takeIf { it.isNotBlank() }
            ?: User.get().userUid?.takeIf { it.isNotBlank() }
    }
}

/** 不覆写明文字段到日志，避免调试输出泄露订单或事件信息。 */
@Keep
internal data class PaymentFailureReportRecord(
    val ownerUserId: String = "",
    val orderNo: String = "",
    val clientEventId: String = "",
    val failReasonCode: String = "",
    val platformErrorCode: String? = null,
    val createdAt: Long = 0L,
) {
    fun isValid(): Boolean {
        return ownerUserId.isNotBlank() &&
            orderNo.length in 1..32 &&
            clientEventId.matches(Regex("[A-Za-z0-9_-]{1,64}")) &&
            failReasonCode in setOf(
                "STORE_LAUNCH_FAILED",
                "PRODUCT_UNAVAILABLE",
                "PAYMENT_METHOD_UNAVAILABLE",
            ) &&
            (platformErrorCode == null || platformErrorCode.matches(Regex("[A-Za-z0-9_.-]{1,128}"))) &&
            createdAt > 0L
    }

    override fun toString(): String {
        return "PaymentFailureReportRecord(reason=$failReasonCode, hasPlatformCode=${platformErrorCode != null})"
    }
}

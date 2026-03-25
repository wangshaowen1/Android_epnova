package com.kuaigeglobal.epnova.common.utils

import java.security.MessageDigest

/**
 * 加密/摘要工具类
 * 原 Utils.md5 迁移至此
 */
object EncryptUtils {

    /**
     * 计算多参数的 MD5
     */
    @JvmStatic
    fun md5(vararg args: String?): String {
        val buffer = StringBuilder()
        for (arg in args) {
            buffer.append(arg ?: "")
        }
        return getMD5(buffer.toString())
    }

    /**
     * 计算字符串 MD5
     */
    fun getMD5(message: String): String {
        var md5str = ""
        try {
            val md = MessageDigest.getInstance("MD5")
            val input = message.toByteArray()
            val buff = md.digest(input)
            md5str = bytesToHex(buff)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return md5str
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val md5str = StringBuilder()
        var digital: Int
        for (b in bytes) {
            digital = b.toInt()
            if (digital < 0) {
                digital += 256
            }
            if (digital < 16) {
                md5str.append("0")
            }
            md5str.append(Integer.toHexString(digital))
        }
        return md5str.toString()
    }
}
@file:JvmName("FormatUtils")

package com.kuaigeglobal.epnova.common.ext

import android.text.TextUtils
import android.text.format.DateUtils
import com.google.gson.Gson
import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.getResString
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// 数字格式化
fun Int.formatLikeNum(): String {
    if (this < 10000) return this.toString()
    return "${formatFloat(this / 10000f, 1)}w"
}

fun Int.formatPlays(): String {
    if (this < 10000) return this.toString()
    return String.format("%.1f万", this / 10000.0F)
}

fun Int.formatW(): String {
    if (this < 10000) return this.toString()
    return String.format(R.string.ten_thousand_format.getResString(), this / 10000)
}

fun Int.toUnitText(): String {
    return if (this < 1000) toString() else {
        formatDouble(toDouble() / 1000f, 1) + "k"
    }
}

fun Int.toUnitTextNoUnit(): String {
    return if (this < 10000) toString() else {
        formatDouble(toDouble() / 10000f, 1)
    }
}

fun Int.toUnitTextIgnoreZero(defaultZeroText: String = ""): String {
    if (this == 0) return defaultZeroText
    return if (this < 10000) toString() else {
        formatDouble(toDouble() / 10000f, 1) + "w+"
    }
}

fun Int.positionText(): String {
    val pos = this + 1
    return if (pos < 10) "$pos" else pos.toString()
}

fun Int?.stringText(): String = this?.toString() ?: "0"
fun Float.toUnitText(): String {
    return if (this < 10000) toString() else {
        formatFloat(this / 10000, 1) + "w+"
    }
}

fun Float.toUnitTextNoUnit(): String {
    return if (this < 10000) {
        formatFloat(this, 2)
    } else {
        formatFloat(this / 10000f, 1)
    }
}

fun Int.formatNumber(): String {
    return "%02d".format(this)
}


// 时间格式化
fun Long.formatDuration(): String {
    val minutes = this / DateUtils.MINUTE_IN_MILLIS
    val seconds = (this % DateUtils.MINUTE_IN_MILLIS) / DateUtils.SECOND_IN_MILLIS
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

fun Long.formatMoney(): String {
    val floatAmounts = this / 1000f
    return formatFloat(floatAmounts, 2)
}

fun Float.formatMoney() = formatFloat(this, 2)

fun Double.formatMoney() = formatDouble(this, 2)

// 浮点数格式化
fun Float?.formatDecimal() = String.format(Locale.getDefault(), "%.2f", this ?: 0f)

// 布尔值格式化
fun Boolean.string(): String = if (this) "是" else "否"

/**
 * 格式化消息数量
 */
fun Int?.formatMessageNum() = this?.let { if (it > 99) "99+" else it.toString() }

fun String.convertInt(): Int {
    return this.toIntOrNull() ?: 0
}

/**、
 * 保留 digits 位小数点 One
 * @receiver Double
 * @return String
 */
fun Double.formatOne(): String {
    // 创建一个DecimalFormat对象并设置模式
    // ##,##0.00 表示整数部分可以有无限位，如果没有则用0代替，小数部分必须有两位
    val formatter = DecimalFormat("##,##0.0", DecimalFormatSymbols.getInstance().apply {
        // 设置小数分隔符为点
        decimalSeparator = '.'
    })
    // 设置不进行四舍五入
    formatter.roundingMode = RoundingMode.DOWN
    // 格式化数字
    val formatted = formatter.format(this)
    // 检查格式化后的字符串，如果小数点后是0，则去掉
    return formatted.replace(".0", "")
}

// JSON序列化扩展
fun Any.toJsonString(): String {
    return Gson().toJson(this)
}

fun <T> String.jsonToObject(clazz: Class<T>): T {
    return Gson().fromJson(this, clazz)
}

fun formatMoney(amounts: Float, digits: Int): String {
    return formatFloat(amounts, digits)
}

fun formatPercentage(value: Double): String {
    return formatDouble(value, 2) + "%"
}

fun formatFloat(floatValue: Float, digits: Int): String {
    val doubleValue = floatValue.toString().toDouble()
    return formatDouble(doubleValue, digits)
}

fun formatDouble(doubleValue: Double, digits: Int): String {
    val formatter = DecimalFormat()
    formatter.setMaximumFractionDigits(digits)
    formatter.setGroupingSize(0)
    formatter.setRoundingMode(RoundingMode.FLOOR)
    return formatter.format(doubleValue)
}

fun formatDiscount(discountRatio: Int): String {
    val value = discountRatio / 10.0
    val formatter = DecimalFormat()
    formatter.setMaximumFractionDigits(1)
    formatter.setGroupingSize(0)
    formatter.setRoundingMode(RoundingMode.FLOOR)
    return formatter.format(value)
}


fun formatDisplay(li: Long): String {
    return "¥" + li.formatMoney()
}

fun formatYuan(li: Long): String {
    return li.formatMoney() + "元"
}

fun formatDisplayWithSpace(li: Long): String {
    return "¥ " + li.formatMoney()
}

fun formatHours(minutes: Int): String {
    val hours = minutes / 60f
    return formatDouble(hours.toDouble(), 1)
}

fun formatDoubleHours(minutes: Int): Double {
    return (minutes / 60f).toDouble()
}

fun formatDisplayNegative(li: Long): String {
    return "- ¥" + li.formatMoney()
}

fun formatDisplayPositive(li: Long): String {
    return "+ ¥" + li.formatMoney()
}

fun encryptPhoneNum(phoneNum: String): String? {
    return idMask(phoneNum, 3, 4)
}

fun encryptIdCard(idCardNum: String): String? {
    return idMask(idCardNum, 4, 4)
}

fun encryptRealName(realName: String): String? {
    return idMask(realName, 1, 0)
}

/**
 * 用户身份证号码的打码隐藏加星号加* 18位和非18位身份证处理均可成功处理 参数异常返回null
 *
 * @param idCardNum 身份证号码
 * @param front     需要显示前几位
 * @param end       需要显示末几位
 * @return 处理完成的身份证
 */
fun idMask(idCardNum: String, front: Int, end: Int): String? {
    // 身份证不能为空
    if (TextUtils.isEmpty(idCardNum)) {
        return null
    }
    // 需要截取的长度不能大于身份证号长度
    if ((front + end) > idCardNum.length) {
        return null
    }

    // 需要截取的不能小于0
    if (front < 0 || end < 0) {
        return null
    }
    // 计算*的数量
    val asteriskCount = idCardNum.length - (front + end)
    val asteriskStr = StringBuffer()
    for (i in 0..<asteriskCount) {
        asteriskStr.append("*")
    }
    val regex = "(\\w{$front})(\\w+)(\\w{$end})"
    return idCardNum.replace(regex.toRegex(), "$1$asteriskStr$3")
}
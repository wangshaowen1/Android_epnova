package com.kuaigeglobal.epnova.widget

import androidx.fragment.app.FragmentManager
import com.flqy.datepicker.NumberPicker
import com.kuaigeglobal.epnova.databinding.DialogIncomeMonthPickerBinding
import com.nhsd.basic.BaseBottomPushDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import java.util.Calendar
import java.util.Locale

/**
 * 通用年月选择器。
 *
 * 用于从底部弹出双列滚轮，左列选择年份，右列选择月份。
 * 可通过 minYearMonth/maxYearMonth 限定可选年月范围，Confirm 后返回 yyyy-MM 格式结果。
 */
class BottomYearMonthPickerDialog : BaseBottomPushDialogFragment<DialogIncomeMonthPickerBinding>() {

    companion object {
        // 滚轮可见项数量，保持和项目现有选择器一致。
        private const val WHEEL_ITEM_COUNT = 5

        /**
         * 展示年月选择器。
         *
         * @param manager 弹窗所在页面的 FragmentManager
         * @param minYearMonth 最早可选年月，格式 yyyy-MM；为空时默认当前年月
         * @param maxYearMonth 最晚可选年月，格式 yyyy-MM；为空时默认当前年月
         * @param selectedYearMonth 默认选中年月，格式 yyyy-MM；为空时默认 maxYearMonth
         * @param onConfirm 点击确认后的回调，返回 yyyy-MM
         */
        fun show(
            manager: FragmentManager,
            minYearMonth: String?,
            maxYearMonth: String?,
            selectedYearMonth: String?,
            onConfirm: (String) -> Unit
        ) {
            BottomYearMonthPickerDialog().apply {
                this.minYearMonth = MonthValue.parse(minYearMonth)
                this.maxYearMonth = MonthValue.parse(maxYearMonth)
                this.selectedYearMonth = MonthValue.parse(selectedYearMonth)
                this.onConfirm = onConfirm
            }.show(manager)
        }
    }

    // 最早可选年月，格式 yyyy-MM。
    private var minYearMonth: MonthValue? = null

    // 最晚可选年月，格式 yyyy-MM。
    private var maxYearMonth: MonthValue? = null

    // 打开弹窗时默认选中的年月，格式 yyyy-MM。
    private var selectedYearMonth: MonthValue? = null

    // 点击确认后通知调用方，遮罩关闭不会触发。
    private var onConfirm: ((String) -> Unit)? = null

    // 年份滚轮实际展示的数据源。
    private var yearValues = emptyList<Int>()

    // 月份滚轮实际展示的数据源，会根据当前年份动态裁剪。
    private var monthValues = emptyList<Int>()

    override fun onPrepareView() {
        super.onPrepareView()
        val now = MonthValue.current()
        var min = minYearMonth ?: now
        val max = maxYearMonth ?: now
        if (min > max) min = max
        val selected = (selectedYearMonth ?: max).coerceIn(min, max)

        setupYearPicker(min, max, selected.year)
        setupMonthPicker(min, max, selected.month)

        binding.btnConfirm.setOnSingleClickListener {
            val year = yearValues.getOrNull(binding.wheelYear.value) ?: return@setOnSingleClickListener
            val month = monthValues.getOrNull(binding.wheelMonth.value) ?: return@setOnSingleClickListener
            onConfirm?.invoke(MonthValue(year, month).key)
            dismissAllowingStateLoss()
        }
    }

    /**
     * 初始化年份滚轮。
     */
    private fun setupYearPicker(min: MonthValue, max: MonthValue, selectedYear: Int) {
        yearValues = (min.year..max.year).toList()
        binding.wheelYear.apply {
            setDisplayedValues(null)
            minValue = 0
            maxValue = yearValues.lastIndex
            displayedValues = yearValues.map { it.toString() }.toTypedArray()
            setSelectorWheelItemCount(WHEEL_ITEM_COUNT)
            setWrapSelectorWheel(false)
            setWheelScale(0.85f)
            value = yearValues.indexOf(selectedYear).coerceAtLeast(0)
            setOnValueChangedListener { _: NumberPicker, _: Int, _: Int ->
                setupMonthPicker(min, max, monthValues.getOrNull(binding.wheelMonth.value) ?: 1)
            }
        }
    }

    /**
     * 初始化或刷新月份滚轮。
     *
     * 当选中的年份是边界年份时，会裁剪月份范围，避免展示不可选月份。
     */
    private fun setupMonthPicker(min: MonthValue, max: MonthValue, selectedMonth: Int) {
        val selectedYear = yearValues.getOrNull(binding.wheelYear.value) ?: max.year
        val startMonth = if (selectedYear == min.year) min.month else 1
        val endMonth = if (selectedYear == max.year) max.month else 12
        monthValues = (startMonth..endMonth).toList()
        val monthIndex = monthValues.indexOf(selectedMonth.coerceIn(startMonth, endMonth)).coerceAtLeast(0)

        binding.wheelMonth.apply {
            setDisplayedValues(null)
            minValue = 0
            maxValue = monthValues.lastIndex
            displayedValues = monthValues.map { it.toString() }.toTypedArray()
            setSelectorWheelItemCount(WHEEL_ITEM_COUNT)
            setWrapSelectorWheel(false)
            setWheelScale(0.85f)
            value = monthIndex
        }
    }

    /**
     * 年月值对象，month 表示 1-12 的自然月份，用于比较范围和输出 yyyy-MM。
     */
    private data class MonthValue(val year: Int, val month: Int) : Comparable<MonthValue> {
        val key: String get() = String.format(Locale.US, "%04d-%02d", year, month)

        override fun compareTo(other: MonthValue): Int {
            return when {
                year != other.year -> year - other.year
                else -> month - other.month
            }
        }

        /**
         * 将当前年月限制在 min/max 范围内。
         */
        fun coerceIn(min: MonthValue, max: MonthValue): MonthValue {
            return when {
                this < min -> min
                this > max -> max
                else -> this
            }
        }

        companion object {
            /**
             * 解析 yyyy-MM 字符串，格式非法时返回 null。
             */
            fun parse(value: String?): MonthValue? {
                if (value.isNullOrBlank()) return null
                val parts = value.split("-")
                if (parts.size != 2) return null
                val year = parts[0].toIntOrNull() ?: return null
                val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
                return MonthValue(year, month)
            }

            /**
             * 当前自然月。
             */
            fun current(): MonthValue {
                val calendar = Calendar.getInstance()
                return MonthValue(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
            }
        }
    }
}

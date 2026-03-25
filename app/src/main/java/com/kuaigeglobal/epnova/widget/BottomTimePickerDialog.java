package com.kuaigeglobal.epnova.widget;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kuaigeglobal.epnova.R;
import com.kuaigeglobal.epnova.common.utils.DateUtil;
import com.kuaigeglobal.epnova.databinding.DialogTimePickerBinding;
import com.nhsd.basic.BaseBottomPushDialogFragment;

import java.util.Calendar;
import java.util.Locale;

public class BottomTimePickerDialog extends BaseBottomPushDialogFragment<DialogTimePickerBinding> implements View.OnClickListener {

    private static final String MAX_DATE = "max_date";
    private static final String MIN_DATE = "min_date";
    private static final int MAX_SHOW_DAYS = 14;
    private static final int MILLIS_OF_DAY = 1000 * 60 * 60 * 24;
    private Calendar mCurrentDate;
    private Calendar mMinDate;
    private Calendar mMaxDate;
    private Locale mCurrentLocale;
    private Calendar mTempDate;
    private int year = -1;
    private int monthOfYear = -1;
    private int dayOfMonth = -1;
    private long minDateTimeInMillis;
    private long maxDateTimeInMillis;
    private OnDateChangedListener mOnDateChangedListener;
    private OnTimePickedListener onTimePickedListener;
    private boolean titleVisible;

    public void setTitleVisible(boolean titleVisible) {
        this.titleVisible = titleVisible;
        DialogTimePickerBinding binding = getBindingNullable();
        if (binding != null && binding.title != null) {
            binding.title.setVisibility(titleVisible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public interface OnDateChangedListener {

        void onDateChanged(int year, int monthOfYear, int dayOfMonth, int hour, int minute);
    }

    public interface OnTimePickedListener {

        void onTimePicked(int year, int monthOfYear, int dayOfMonth, int hour, int minute);
    }

    @Override
    public void onPrepareView() {
        super.onPrepareView();
        setCurrentLocale(Locale.getDefault());
        DialogTimePickerBinding binding = getBindingNullable();
        if (binding == null) return;
        NumberPicker.OnValueChangeListener onChangeListener = (picker, oldVal, newVal) -> {
//            updateInputState();
            mTempDate.setTimeInMillis(mCurrentDate.getTimeInMillis());
            // take care of wrapping of days and months to update greater fields
            if (picker == binding.daySpinner) {
                int maxDayOfMonth = mTempDate.getActualMaximum(Calendar.DAY_OF_MONTH);
                if (oldVal == maxDayOfMonth && newVal == 1) {
                    mTempDate.add(Calendar.DAY_OF_MONTH, 1);
                } else if (oldVal == 1 && newVal == maxDayOfMonth) {
                    mTempDate.add(Calendar.DAY_OF_MONTH, -1);
                } else {
                    mTempDate.add(Calendar.DAY_OF_MONTH, newVal - oldVal);
                }
            } else if (picker == binding.hourSpinner) {
                mTempDate.set(Calendar.HOUR_OF_DAY, newVal);
            }
            // now set the date to the adjusted one
            setDate(mTempDate.get(Calendar.YEAR), mTempDate.get(Calendar.MONTH),
                    mTempDate.get(Calendar.DAY_OF_MONTH), mTempDate.get(Calendar.HOUR_OF_DAY), mTempDate.get(Calendar.MINUTE));
            updateSpinners();
//            updateCalendarView();
            notifyDateChanged();
        };
        //minutes
        binding.minuteSpinner.setOnLongPressUpdateInterval(100);
        binding.minuteSpinner.setOnValueChangedListener(onChangeListener);
//        binding.minuteSpinner.setDisplaySuffix("分");
        binding.minuteSpinner.setMinValue(0);
        binding.minuteSpinner.setMaxValue(5);
        binding.minuteSpinner.setDisplayedValues(new String[]{"00分", "10分", "20分", "30分", "40分", "50分"});
        binding.minuteSpinner.setWrapSelectorWheel(false);
        //hour
        binding.hourSpinner.setOnLongPressUpdateInterval(100);
        binding.hourSpinner.setOnValueChangedListener(onChangeListener);
//        binding.hourSpinner.setDisplaySuffix("点");
        binding.hourSpinner.setMinValue(1);
        binding.hourSpinner.setMaxValue(mCurrentDate.getActualMaximum(Calendar.HOUR_OF_DAY));
        binding.hourSpinner.setValue(mCurrentDate.get(Calendar.HOUR_OF_DAY));
        binding.hourSpinner.setWrapSelectorWheel(false);
        //day
        binding.daySpinner.setOnLongPressUpdateInterval(100);
        binding.daySpinner.setOnValueChangedListener(onChangeListener);
//        binding.daySpinner.setDisplaySuffix("日");

        setSelectorWheelItemCount(5);
        mTempDate.clear();


        // initialize to current date
        mCurrentDate.setTimeInMillis(System.currentTimeMillis());
        setDate(mCurrentDate.get(Calendar.YEAR), mCurrentDate.get(Calendar.MONTH), mCurrentDate
                .get(Calendar.DAY_OF_MONTH), mCurrentDate
                .get(Calendar.HOUR_OF_DAY), mCurrentDate
                .get(Calendar.MINUTE));
//        updateSpinners();
        initData();
        binding.title.setVisibility(titleVisible ? View.VISIBLE : View.INVISIBLE);
        binding.cancel.setOnClickListener(this);
        binding.confirm.setOnClickListener(this);
    }

    private void initData() {
        setMinDate(minDateTimeInMillis);
        setMaxDate(maxDateTimeInMillis);
        updateDate(year, monthOfYear, dayOfMonth, mCurrentDate.get(Calendar.HOUR_OF_DAY), 0);
    }


    public void setOnDateChangedListener(OnDateChangedListener onDateChangedListener) {
        this.mOnDateChangedListener = onDateChangedListener;
    }

    public void setOnTimePickedListener(OnTimePickedListener onTimePickedListener) {
        this.onTimePickedListener = onTimePickedListener;
    }

    public void setSelectorWheelItemCount(int selectorWheelItemCount) {
//        binding.yearSpinner.setSelectorWheelItemCount(selectorWheelItemCount);
//        binding.monthSpinner.setSelectorWheelItemCount(selectorWheelItemCount);
//        binding.daySpinner.setSelectorWheelItemCount(selectorWheelItemCount);
//        binding.hourSpinner.setSelectorWheelItemCount(selectorWheelItemCount);
//        binding.minuteSpinner.setSelectorWheelItemCount(selectorWheelItemCount);
    }


    public void setMinDate(long minDateTimeInMillis) {
        this.minDateTimeInMillis = minDateTimeInMillis;
        if (mMinDate != null) {
            mMinDate.setTimeInMillis(this.minDateTimeInMillis);
            if (mCurrentDate.before(mMinDate)) {
                mCurrentDate.setTimeInMillis(mMinDate.getTimeInMillis());
            }
            updateSpinners();
        }
    }

    public void setMaxDate(long maxDateTimeInMillis) {
        this.maxDateTimeInMillis = maxDateTimeInMillis;
        if (mMaxDate != null) {
            mMaxDate.setTimeInMillis(maxDateTimeInMillis);
            Log.i("BottomTimePickerDialog", "max:" + mMaxDate.get(Calendar.DAY_OF_MONTH));
            if (mCurrentDate.after(mMaxDate)) {
                mCurrentDate.setTimeInMillis(mMaxDate.getTimeInMillis());
            }
            updateSpinners();
        }

    }

    private void setCurrentLocale(Locale locale) {
        if (locale.equals(mCurrentLocale)) {
            return;
        }

        mCurrentLocale = locale;

        mTempDate = getCalendarForLocale(mTempDate, locale);
        mMinDate = getCalendarForLocale(mMinDate, locale);
        mMaxDate = getCalendarForLocale(mMaxDate, locale);
        mCurrentDate = getCalendarForLocale(mCurrentDate, locale);

    }

    private Calendar getCalendarForLocale(Calendar oldCalendar, Locale locale) {
        if (oldCalendar == null) {
            return Calendar.getInstance(locale);
        } else {
            final long currentTimeMillis = oldCalendar.getTimeInMillis();
            Calendar newCalendar = Calendar.getInstance(locale);
            newCalendar.setTimeInMillis(currentTimeMillis);
            return newCalendar;
        }
    }

    public void updateDate(int year, int month, int dayOfMonth, int hour, int minute) {
        this.year = year;
        this.monthOfYear = month;
        this.dayOfMonth = dayOfMonth;
        if (isAdded()) {
            if (!isNewDate(year, month, dayOfMonth, hour, minute)) {
                return;
            }
            setDate(year, month, dayOfMonth, hour, minute);
            updateSpinners();
            notifyDateChanged();
        }
    }

    private boolean isNewDate(int year, int month, int dayOfMonth, int hour, int minute) {
        return (mCurrentDate.get(Calendar.YEAR) != year
                || mCurrentDate.get(Calendar.MONTH) != dayOfMonth
                || mCurrentDate.get(Calendar.DAY_OF_MONTH) != month
                || mCurrentDate.get(Calendar.HOUR_OF_DAY) != hour
                || mCurrentDate.get(Calendar.MINUTE) != minute);
    }


    private void setDate(int year, int month, int dayOfMonth, int hour, int minute) {
        mCurrentDate.set(year, month, dayOfMonth, hour, minute);
        if (mCurrentDate.before(mMinDate)) {
            mCurrentDate.setTimeInMillis(mMinDate.getTimeInMillis());
        } else if (mCurrentDate.after(mMaxDate)) {
            mCurrentDate.setTimeInMillis(mMaxDate.getTimeInMillis());
        }
    }


    private void updateSpinners() {

        setDayDisplayValues();
        DialogTimePickerBinding binding = getBindingNullable();
        if (binding == null) return;
        if (mCurrentDate.equals(mMinDate)) {
            binding.hourSpinner.setMinValue(mMinDate.get(Calendar.HOUR_OF_DAY));
            binding.hourSpinner.setMaxValue(23);
        } else if (mCurrentDate.equals(mMaxDate)) {
            binding.hourSpinner.setMinValue(0);
            binding.hourSpinner.setMaxValue(mMaxDate.get(Calendar.HOUR_OF_DAY));
        } else {
            binding.hourSpinner.setMinValue(0);
            binding.hourSpinner.setMaxValue(23);
        }
        long diffDays = getDiffDays(mCurrentDate);
        binding.daySpinner.setValue((int) diffDays);
        binding.hourSpinner.setValue(mCurrentDate.get(Calendar.HOUR_OF_DAY));
    }

    private long getDiffDays(Calendar currentDay) {
        Calendar d = Calendar.getInstance();
        d.setTimeInMillis(mMinDate.getTimeInMillis());
        d.set(Calendar.HOUR_OF_DAY, 0);
        d.set(Calendar.MINUTE, 0);
        d.set(Calendar.SECOND, 0);
        d.set(Calendar.MILLISECOND, 0);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(currentDay.getTimeInMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        return (c.getTimeInMillis() - d.getTimeInMillis()) / MILLIS_OF_DAY;
    }

    private void setDayDisplayValues() {
        DialogTimePickerBinding binding = getBindingNullable();
        if (binding == null) return;
        int diffDays = MAX_SHOW_DAYS;
        if (mMinDate != null && mMaxDate != null) {
            diffDays = Math.max(1, (int) (getDiffDays(mMaxDate)));
        }
        binding.daySpinner.setMinValue(0);
        binding.daySpinner.setMaxValue(diffDays - 1);
        binding.daySpinner.setWrapSelectorWheel(false);
        Calendar stepCalendar = Calendar.getInstance();
        stepCalendar.setTimeInMillis(mMinDate.getTimeInMillis());
        String displayValues[] = new String[diffDays];
        for (int i = 0; i < diffDays; i++) {

            String monthStr = (stepCalendar.get(Calendar.MONTH) + 1) + getString(R.string.month);
            String dayStr;
            Calendar today = Calendar.getInstance();
            if (today.get(Calendar.DAY_OF_MONTH) == stepCalendar.get(Calendar.DAY_OF_MONTH)) {
                monthStr = "";
                dayStr =getString( R.string.today);
            } else if (today.get(Calendar.DAY_OF_MONTH) == stepCalendar.get(Calendar.DAY_OF_MONTH) - 1) {
                monthStr = "";
                dayStr = getString(R.string.tomorrow);
            } else {
                dayStr = stepCalendar.get(Calendar.DAY_OF_MONTH) + getString(R.string.day);
            }
            String hourStr = "(" + DateUtil.dateToWeek(stepCalendar) + ")";
            displayValues[i] = monthStr + dayStr + hourStr;
            stepCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }
//        Log.i("NumberPicker","set minvalue:"+minValue+",maxvalue:"+maxValue+", displayValues size:"+displayValues.length);
        binding.daySpinner.setDisplayedValues(displayValues);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null) {
            outState.putLong(MAX_DATE, maxDateTimeInMillis);
            outState.putLong(MIN_DATE, minDateTimeInMillis);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            long maxDate = savedInstanceState.getLong(MAX_DATE);
            long minDate = savedInstanceState.getLong(MIN_DATE);
            setMaxDate(maxDate);
            setMinDate(minDate);
        }
    }

    /**
     * @return The selected year.
     */
    public int getYear() {
        return mCurrentDate.get(Calendar.YEAR);
    }

    /**
     * @return The selected month.
     */
    public int getMonth() {
        return mCurrentDate.get(Calendar.MONTH);
    }

    /**
     * @return The selected day of month.
     */
    public int getDayOfMonth() {
        return mCurrentDate.get(Calendar.DAY_OF_MONTH);
    }

    private int getHour() {
        return mCurrentDate.get(Calendar.HOUR_OF_DAY);
    }

    private int getMinutes() {
        DialogTimePickerBinding binding = getBindingNullable();
        if (binding == null) return 0;
        return binding.minuteSpinner.getValue() * 10;
    }


    private void notifyDateChanged() {
//        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        if (mOnDateChangedListener != null) {
            mOnDateChangedListener.onDateChanged(getYear(), getMonth(), getDayOfMonth(), getHour(), getMinutes());
        }
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
                dismissAllowingStateLoss();
                break;
            case R.id.confirm:
                if (onTimePickedListener != null) {
                    onTimePickedListener.onTimePicked(getYear(), getMonth(), getDayOfMonth(), getHour(), getMinutes());
                }
                dismissAllowingStateLoss();
                break;
        }

    }

}

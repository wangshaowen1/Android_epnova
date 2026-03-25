/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kuaigeglobal.epnova.widget;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.flqy.datepicker.DatePicker;
import com.kuaigeglobal.epnova.databinding.BottomDatePickerDialogBinding;
import com.nhsd.basic.BaseBottomPushDialogFragment;

import java.util.Calendar;
import java.util.Date;


/**
 * A simple dialog containing an {@link android.widget.DatePicker}.
 *
 * <p>
 * See the <a href="{@docRoot}guide/topics/ui/controls/pickers.html">Pickers</a>
 * guide.
 * </p>
 */
public class BottomDatePickerDialog extends BaseBottomPushDialogFragment<BottomDatePickerDialogBinding> {

    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DAY = "day";

    private OnDateSetListener mCallBack;
    private Calendar mCalendar;

    private boolean mTitleNeedsUpdate = true;
    private int year = -1;
    private int monthOfYear = -1;
    private int dayOfMonth = -1;
    private long minDate;
    private long maxDate;

    @Override
    public void onPrepareView() {
        super.onPrepareView();
        mCalendar = Calendar.getInstance();
        BottomDatePickerDialogBinding binding = getBindingNullable();
        if (binding == null) return;
        binding.confirmBtn.setOnClickListener(onClickListener);
        binding.cancelBtn.setOnClickListener(onClickListener);

        if (year != -1 && monthOfYear != -1 && dayOfMonth != -1) {
            binding.datePicker.init(year, monthOfYear, dayOfMonth, null);
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            binding.datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH), null);
        }
        binding.datePicker.setSelectorWheelItemCount(5);
        binding.datePicker.setMinDate(minDate);
        binding.datePicker.setMaxDate(maxDate);
        binding.datePicker.updateDate(year, monthOfYear, dayOfMonth);
        binding.datePicker.setYearVisible(true);
        updateTitle(year, monthOfYear, dayOfMonth);
    }

    /**
     * The callback used to indicate the user is done filling in the date.
     */
    public interface OnDateSetListener {

        /**
         * @param view        The view associated with this listener.
         * @param year        The year that was set.
         * @param monthOfYear The month that was set (0-11) for compatibility with
         *                    {@link Calendar}.
         * @param dayOfMonth  The day of the month that was set.
         */
        void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth);
    }


    public void init(int year, int monthOfYear, int dayOfMonth, DatePicker.OnDateChangedListener listener) {
        BottomDatePickerDialogBinding binding = getBindingNullable();
        if (binding == null) return;
        binding.datePicker.init(year, monthOfYear, dayOfMonth, listener);
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            BottomDatePickerDialogBinding binding = getBindingNullable();
            if (binding == null) return;
            if (v.getId() == binding.confirmBtn.getId()) {
                dismissAllowingStateLoss();
                if (listener != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.YEAR, binding.datePicker.getYear());
                    calendar.set(Calendar.MONTH, binding.datePicker.getMonth());
                    calendar.set(Calendar.DAY_OF_MONTH, binding.datePicker.getDayOfMonth());
                    listener.onPickDate(calendar);
                }
            } else if (v.getId() == binding.cancelBtn.getId()) {
                dismissAllowingStateLoss();
            }
        }

    };

    public void onClick(DialogInterface dialog, int which) {
        tryNotifyDateSet();
    }

    public void onDateChanged(DatePicker view, int year, int month, int day) {
        // mDatePicker.init(year, month, day, this);
        // updateTitle(year, month, day);
    }

    public void updateDate(int year, int monthOfYear, int dayOfMonth) {
        this.year = year;
        this.monthOfYear = monthOfYear;
        this.dayOfMonth = dayOfMonth;
        BottomDatePickerDialogBinding binding = getBindingNullable();
        if (binding != null && binding.datePicker != null) {
            binding.datePicker.updateDate(year, monthOfYear, dayOfMonth);
        }
    }

    private void tryNotifyDateSet() {
        if (mCallBack != null) {
            BottomDatePickerDialogBinding binding = getBindingNullable();
            if (binding == null) return;
            binding.datePicker.clearFocus();
            mCallBack.onDateSet(binding.datePicker, binding.datePicker.getYear(), binding.datePicker.getMonth(), binding.datePicker.getDayOfMonth());
        }
    }

    @Deprecated
    private void updateTitle(int year, int month, int day) {
        BottomDatePickerDialogBinding binding = getBindingNullable();
        if (binding == null) return;
        if (!binding.datePicker.getCalendarViewShown()) {
            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, month);
            mCalendar.set(Calendar.DAY_OF_MONTH, day);
            String title = DateUtils.formatDateTime(getContext(), mCalendar.getTimeInMillis(),
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR
                            | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_ABBREV_WEEKDAY);
            // setTitle(title);
            mTitleNeedsUpdate = true;
        } else {
            if (mTitleNeedsUpdate) {
                mTitleNeedsUpdate = false;
                // setTitle(R.string.date_picker_dialog_title);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (outState == null) {
            outState = new Bundle();
        }
        BottomDatePickerDialogBinding binding = getBindingNullable();
        if (binding != null) {
            outState.putInt(YEAR, binding.datePicker.getYear());
            outState.putInt(MONTH, binding.datePicker.getMonth());
            outState.putInt(DAY, binding.datePicker.getDayOfMonth());
        }
        super.onSaveInstanceState(outState);

    }


    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            int year = savedInstanceState.getInt(YEAR);
            int month = savedInstanceState.getInt(MONTH);
            int day = savedInstanceState.getInt(DAY);
            BottomDatePickerDialogBinding binding = getBindingNullable();
            if (binding == null) return;
            binding.datePicker.init(year, month, day, null);
        }
    }


    public void setMinDate(long minDate) {
        this.minDate = minDate;
    }

    public void setMaxDate(long maxDate) {
        this.maxDate = maxDate;
    }

    public void updateDate(Date date) {
        if (date == null) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        updateDate(year, month, day);
    }

    private DatePickerListener listener;

    public void setDatePickListener(DatePickerListener listener) {
        this.listener = listener;
    }

    public interface DatePickerListener {
        void onPickDate(Calendar calendar);
    }
}

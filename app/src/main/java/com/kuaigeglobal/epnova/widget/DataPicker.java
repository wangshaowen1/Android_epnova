package com.kuaigeglobal.epnova.widget;

import android.text.TextUtils;
import android.view.View;

import androidx.fragment.app.FragmentManager;

import com.kuaigeglobal.epnova.R;
import com.kuaigeglobal.epnova.databinding.DialogDataPickerBinding;
import com.nhsd.basic.BaseBottomPushDialogFragment;

import java.util.List;

/**
 * Created by lu on 2020/7/22.
 */

public class DataPicker<DATA> extends BaseBottomPushDialogFragment<DialogDataPickerBinding> implements View.OnClickListener {
    public static final int WHEEL_ITEM_COUNT = 5;
    private static final int MIN_WHEEL_ITEM_COUNT = 4;

    private String title;
    private List<DATA> items;
    private int currentIndex = 0;
    private DATA selectedItem;
    private DisplayConverter converter;
    private DataPickerListener listener;

    public interface DisplayConverter<T> {
        String convert(T item);
    }

    public interface DataPickerListener<DATA> {
        void onItemPicked(DATA item);
    }


    public DataPicker setDisplayConverter(DisplayConverter<DATA> converter) {
        this.converter = converter;
        return this;
    }


    public DataPicker setItems(List<DATA> items) {
        this.items = items;
        return this;
    }

    @Override
    public void onPrepareView() {
        super.onPrepareView();
        DialogDataPickerBinding binding = getBindingNullable();
        if (binding == null) return;
        if (!TextUtils.isEmpty(title)) {
            binding.titleView.setVisibility(View.VISIBLE);
            binding.titleView.setText(title);
        }

        String[] displayedValues = getDisplayValues(items, 0);
        binding.dataPicker.setMaxValue(displayedValues.length - 1);
        binding.dataPicker.setMinValue(0);
        binding.dataPicker.setDisplayedValues(displayedValues);
//        dataPicker.setCameraDistance();
        binding.dataPicker.setSelectorWheelItemCount(displayedValues.length < WHEEL_ITEM_COUNT ? MIN_WHEEL_ITEM_COUNT : WHEEL_ITEM_COUNT);
        binding.dataPicker.setValue(currentIndex);
        binding.dataPicker.setWrapSelectorWheel(false);
        binding.dataPicker.setWheelScale(0.85f);
        binding.dataPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            selectedItem = items.get(newVal);
            currentIndex = newVal;
        });

        binding.cancel.setOnClickListener(this::onClick);
        binding.confirm.setOnClickListener(this::onClick);
    }

    private String[] getDisplayValues(List values, int startIndex) {
        startIndex = Math.min(values.size() - 1, startIndex);
        int size = values.size();
        boolean subList = false;
        if (startIndex > 0) {
            size = values.size() - startIndex + 1;
            subList = true;
        }

        String[] displayedValues = new String[size];
        if (subList) {
            displayedValues[0] = converter != null ? converter.convert(values.get(0)) : values.get(0).toString();
        }
        if (converter == null) {
            for (int i = startIndex; i < values.size(); i++) {
                String itemValue = values.get(i).toString();
                displayedValues[subList ? i - startIndex + 1 : i] = itemValue;
            }
        } else {
            for (int i = startIndex; i < values.size(); i++) {
                displayedValues[subList ? i - startIndex + 1 : i] = converter.convert(values.get(i));
            }
        }
        return displayedValues;
    }

    public DataPicker<DATA> setSelectedIndex(int index) {
        this.currentIndex = index;
        selectedItem = items.get(index);
        return this;
    }


    private DataPicker setTitle(String title) {
        this.title = title;
        return this;
    }

    public DataPicker setDataPickerListener(DataPickerListener<DATA> listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
                dismiss();
                break;
            case R.id.confirm:
                if (listener != null) {
                    listener.onItemPicked(selectedItem);
                }
                dismiss();
                break;
        }
    }

    public static <DATA> DataPicker show(FragmentManager manager, List<DATA> items, DataPickerListener<DATA> listener) {
        return show(manager, null, items, listener);
    }

    public static <DATA> DataPicker show(FragmentManager manager, String title, List<DATA> items, DataPickerListener<DATA> listener) {
        DataPicker<DATA> picker = new DataPicker();
        picker.setTitle(title);
        picker.setItems(items);
        picker.show(manager);
        picker.setSelectedIndex(0);
        picker.setDataPickerListener(listener);
        return picker;
    }

}

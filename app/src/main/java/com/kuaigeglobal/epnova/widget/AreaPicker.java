package com.kuaigeglobal.epnova.widget;

import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;

import androidx.fragment.app.FragmentManager;

import com.flqy.datepicker.NumberPicker;
import com.kuaigeglobal.epnova.App;
import com.kuaigeglobal.epnova.core.manager.PlaceManager;
import com.kuaigeglobal.epnova.databinding.AreaPickerBinding;
import com.nhsd.basic.BaseBottomPushDialogFragment;

import java.util.ArrayList;


/**
 * Created by liurenhui on 15/3/25.
 */
public class AreaPicker extends BaseBottomPushDialogFragment<AreaPickerBinding> implements View.OnClickListener {

    private ArrayList<String> provinceList;
    private SparseArray<ArrayList<String>> cityMap;

    private AreaPickerListener listener;
    private boolean unlimited = false;
    private String defaultProvince, defaultCity;


    public void setUnlimited(boolean unlimited) {
        this.unlimited = unlimited;
    }

    public void setDefaultProvinceAndCity(String province, String city) {
        this.defaultProvince = province;
        this.defaultCity = city;
    }

    @Override
    public void onPrepareView() {
        super.onPrepareView();
        PlaceManager.Region region = unlimited ? App.getInstance().getPlaceManager().getRegionUnlimited() :
                App.getInstance().getPlaceManager().getRegion();
        provinceList = region.provinces;
        cityMap = region.cities;
        AreaPickerBinding binding = getBindingNullable();
        if (binding == null) return;
        binding.wheelProvince.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                updateCity();
            }
        });

        String[] displayValues = new String[provinceList.size()];
        binding.wheelProvince.setMinValue(0);
        binding.wheelProvince.setMaxValue(displayValues.length - 1);
        binding.wheelProvince.setDisplayedValues(provinceList.toArray(displayValues));
        binding.wheelProvince.setSelectorWheelItemCount(5);
        binding.wheelProvince.setWrapSelectorWheel(false);
        binding.confirm.setOnClickListener(this);
        binding.cancel.setOnClickListener(this);

        if (!TextUtils.isEmpty(defaultProvince)) {
            int defaultIndex = provinceList.indexOf(defaultProvince);
            updateProvince(defaultIndex >= 0 ? defaultIndex : 0);
        } else {
            updateProvince(0);
        }
        //updateDefault();
    }

    private void updateDefault() {
        if (!TextUtils.isEmpty(defaultProvince) || !TextUtils.isEmpty(defaultCity)) {
            int defaultIndex = provinceList.indexOf(defaultProvince);
            if (defaultIndex >= 0) {
                AreaPickerBinding binding = getBindingNullable();
                if (binding == null) return;
                binding.wheelProvince.setValue(defaultIndex);
                int provinceIndex = binding.wheelProvince.getValue();
                ArrayList<String> cityList = cityMap.get(provinceIndex);
                final int cityIndex = cityList.indexOf(defaultCity);
                if (cityIndex >= 0) {
                    binding.wheelCity.setValue(cityIndex);
                }
            }
        }
    }

    public interface AreaPickerListener {
        void onPicked(String province, String city);
    }


    @Override
    public void onClick(View v) {
        AreaPickerBinding binding = getBindingNullable();
        if (binding == null) return;
        if (v == binding.cancel) {
            dismiss();
        } else if (v == binding.confirm) {
            if (listener != null) {
                String province = provinceList.get(binding.wheelProvince.getValue());
                String city = cityMap.get(binding.wheelProvince.getValue()).get(binding.wheelCity.getValue());
                listener.onPicked(province, city);
            }
            dismiss();
        }
    }

    public void setTitle(String title) {
        if (title == null)
            title = "";
        AreaPickerBinding binding = getBindingNullable();
        if (binding == null) return;
        binding.title.setText(title);
    }


    private void updateProvince(int index) {
        AreaPickerBinding binding = getBindingNullable();
        if (binding == null) return;
        binding.wheelProvince.setValue(index);
        updateDefaultCity();
    }

    private void updateDefaultCity() {
        updateCity();
        if (!TextUtils.isEmpty(defaultCity)) {
            AreaPickerBinding binding = getBindingNullable();
            if (binding == null) return;
            int provinceIndex = binding.wheelProvince.getValue();
            ArrayList<String> cityList = cityMap.get(provinceIndex);
            final int cityIndex = cityList.indexOf(defaultCity);
            if (cityIndex >= 0) {
                binding.wheelCity.setValue(cityIndex);
            }
        }
    }

    private void updateCity() {
        AreaPickerBinding binding = getBindingNullable();
        if (binding == null) return;
        int provinceIndex = binding.wheelProvince.getValue();

        ArrayList<String> cityList = cityMap.get(provinceIndex);
        String[] displayValues = new String[cityList.size()];

        binding.wheelCity.setSelectorWheelItemCount(5);


        binding.wheelCity.setDisplayedValues(null);
        binding.wheelCity.setMinValue(0);
        binding.wheelCity.setMaxValue(displayValues.length - 1);
        binding.wheelCity.setDisplayedValues(cityList.toArray(displayValues));
//		binding.wheelCity.setWrapSelectorWheel(false);
    }


    public void setListener(AreaPickerListener listener) {
        this.listener = listener;
    }

    public static AreaPicker show(FragmentManager manager, AreaPickerListener listener) {
        AreaPicker picker = new AreaPicker();
        picker.show(manager);
        picker.setListener(listener);
        return picker;
    }


}

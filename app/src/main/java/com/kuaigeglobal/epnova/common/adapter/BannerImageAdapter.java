package com.kuaigeglobal.epnova.common.adapter;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.kuaigeglobal.epnova.R;
import com.zhpan.bannerview.BaseBannerAdapter;
import com.zhpan.bannerview.BaseViewHolder;

public abstract class BannerImageAdapter<T> extends BaseBannerAdapter<T> {
    @Override
    protected void bindData(BaseViewHolder<T> holder, T data, int position, int pageSize) {
        ImageView image = holder.findViewById(R.id.image);
        onBindView(image, data, position, pageSize);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBannerClick(v, data, position);
            }
        });

    }

    public abstract void onBindView(ImageView image, T data, int position, int pageSize);

    public abstract void onBannerClick(@Nullable View view, T data, int position);

    @Override
    public int getLayoutId(int viewType) {
        return R.layout.item_banner_image;
    }
}

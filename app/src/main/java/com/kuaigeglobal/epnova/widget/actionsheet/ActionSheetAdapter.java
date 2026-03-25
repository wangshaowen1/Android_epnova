package com.kuaigeglobal.epnova.widget.actionsheet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import com.kuaigeglobal.epnova.R;

import java.util.List;

/**
 * Created by Sai on 15/8/9.
 */
public class ActionSheetAdapter extends BaseAdapter {
    private List<String> mDatas;
    private List<String> mDestructive;
    private boolean headerVisible;

    public ActionSheetAdapter(List<String> datas, List<String> destructive, boolean headerVisible) {
        this.mDatas = datas;
        this.mDestructive = destructive;
        this.headerVisible = headerVisible;
    }

    @Override
    public int getCount() {
        return mDatas.size();
    }

    @Override
    public Object getItem(int position) {
        return mDatas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String data = mDatas.get(position);
        Holder holder = null;
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            view = inflater.inflate(R.layout.action_sheet_item_button, null);
            holder = createHolder(view);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }
        holder.updateUI(parent.getContext(), data, position);
        holder.divider.setVisibility(position == 0 && !headerVisible ? View.GONE : View.VISIBLE);
        if (position == 0 && !headerVisible) {
            view.setBackgroundResource(R.drawable.bg_actionsheet_item_top);
        } else {
            view.setBackgroundResource(R.drawable.bg_actionsheet_item_middle);
        }
        return view;
    }

    public Holder createHolder(View view) {
        return new Holder(view);
    }

    class Holder {
        private TextView tvAlert;
        private View divider;

        public Holder(View view) {
            tvAlert = view.findViewById(R.id.tvAlert);
            divider = view.findViewById(R.id.divider);
        }

        public void updateUI(Context context, String data, int position) {

            tvAlert.setText(data);
            if (mDestructive != null && mDestructive.contains(data)) {
                tvAlert.setTextColor(context.getResources().getColor(R.color.text_color_2_dark));
            } else {
                tvAlert.setTextColor(context.getResources().getColor(R.color.text_color_2_dark));
            }
        }
    }
}
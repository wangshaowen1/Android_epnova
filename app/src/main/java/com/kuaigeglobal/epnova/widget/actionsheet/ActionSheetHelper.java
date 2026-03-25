package com.kuaigeglobal.epnova.widget.actionsheet;


import com.blankj.utilcode.util.StringUtils;
import com.kuaigeglobal.epnova.R;

public class ActionSheetHelper {
    public static ActionSheetDialog createDialog(String[] items, OnItemClickListener onItemClickListener) {
        ActionSheetDialog.Builder builder = new ActionSheetDialog.Builder();
        builder.setOthers(items);
        builder.setCancelText(StringUtils.getString(R.string.cancel));
        builder.setOnItemClickListener(onItemClickListener);
        return builder.build();
    }
}

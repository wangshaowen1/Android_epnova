package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

public class CursorAutoEndEditText extends AppCompatEditText {


    public CursorAutoEndEditText(Context context) {
        super(context);
    }

    public CursorAutoEndEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CursorAutoEndEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        if(text!=null) {
            setSelection(text.length());
        }
    }

}

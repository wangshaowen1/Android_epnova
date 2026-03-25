package com.kuaigeglobal.epnova.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 当EditText同时设置了imeOption和MultiLine时, imeOPtion会失效
 * 这个类用来修正这个问题.
 * 不过回车键还是会没有就是
 * 新增对换行符的限制。
 *
 * @author ncikywan
 */
public class XEditText extends AppCompatEditText {

    private OnEditorActionListener onEditorActionListener;

    public XEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public XEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public XEditText(Context context) {
        super(context);
        init();
    }

    private boolean noNextLine = false;

    public void setNoNextLine(boolean noNextLine) {
        this.noNextLine = noNextLine;
    }

    private boolean noSpace = false;

    public void setNoEmojAndSpace(boolean noSpace, int maxLength) {
        this.noSpace = noSpace;
        if (noSpace) {
            InputFilter filter = (source, start, end, dest, dstart, dend) -> {
                if (isEmoji(source)) {
                    return "";
                }
                if (" ".equals(source)) {
                    return "";
                } else {
                    return null;
                }
            };
            setFilters(new InputFilter[]{filter, new LengthFilter(maxLength)});
        } else {
            setFilters(new InputFilter[]{new LengthFilter(maxLength)});
        }
    }

    public void setMaxLength(int maxLength) {
        setFilters(new InputFilter[]{new LengthFilter(maxLength)});
    }


    public interface OnTextOutOfBondsListener {
        public void onTextOutOfBonds(CharSequence source);
    }

    private OnTextOutOfBondsListener onTextOutOfBondsListener;

    public void setOnTextOutOfBondsListener(OnTextOutOfBondsListener onTextOutOfBondsListener) {
        this.onTextOutOfBondsListener = onTextOutOfBondsListener;
    }

    public class LengthFilter implements InputFilter {

        private final int mMax;

        public LengthFilter(int max) {
            mMax = max;
        }

        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                                   int dstart, int dend) {
            int replacedLength = dend - dstart;
            int currentLength = dest.length() - replacedLength;
            int newTextLength = end - start;
            int totalLength = currentLength + newTextLength;

            // 触发回调：输入后总长度超限
            if (totalLength > mMax) {
                if (onTextOutOfBondsListener != null) {
                    onTextOutOfBondsListener.onTextOutOfBonds(source);
                }
            }

            // 计算允许输入的字符数
            int keep = mMax - currentLength;
            if (keep <= 0) {
                return "";
            } else if (keep >= newTextLength) {
                return null; // 无需截断
            } else {
                keep = Math.max(keep, 0);
                if (Character.isHighSurrogate(source.charAt(keep - 1))) {
                    keep--;
                    if (keep == 0) return "";
                }
                return source.subSequence(start, start + keep);
            }
        }

        /**
         * @return the maximum length enforced by this input filter
         */
        public int getMax() {
            return mMax;
        }
    }

    private void init() {
        super.setOnEditorActionListener((v, actionId, event) -> {
            if (event != null) {
                if (noSpace) {
                    return event.getKeyCode() == KeyEvent.KEYCODE_SPACE || event.getKeyCode() == KeyEvent.KEYCODE_TAB;
                }
                if (noNextLine) {
                    return event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
                }

            }
            if (onEditorActionListener != null) {
                return onEditorActionListener.onEditorAction(v, actionId, event);
            }
            return false;
        });
    }

//    @Override
//    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
//        InputConnection connection = super.onCreateInputConnection(outAttrs);
//        if (connection != null && ((getInputType() & EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) == EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) && getImeOptions() != EditorInfo.IME_NULL) {
//            outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
//            outAttrs.inputType &= ~EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
//        }
//        return connection;
//    }


    @Override
    public void setOnEditorActionListener(OnEditorActionListener l) {
        this.onEditorActionListener = l;
    }

//    private static boolean isEmojiCharacter(char codePoint) {
//        return (codePoint == 0x0) || (codePoint == 0x9) || (codePoint == 0xA) || (codePoint == 0xD)
//                || ((codePoint >= 0x20) && (codePoint <= 0xD7FF)) || ((codePoint >= 0xE000) && (codePoint <= 0xFFFD))
//                || ((codePoint >= 0x10000) && (codePoint <= 0x10FFFF));
//    }

    /**
     * 正则判断emoji表情
     */
    protected boolean isEmoji(CharSequence CharSequence) {
        for (int i = 0; i < CharSequence.length(); i++) {
            int type = Character.getType(CharSequence.charAt(i));
            // 检查字符类型是否属于“其他符号”或“代理字符”
            if (type == Character.SURROGATE || type == Character.OTHER_SYMBOL) {
                return true;
            }
        }
        return false;
    }
    /**
     * Here you can catch paste, copy and cut events
     */
    @Override
    public boolean onTextContextMenuItem(int id) {
        boolean consumed = super.onTextContextMenuItem(id);
        switch (id) {
            case android.R.id.cut:
                onTextCut();
                break;
            case android.R.id.paste:
                onTextPaste();
                break;
            case android.R.id.copy:
                onTextCopy();
        }
        return consumed;
    }

    private void onTextCopy() {
    }

    private void onTextPaste() {
        if (onTextChangedListener != null) {
            onTextChangedListener.onTextPasted();
        }
    }

    private void onTextCut() {
    }

    private OnTextChangedListener onTextChangedListener;

    public void setOnTextChangedListener(OnTextChangedListener onTextChangedListener) {
        this.onTextChangedListener = onTextChangedListener;
    }

    public interface OnTextChangedListener {
        public void onTextPasted();
    }

    public void setCursorColor(@ColorInt int color) {
        try {
            // Get the cursor resource id
            @SuppressLint("SoonBlockedPrivateApi") Field field = TextView.class.getDeclaredField("mCursorDrawableRes");
            field.setAccessible(true);
            int drawableResId = field.getInt(this);

            // Get the editor
            field = TextView.class.getDeclaredField("mEditor");
            field.setAccessible(true);
            Object editor = field.get(this);

            // Get the drawable and set a color filter
            Drawable drawable = ContextCompat.getDrawable(this.getContext(), drawableResId);
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            Drawable[] drawables = {drawable, drawable};

            // Set the drawables
            field = editor.getClass().getDeclaredField("mCursorDrawable");
            field.setAccessible(true);
            field.set(editor, drawables);
        } catch (Exception ignored) {
        }
    }

}

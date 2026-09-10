package com.kuaigeglobal.epnova.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

import com.kuaigeglobal.epnova.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 当EditText同时设置了imeOption和MultiLine时, imeOPtion会失效
 * 这个类用来修正这个问题.
 * 不过回车键还是会没有就是
 * 新增对换行符的限制。
 *
 * @author ncikywan
 */
public class XEditText extends AppCompatEditText {

    public static final int RESTRICTION_NONE = 0;
    public static final int RESTRICTION_NO_EMOJI = 1;
    /** 禁止空格、Tab 等横向空白，回车和换行由 RESTRICTION_NO_NEXT_LINE 独立控制。 */
    public static final int RESTRICTION_NO_SPACE = 2;
    public static final int RESTRICTION_NO_NEXT_LINE = 4;
    private static final int ALL_RESTRICTIONS = RESTRICTION_NO_EMOJI
            | RESTRICTION_NO_SPACE | RESTRICTION_NO_NEXT_LINE;
    private int inputRestrictions;

    private OnEditorActionListener onEditorActionListener;

    public XEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    public XEditText(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.appcompat.R.attr.editTextStyle);
    }

    public XEditText(Context context) {
        this(context, null);
    }

    /** 返回当前有效限制；可以用按位或组合 RESTRICTION_* 常量。 */
    public int getInputRestrictions() {
        return inputRestrictions;
    }

    /** 完整替换限制，传 RESTRICTION_NONE 关闭；不追溯修改已有文本或长度上限。 */
    public void setInputRestrictions(int restrictions) {
        inputRestrictions = restrictions & ALL_RESTRICTIONS;
        updateRestrictionFilter();
    }

    private final InputFilter restrictionFilter = (source, start, end, dest, dstart, dend) -> {
        if (start == end) return null;
        int restrictions = getInputRestrictions();
        boolean rejected = (restrictions & RESTRICTION_NO_EMOJI) != 0
                && isEmoji(source.subSequence(start, end));
        for (int i = start; !rejected && i < end; i++) {
            char ch = source.charAt(i);
            boolean nextLine = ch == '\n' || ch == '\r';
            boolean space = ch == '\t' || Character.getType(ch) == Character.SPACE_SEPARATOR;
            rejected = ((restrictions & RESTRICTION_NO_SPACE) != 0 && space)
                    || ((restrictions & RESTRICTION_NO_NEXT_LINE) != 0 && nextLine);
        }
        // 拒绝选区替换时保留原文，返回空串会把选中文字删除。
        return rejected ? dest.subSequence(dstart, dend) : null;
    };

    public void setMaxLength(int maxLength) {
        List<InputFilter> filters = new ArrayList<>();
        for (InputFilter filter : getFilters()) {
            if (filter != restrictionFilter && !(filter instanceof LengthFilter)
                    && !(filter instanceof InputFilter.LengthFilter)) {
                filters.add(filter);
            }
        }
        filters.add(new LengthFilter(maxLength));
        setFilters(filters.toArray(new InputFilter[0]));
    }

    private void updateRestrictionFilter() {
        setFilters(getFilters());
    }

    @Override
    public void setFilters(InputFilter[] inputFilters) {
        // TextView 构造时会调用此方法，此时本类的过滤器尚未初始化。
        if (restrictionFilter == null) {
            super.setFilters(inputFilters);
            return;
        }
        List<InputFilter> filters = new ArrayList<>();
        boolean restricted = getInputRestrictions() != RESTRICTION_NONE;
        if (restricted) filters.add(restrictionFilter);
        for (InputFilter filter : inputFilters) {
            if (filter == restrictionFilter) continue;
            // XML 的 android:maxLength 也保护原选区；保留业务自定义长度过滤器的重写规则。
            if (restricted && filter.getClass() == InputFilter.LengthFilter.class) {
                filter = new LengthFilter(((InputFilter.LengthFilter) filter).getMax());
            }
            filters.add(filter);
        }
        super.setFilters(filters.toArray(new InputFilter[0]));
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
            mMax = Math.max(0, max);
        }

        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                                   int dstart, int dend) {
            if (start == end) return null;
            int replacedLength = dend - dstart;
            int currentLength = dest.length() - replacedLength;
            int newTextLength = end - start;
            int totalLength = currentLength + newTextLength;

            // 前置过滤器拒绝替换会返回原选区；即使旧文本超限，也不能继续截短原文。
            if (newTextLength == replacedLength
                    && TextUtils.equals(source.subSequence(start, end), dest.subSequence(dstart, dend))) {
                return null;
            }

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
                if (Character.isHighSurrogate(source.charAt(start + keep - 1))) {
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

    private void init(AttributeSet attrs, int defStyleAttr) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.XEditText, defStyleAttr, 0);
        try {
            setInputRestrictions(a.getInt(R.styleable.XEditText_inputRestrictions, RESTRICTION_NONE));
        } finally {
            a.recycle();
        }
        super.setOnEditorActionListener((v, actionId, event) -> {
            if (event != null) {
                if ((inputRestrictions & RESTRICTION_NO_SPACE) != 0 && (event.getKeyCode() == KeyEvent.KEYCODE_SPACE
                        || event.getKeyCode() == KeyEvent.KEYCODE_TAB)) {
                    return true;
                }
                if ((inputRestrictions & RESTRICTION_NO_NEXT_LINE) != 0 && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    return true;
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
     * 判断 emoji 表情（沿用现有字符类型规则）
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

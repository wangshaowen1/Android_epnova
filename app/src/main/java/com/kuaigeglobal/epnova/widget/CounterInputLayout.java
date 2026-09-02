package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.kuaigeglobal.epnova.R;


/**
 * Created by lu on 2017/4/9.
 */

public class CounterInputLayout extends FrameLayout implements TextWatcher {

    private EditTextWithScrollView contentInput;
    private TextView inputLengthHint;
    private TextView inputLength;
    private View hintLayout;
    private boolean noNextLine;
    private boolean contentBold;

    private int contentPaddingLeft;
    private int contentPaddingTop;
    private int contentPaddingRight;
    private int contentPaddingBottom;

    private int maxLength;

    public CounterInputLayout(Context context) {
        super(context);
        init(context, null);
    }

    public CounterInputLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CounterInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {


        View v = inflate(context, R.layout.limited_text_layout, this);
        contentInput = v.findViewById(R.id.contentInput);
        inputLengthHint = v.findViewById(R.id.inputLengthHint);
        inputLength = v.findViewById(R.id.inputLength);
        hintLayout = v.findViewById(R.id.hint_layout);
        contentInput.addTextChangedListener(this);
//        contentInput.setText("123");

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CounterInputLayout);
            maxLength = a.getInt(R.styleable.CounterInputLayout_inputMaxLength, 0);
            setMaxLength(maxLength);
            String hint = a.getString(R.styleable.CounterInputLayout_inputHint);
            if (a.hasValue(R.styleable.CounterInputLayout_inputLines)) {
                int lines = a.getInt(R.styleable.CounterInputLayout_inputLines, 0);
                if (lines > 0) {
                    setLines(lines);
                }
            }
            if (a.hasValue(R.styleable.CounterInputLayout_inputMinLines)) {
                int minLines = a.getInt(R.styleable.CounterInputLayout_inputMinLines, 0);
                contentInput.setMinLines(minLines);
            }
            if (a.hasValue(R.styleable.CounterInputLayout_inputMaxLines)) {
                int maxLines = a.getInt(R.styleable.CounterInputLayout_inputMaxLines, 0);
                contentInput.setMaxLines(maxLines);
            }
            noNextLine = a.getBoolean(R.styleable.CounterInputLayout_inputNoNextLine, false);
            contentInput.setNoNextLine(noNextLine);
            if (!TextUtils.isEmpty(hint)) {
                setHint(hint);
            }

            if (a.hasValue(R.styleable.CounterInputLayout_inputBackground)) {
                setBackgroundResource(a.getResourceId(R.styleable.CounterInputLayout_inputBackground, 0));
            }

            if (a.hasValue(R.styleable.CounterInputLayout_inputCounterTextSize)) {
                float counterTextSize = a.getDimension(R.styleable.CounterInputLayout_inputCounterTextSize, 0f);
                inputLengthHint.setTextSize(TypedValue.COMPLEX_UNIT_PX, counterTextSize);
                inputLength.setTextSize(TypedValue.COMPLEX_UNIT_PX, counterTextSize);
            }
            if (a.hasValue(R.styleable.CounterInputLayout_inputCounterTextColor)) {
                int counterTextColor = a.getColor(R.styleable.CounterInputLayout_inputCounterTextColor, 0);
                inputLengthHint.setTextColor(counterTextColor);
                inputLength.setTextColor(counterTextColor);
            }

            if (a.hasValue(R.styleable.CounterInputLayout_inputCounterTextSize)) {
                float contentTextSize = a.getDimension(R.styleable.CounterInputLayout_inputTextSize, 0f);
                contentInput.setTextSize(TypedValue.COMPLEX_UNIT_PX, contentTextSize);
            }

            if (a.hasValue(R.styleable.CounterInputLayout_inputTextColor)) {
                int inputTextColor = a.getColor(R.styleable.CounterInputLayout_inputTextColor, 0);
                contentInput.setTextColor(inputTextColor);
            }
            if (a.hasValue(R.styleable.CounterInputLayout_inputTextColorHint)) {
                int hintTextColor = a.getColor(R.styleable.CounterInputLayout_inputTextColorHint, 0);
                contentInput.setHintTextColor(hintTextColor);
            }
            if (a.hasValue(R.styleable.CounterInputLayout_inputPadding)) {
                int padding = (int) a.getDimension(R.styleable.CounterInputLayout_inputPadding, 0f);
                contentInput.setPadding(padding, padding, padding, padding);
                //hintLayout.setPadding(padding, padding, padding, padding);
            }
            if (a.hasValue(R.styleable.CounterInputLayout_inputCounterMargin)) {
                float margin = a.getDimension(R.styleable.CounterInputLayout_inputCounterMargin, -1);
                MarginLayoutParams marginLayoutParams = (MarginLayoutParams) hintLayout.getLayoutParams();
                marginLayoutParams.topMargin = (int) margin;
                marginLayoutParams.bottomMargin = (int) margin;
                marginLayoutParams.leftMargin = (int) margin;
                marginLayoutParams.rightMargin = (int) margin;
            }
            if (a.hasValue(R.styleable.CounterInputLayout_inputCounterTopMargin)) {
                float topMargin = a.getDimension(R.styleable.CounterInputLayout_inputCounterTopMargin, -1);
                MarginLayoutParams marginLayoutParams = (MarginLayoutParams) hintLayout.getLayoutParams();
                marginLayoutParams.topMargin = (int) topMargin;
            }
            if (a.hasValue(R.styleable.CounterInputLayout_inputCounterBottomMargin)) {
                float bottomMargin = a.getDimension(R.styleable.CounterInputLayout_inputCounterBottomMargin, -1);
                MarginLayoutParams marginLayoutParams = (MarginLayoutParams) hintLayout.getLayoutParams();
                marginLayoutParams.bottomMargin = (int) bottomMargin;
            }
            if (a.hasValue(R.styleable.CounterInputLayout_inputCounterLeftMargin)) {
                float leftMargin = a.getDimension(R.styleable.CounterInputLayout_inputCounterLeftMargin, -1);
                MarginLayoutParams marginLayoutParams = (MarginLayoutParams) hintLayout.getLayoutParams();
                marginLayoutParams.leftMargin = (int) leftMargin;
            }
            if (a.hasValue(R.styleable.CounterInputLayout_inputCounterRightMargin)) {
                float rightMargin = a.getDimension(R.styleable.CounterInputLayout_inputCounterRightMargin, -1);
                MarginLayoutParams marginLayoutParams = (MarginLayoutParams) hintLayout.getLayoutParams();
                marginLayoutParams.rightMargin = (int) rightMargin;
            }

            if (a.hasValue(R.styleable.CounterInputLayout_inputHorizontalPadding)) {
                int horizontalPadding = a.getDimensionPixelOffset(R.styleable.CounterInputLayout_inputHorizontalPadding, 0);
                contentInput.setPadding(horizontalPadding, contentInput.getPaddingTop(), horizontalPadding, contentInput.getPaddingBottom());
                hintLayout.setPadding(horizontalPadding, hintLayout.getPaddingTop(), horizontalPadding, hintLayout.getPaddingBottom());
            }
            if (a.hasValue(R.styleable.CounterInputLayout_inputVerticalPadding)) {
                int verticalPadding = a.getDimensionPixelOffset(R.styleable.CounterInputLayout_inputVerticalPadding, 0);
                contentInput.setPadding(contentInput.getPaddingLeft(), verticalPadding, contentInput.getPaddingRight(), verticalPadding);
                hintLayout.setPadding(hintLayout.getPaddingLeft(), verticalPadding, hintLayout.getPaddingRight(), verticalPadding);
            }

            if(a.hasValue(R.styleable.CounterInputLayout_inputBottomMargin)){
                ((MarginLayoutParams)contentInput.getLayoutParams() ).bottomMargin= a.getDimensionPixelOffset(R.styleable.CounterInputLayout_inputBottomMargin, 0);
            }
            if(a.hasValue(R.styleable.CounterInputLayout_inputTopMargin)){
                ((MarginLayoutParams)contentInput.getLayoutParams() ).topMargin= a.getDimensionPixelOffset(R.styleable.CounterInputLayout_inputTopMargin, 0);
            }
            if(a.hasValue(R.styleable.CounterInputLayout_inputRightMargin)){
                ((MarginLayoutParams)contentInput.getLayoutParams() ).rightMargin= a.getDimensionPixelOffset(R.styleable.CounterInputLayout_inputRightMargin, 0);
            }
            if(a.hasValue(R.styleable.CounterInputLayout_inputLeftMargin)){
                ((MarginLayoutParams)contentInput.getLayoutParams() ).leftMargin= a.getDimensionPixelOffset(R.styleable.CounterInputLayout_inputLeftMargin, 0);
            }
            if(a.hasValue(R.styleable.CounterInputLayout_inputMargin)){
                ((MarginLayoutParams)contentInput.getLayoutParams() ).setMargins(a.getDimensionPixelOffset(R.styleable.CounterInputLayout_inputMargin, 0),
                        a.getDimensionPixelOffset(R.styleable.CounterInputLayout_inputMargin, 0),
                        a.getDimensionPixelOffset(R.styleable.CounterInputLayout_inputMargin, 0),
                        a.getDimensionPixelOffset(R.styleable.CounterInputLayout_inputMargin, 0));
            }
            if (a.hasValue(R.styleable.CounterInputLayout_inputLineSpacingExtra)) {
                contentInput.setLineSpacing(
                        a.getDimensionPixelOffset(R.styleable.CounterInputLayout_inputLineSpacingExtra, 0),
                        1.0f
                );
            }

            contentBold = a.getBoolean(R.styleable.CounterInputLayout_inputTextBold, false);
            if (contentBold) {
                contentInput.setTypeface(null, Typeface.BOLD);
            }

            if (a.hasValue(R.styleable.CounterInputLayout_inputCounterGravity)) {
                int inputGravity = a.getInt(R.styleable.CounterInputLayout_inputCounterGravity, 0);
                int androidGravity = resolveGravity(inputGravity);
                if(hintLayout.getLayoutParams() instanceof LayoutParams params){
                    params.gravity=androidGravity;
                }
            }
            if (a.hasValue(R.styleable.CounterInputLayout_inputCounterVisible)) {
                boolean visible = a.getBoolean(R.styleable.CounterInputLayout_inputCounterVisible, true);
                hintLayout.setVisibility(visible ? VISIBLE : GONE);
            }
            a.recycle();
        }

        contentPaddingLeft = contentInput.getPaddingLeft();
        contentPaddingTop = contentInput.getPaddingTop();
        contentPaddingRight = contentInput.getPaddingRight();
        contentPaddingBottom = contentInput.getPaddingBottom();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (updateContentPaddingForCounter()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /**
     * 单行时让输入内容止于计数器左侧；多行时为右下角计数器预留一行空间。
     */
    private boolean updateContentPaddingForCounter() {
        int left = contentPaddingLeft;
        int top = contentPaddingTop;
        int right = contentPaddingRight;
        int bottom = contentPaddingBottom;

        if (hintLayout.getVisibility() != GONE) {
            MarginLayoutParams params = (MarginLayoutParams) hintLayout.getLayoutParams();
            int counterWidth = hintLayout.getMeasuredWidth() + params.leftMargin + params.rightMargin;
            int counterHeight = hintLayout.getMeasuredHeight() + params.topMargin + params.bottomMargin;

            if (contentInput.getMaxLines() == 1 || contentInput.getMaxLines() == 2) {
                right = Math.max(right, counterWidth);
            } else {
                bottom = Math.max(bottom, counterHeight);
            }
        }

        if (contentInput.getPaddingLeft() == left
                && contentInput.getPaddingTop() == top
                && contentInput.getPaddingRight() == right
                && contentInput.getPaddingBottom() == bottom) {
            return false;
        }
        contentInput.setPadding(left, top, right, bottom);
        return true;
    }


    public static int resolveGravity(int inputGravity) {
        int resolved = Gravity.NO_GRAVITY;

        // 使用明确的位标志检查
        if (hasFlag(inputGravity, 1)) resolved |= Gravity.LEFT;
        if (hasFlag(inputGravity, 2)) resolved |= Gravity.RIGHT;
        if (hasFlag(inputGravity, 4)) resolved |= Gravity.TOP;
        if (hasFlag(inputGravity, 8)) resolved |= Gravity.BOTTOM;
        if (hasFlag(inputGravity, 16)) resolved |= Gravity.CENTER_HORIZONTAL;
        if (hasFlag(inputGravity, 32)) resolved |= Gravity.CENTER_VERTICAL;

        // 设置默认值
        if ((resolved & Gravity.HORIZONTAL_GRAVITY_MASK) == 0) {
            resolved |= Gravity.RIGHT;
        }
        if ((resolved & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
            resolved |= Gravity.BOTTOM;
        }

        return resolved;
    }

    private static boolean hasFlag(int flags, int flag) {
        return (flags & flag) == flag;
    }

    public void addTextChangedListener(TextWatcher textWatcher) {
        contentInput.addTextChangedListener(textWatcher);
    }

    /**
     * 提示
     *
     * @param hint
     */
    public void setHint(String hint) {
        contentInput.setHint(hint);
    }

    /**
     * 行数
     *
     * @param lines
     */
    public void setLines(int lines) {
        contentInput.setLines(lines);
        contentInput.setMaxLines(lines);
    }

    public String getText() {
        return contentInput.getText().toString();
    }

    public Editable getEditableText() {
        return contentInput.getEditableText();
    }

    public EditText getInput() {
        return contentInput;
    }

    /**
     * 最大输入字数
     *
     * @param maxLength
     */
    public void setMaxLength(int maxLength) {
        contentInput.setMaxLength(maxLength);
        inputLength.setText("0");
        inputLengthHint.setText("/" + maxLength);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        //inputLengthHint.setText(s.toString().length() + "/" + maxLength);
        int length = s.toString().length();
        inputLength.setText(length + "");
//        inputLength.setTextColor(length > 0 ? getResources().getColor(R.color.textColorGray) : getResources().getColor(R.color.textColorSecondary));
//        inputLengthHint.setTextColor(length > 0 ? getResources().getColor(R.color.textColorGray) : getResources().getColor(R.color.textColorSecondary));
    }

    public void setText(CharSequence text) {
        contentInput.setText(text);
        contentInput.setSelection(contentInput.getText().length());
    }

    public void setTextNoSelection(CharSequence text) {
        contentInput.setText(text);
    }

    public void setInputEnable(boolean inputEnable) {
        contentInput.setEnabled(inputEnable);
        inputLength.setVisibility(inputEnable ? View.VISIBLE : View.INVISIBLE);
        inputLengthHint.setVisibility(inputEnable ? View.VISIBLE : View.INVISIBLE);
    }

    public void setOnTextOutOfBondsListener(XEditText.OnTextOutOfBondsListener onTextOutOfBondsListener) {
        this.contentInput.setOnTextOutOfBondsListener(onTextOutOfBondsListener);
    }

    public int getMaxLength() {
        return maxLength;
    }
}

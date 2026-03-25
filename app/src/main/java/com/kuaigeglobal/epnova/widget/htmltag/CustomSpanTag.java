package com.kuaigeglobal.epnova.widget.htmltag;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Parcel;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;

import org.xml.sax.Attributes;

import java.util.Stack;

public class CustomSpanTag extends BaseHtmlTag {
    public static final String SPAN = "span";
    private final Stack<Integer> spanStartIndexStack = new Stack<>();
    private final Stack<StashedSpanStyle> stashSpanStyleStack = new Stack<>();

    @Override
    public void startHandleTag(Editable originEditable, Attributes atts) {
        String style = atts.getValue("", STYLE);
        if (TextUtils.isEmpty(style)) {
            return;
        }
        final String textColorStr = getValueFromStyle(style, COLOR);
        final String fontSizeStr = getValueFromStyle(style, FONT_SIZE);
        final String backgroundColorStr = getValueFromStyle(style, BACKGROUND_COLOR);
        final String fontWeight = getValueFromStyle(style, FONT_WEIGHT);
        final int fontSize = getFontSize(fontSizeStr);
        boolean isFind = false;
        if (fontSize != -1) {
            //接收数据为px单位但因为iOS、Android使用单位不同且无法使用px实现适配，暂时采用15px当做15dp进行处理
            setSpanStartIndex(originEditable, new FontSize(fontSize));
            isFind = true;
        }
        final int textColor = parseColor(textColorStr);
        if (textColor != -1) {
            setSpanStartIndex(originEditable, new ForegroundColor(textColor));
            isFind = true;
        }
        final int backgroundColor = parseColor(backgroundColorStr);
        if (backgroundColor != -1) {
            setSpanStartIndex(originEditable, new BackgroundColor(backgroundColor));
            isFind = true;
        }
        if (fontWeight != null && fontWeight.toLowerCase().equals(BOLD)) {
            setSpanStartIndex(originEditable, new Bold());
            isFind = true;
        }
        if (isFind) {
            spanStartIndexStack.push(originEditable.length());
        }
    }

    private String getValueFromStyle(String style, String matchAttr) {
        if (TextUtils.isEmpty(style)) {
            return null;
        }
        return getHtmlCssAttrs(style, matchAttr);
    }

    private String getHtmlCssAttrs(@NonNull String style, String matchAttr) {
        if (TextUtils.isEmpty(style)) {
            return null;
        }
        String[] styleAttrs = style.trim().toLowerCase().split(";");
        for (String attr : styleAttrs) {
            attr = attr.trim();
            if (attr.indexOf(matchAttr) == 0) {
                String[] split = attr.split(":");
                if (split.length != 2) {
                    continue;
                }
                return split[1].trim();
            }
        }
        return null;
    }

    @Override
    public void endHandleTag(Editable originEditable) {
        Integer index = 0;
        if (!spanStartIndexStack.empty()) {
            index = spanStartIndexStack.pop();
            if (index == null) {
                index = 0;
            }
        }
        FontSize fontSizeSpan = getLastSpanFromEdit(index, originEditable, FontSize.class);
        if (fontSizeSpan != null) {
            tagSpans(originEditable, fontSizeSpan, new AbsoluteSizeSpan(fontSizeSpan.fontSize, true));
        }
        ForegroundColor foregroundColorSpan = getLastSpanFromEdit(index, originEditable, ForegroundColor.class);
        if (foregroundColorSpan != null) {
            tagSpans(originEditable, foregroundColorSpan, new ForegroundColorSpan(foregroundColorSpan.foregroundColor));
        }
        BackgroundColor backgroundColorSpan = getLastSpanFromEdit(index, originEditable, BackgroundColor.class);
        if (backgroundColorSpan != null) {
            tagSpans(originEditable, backgroundColorSpan, new BackgroundColorSpan(backgroundColorSpan.backgroundColor));
        }
        Bold boldSpans = getLastSpanFromEdit(index, originEditable, Bold.class);
        if (boldSpans != null) {
            tagSpans(originEditable, boldSpans, new CustomFontBoldSpan());
        }
    }

    @Override
    public void finishHandleTag(Editable originEditable) {
        while (!stashSpanStyleStack.empty()) {
            final StashedSpanStyle stashedSpanStyle = stashSpanStyleStack.pop();
            if (stashedSpanStyle == null) {
                continue;
            }
            originEditable.setSpan(stashedSpanStyle.span, stashedSpanStyle.start, stashedSpanStyle.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * 标记span样式的起点位置
     *
     * @param editable
     * @param mark
     */
    private void setSpanStartIndex(Editable editable, Object mark) {
        // startHandle阶段 setSpan只做标记位置作用不实现具体效果
        int length = editable.length();
        editable.setSpan(mark, length, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    /**
     * 根据起点终点保存span样式
     *
     * @param editable
     * @param mark
     * @param spans
     */
    private void tagSpans(Editable editable, Object mark, Object... spans) {
        int start = editable.getSpanStart(mark);
        editable.removeSpan(mark);
        int end = editable.length();
        if (start != end) {
            for (Object span : spans) {
                stashSpanStyleStack.push(new StashedSpanStyle(span, start, end));
            }
        }
    }

    private static class StashedSpanStyle {
        Object span;
        int start;
        int end;

        public StashedSpanStyle(Object span, int start, int end) {
            this.span = span;
            this.start = start;
            this.end = end;
        }
    }

    private static class Bold {

    }

    private static class FontSize {
        int fontSize;

        public FontSize(int fontSize) {
            this.fontSize = fontSize;
        }
    }

    private static class BackgroundColor {
        int backgroundColor;

        public BackgroundColor(int backgroundColor) {
            this.backgroundColor = backgroundColor;
        }
    }

    private static class ForegroundColor {
        int foregroundColor;

        public ForegroundColor(int foregroundColor) {
            this.foregroundColor = foregroundColor;
        }
    }

    private static class CustomFontBoldSpan extends StyleSpan {

        public CustomFontBoldSpan() {
            super(Typeface.BOLD);
        }

        public CustomFontBoldSpan(@NonNull Parcel src) {
            super(src);
        }

        @Override
        public void updateDrawState(TextPaint tp) {
            tp.setStrokeWidth(1.0F);
            tp.setStyle(Paint.Style.FILL_AND_STROKE);
        }
    }
}
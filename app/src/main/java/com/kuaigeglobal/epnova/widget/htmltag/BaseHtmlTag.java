package com.kuaigeglobal.epnova.widget.htmltag;

import android.graphics.Color;
import android.text.Editable;
import android.text.TextUtils;

import org.xml.sax.Attributes;

public abstract class BaseHtmlTag {
    private static final String UNIT_PX = "px";
    protected static final String FONT_SIZE = "font-size";
    protected static final String COLOR = "color";
    protected static final String BACKGROUND_COLOR = "background-color";
    protected static final String FONT_WEIGHT = "font-weight";
    protected static final String BOLD = "bold";
    protected static final String STYLE = "style";

    /**
     * 处理头标签<AAA>
     *
     * @param originEditable
     * @param atts
     */
    public abstract void startHandleTag(Editable originEditable, Attributes atts);

    /**
     * 处理尾标签</AAA>
     *
     * @param originEditable
     */
    public abstract void endHandleTag(Editable originEditable);

    /**
     * </custom>标签结束处理
     *
     * @param originEditable
     */
    public abstract void finishHandleTag(Editable originEditable);

    public int getFontSize(String fontSize) {
        if (TextUtils.isEmpty(fontSize)) {
            return -1;
        }
        fontSize = fontSize.toLowerCase();
        //单位px
        if (fontSize.endsWith(UNIT_PX) && TextUtils.isDigitsOnly(fontSize.substring(0, fontSize.indexOf(UNIT_PX)))) {
            return (int) Float.parseFloat(fontSize.substring(0, fontSize.indexOf(UNIT_PX)));
        }
        //无单位
        if (TextUtils.isDigitsOnly(fontSize)) {
            return (int) Float.parseFloat(fontSize);
        }
        return -1;
    }

    /**
     * 重写Color.parseColor 不希望出现Exception
     *
     * @param colorString
     * @return
     */
    public int parseColor(String colorString) {
        if (TextUtils.isEmpty(colorString)) {
            return -1;
        }
        try {
            return Color.parseColor(colorString);
        } catch (IllegalArgumentException ex) {
            return -1;
        }
    }

    /**
     * 获取editable中已经存在的span集合，获取最新添加的span
     *
     * @param start 匹配查询起点
     * @param editable
     * @param kind
     */
    public static <T> T getLastSpanFromEdit(int start, Editable editable, Class<T> kind) {
        T[] objs = editable.getSpans(start, editable.length(), kind);
        if (objs.length == 0) {
            return null;
        } else {
            return objs[objs.length - 1];
        }
    }
}
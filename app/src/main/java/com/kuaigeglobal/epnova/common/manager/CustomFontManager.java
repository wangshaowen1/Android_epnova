package com.kuaigeglobal.epnova.common.manager;


import android.app.Application;
import android.graphics.Typeface;

/**
 * 字体工具类
 */
public class CustomFontManager {
    private static CustomFontManager singleton;
    private Typeface lightFont;
    private Typeface boldFont;

    private CustomFontManager() {

    }

    public void init(final Application context) {
        //读取字体文件
        this.lightFont = Typeface.createFromAsset(context.getAssets(), "fonts/SFCompactText-Light.otf");
        this.boldFont = Typeface.createFromAsset(context.getAssets(), "fonts/SFCompactText-Semibold.otf");
    }

    public synchronized static CustomFontManager getInstance() {
        if (singleton == null) {
            singleton = new CustomFontManager();
        }
        return singleton;
    }

    public Typeface getLightFont() {
        return this.lightFont;
    }

    public Typeface getBoldFont() {
        return this.boldFont;
    }
}
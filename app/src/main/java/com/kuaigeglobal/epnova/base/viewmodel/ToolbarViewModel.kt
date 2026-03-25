package com.kuaigeglobal.epnova.base.viewmodel

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import androidx.databinding.ObservableField
import androidx.databinding.ObservableFloat
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getDrawable
import com.nhsd.basic.ext.getResColor
import com.nhsd.crop.util.ViewUtils

class ToolbarViewModel(
    var navigationText: String? = "",
    var navigationDrawable: Drawable? = R.drawable.icon_back_left.getDrawable(),
    var navigationAction: (() -> Unit)? = null,
    var title: String? = "",
    var navigationColor: Int = R.color.text_color_1_dark.getResColor(),
    var titleColor: Int = R.color.text_color_1_dark.getResColor(),
    var paddingTop: Int = ViewUtils.getStatusBarHeight(App.instance),
    var menuText: String? = "",
    var menuTextSize: Int = 14.dp,
    var menuButtonText: String? = null,
    var menuIconRes: Int? = null,
    var menuTextDrawableEnd: Drawable? = null,
    var menuTextDrawablePadding: Int = 5.dp,
    var menuTextDrawableStart: Drawable? = null,
    var menuEnable: Boolean? = true,
    var menuButtonBackground: Drawable? = R.drawable.selector_common_button.getDrawable(),
    var menuButtonDrawableEnd: Drawable? = null,
    var menuButtonDrawableStart: Drawable? = null,
    var menuButtonDrawablePadding: Int = 5.dp,
    var menuAction: ((view: View) -> Unit)? = null,
    var background: Int = Color.TRANSPARENT,
    var menuTextHorPadding: Int = 0,
    var menuTextVerPadding: Int = 0,
    var menuButtonHorPadding: Int = 12.dp,
    var menuButtonVerPadding: Int = 5.dp,
    var menuTextColorRes: Int = R.color.text_color_3_dark,
    var menuButtonTextColor: Int = Color.WHITE,
    var menuIconMarginEnd: Int = 12.dp,
    var menuButtonMarginEnd: Int = 8.dp,
    var menuTextMarginEnd: Int = 12.dp,
    var menuButtonTextAllCaps: Boolean = false
) {
    val mTitle = ObservableField(title)
    val mTitleAlpha = ObservableFloat(1f)
    val mBackground = ObservableField(background)
    val mMenuText = ObservableField(menuText)
    val mMenuTextSize = ObservableField(menuTextSize)
    val mMenuTextColor = ObservableField(menuTextColorRes)
    var mNavigationText = ObservableField(navigationText)
    val mMenuButtonText = ObservableField(menuButtonText)
    val mMenuIconRes = ObservableField<Int>(menuIconRes)
    val mMenuButtonBackground = ObservableField(menuButtonBackground)
    val mMenuDrawableStart = ObservableField(menuTextDrawableStart)
    val mMenuDrawableEnd = ObservableField(menuTextDrawableEnd)
    val mMenuButtonDrawableStart = ObservableField(menuButtonDrawableStart)
    val mMenuButtonDrawableEnd = ObservableField(menuButtonDrawableEnd)
    val mMenuEnable = ObservableField(menuEnable)
    val mMenuButtonTextAllCaps = ObservableField(menuButtonTextAllCaps)
}
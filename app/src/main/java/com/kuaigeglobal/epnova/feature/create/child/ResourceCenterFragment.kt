package com.kuaigeglobal.epnova.feature.create.child

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.databinding.FragmentCreateBinding
import com.kuaigeglobal.epnova.feature.creator.CreationRootActivity
import com.kuaigeglobal.epnova.router.AppNavigator
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.network.apis.entity.BannerInfo
import com.kuaigeglobal.epnova.databinding.FragmentResourceCenterBinding
import com.kuaigeglobal.epnova.feature.create.CreateViewModel
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getThemeColor
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.crop.util.ViewUtils
import com.nhsd.magicindicator.ViewPagerHelper
import com.nhsd.magicindicator.buildins.commonnavigator.CommonNavigator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import com.nhsd.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import com.zhpan.indicator.enums.IndicatorStyle
import kotlin.math.abs

class ResourceCenterFragment : BaseViewModelFragment<FragmentResourceCenterBinding, ResourceCenterViewModel>() {

}
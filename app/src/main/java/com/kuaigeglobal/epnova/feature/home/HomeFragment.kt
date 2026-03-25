package com.kuaigeglobal.epnova.feature.home

import android.view.View
import android.widget.ImageView
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.databinding.loadGifUrl
import com.kuaigeglobal.epnova.core.databinding.loadUri
import com.kuaigeglobal.epnova.core.manager.AppEvent
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.network.apis.entity.BannerInfo
import com.kuaigeglobal.epnova.core.network.apis.response.ActivityConfigData
import com.kuaigeglobal.epnova.core.network.apis.response.FloatWindowInfo
import com.kuaigeglobal.epnova.core.network.apis.response.PopupInfo
import com.kuaigeglobal.epnova.databinding.FragmentHomeBinding
import com.kuaigeglobal.epnova.feature.creator.CreationRootActivity
import com.kuaigeglobal.epnova.feature.main.ActivityDialog
import com.kuaigeglobal.epnova.feature.main.MainViewModel
import com.kuaigeglobal.epnova.router.AppNavigator
import com.kuaigeglobal.epnova.router.AppRouter
import com.kuaigeglobal.epnova.widget.decoration.StaggeredGridSpaceItemDecoration
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.utils.Logger
import com.nhsd.crop.util.ViewUtils
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import com.zhpan.indicator.enums.IndicatorStyle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class HomeFragment :
    BaseViewModelFragment<FragmentHomeBinding, HomeViewModel>() {
    companion object {
        fun newInstance(): HomeFragment {
            val fragment = HomeFragment()
            return fragment
        }
    }

    private val homeAdapter by lazy {
        HomeAdapter()
    }
    private lateinit var bannerAdapter: BaseBannerAdapter<BannerInfo>
    private val parentVm by lazy {
        getViewModelOfActivity<MainViewModel>()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        binding.llTop.minimumHeight = ViewUtils.getStatusBarHeight(activity)
        binding.appbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange.toFloat()
            val ratio = abs(verticalOffset) / totalScrollRange
            val alpha = if (ratio < 0.95f) 1f - ratio / 0.95f else 0f
            binding.llTop.alpha = alpha
        }
        bannerAdapter = object : BaseBannerAdapter<BannerInfo>() {
            override fun bindData(
                holder: BaseViewHolder<BannerInfo>?, data: BannerInfo, position: Int, pageSize: Int
            ) {
                holder?.apply {
                    val image = findViewById<ImageView>(R.id.image)
                    loadUri(image, data.imageUrl, R.drawable.img_default_cover_16_9.getResDrawable())
                    image.setOnSingleClickListener {
                        AppRouter.jump(data.jumpUri)
                    }
                }
            }

            override fun getLayoutId(viewType: Int): Int {
                return R.layout.item_message_banner
            }
        }
        binding.banner.setAdapter(bannerAdapter)
            .setLifecycleRegistry(lifecycle)
            .setAutoPlay(true)
            .setIndicatorSliderRadius(2f.dp)
            .setIndicatorSliderGap(4.dp)
            .setIndicatorHeight(4.dp)
            .setIndicatorSliderWidth(4.dp, 16.dp)
            .setIndicatorSliderColor(
                R.color.color_60_ffffff.getResColor(), R.color.white.getResColor()
            ).setIndicatorStyle(IndicatorStyle.ROUND_RECT)
            .setIndicatorMargin(0, 0, 0, 4.dp)
            .setIndicatorView(binding.ivIndicator)
            .setIndicatorVisibility(View.VISIBLE)
            .create()

        binding.recyclerView.apply {
            isFocusable = false
            addItemDecoration(StaggeredGridSpaceItemDecoration(2, 10.dp, 0, 0))
            adapter = homeAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = homeAdapter,
                    noDataButtonDisplay = false,
                    requestButtonDisplay = false,
                    displayedHeight = 400.dp,
                    refresh = {
                        homeAdapter.refresh()
                    },
                    requestButtonTextAndImg = false,
                )
            )
            doOnLayout {
                scrollToPosition(0)
            }
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = homeAdapter
        )
        binding.lsv.apply {
            makeScrollable()
            setRetryListener {
                if (mRealVM.bannerData.value.isNullOrEmpty()) {
                    mRealVM.getData()
                }
                homeAdapter.refresh()
            }
        }

        binding.btnCreation.setOnSingleClickListener {
            //if (AppNavigator.checkLoginIntercepted()) return@setOnSingleClickListener
            CreationRootActivity.start(activity)
        }
    }

    fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.feedFlow.collectLatest { pagingData ->
                homeAdapter.submitData(pagingData)
            }
        }

        mRealVM.bannerData.observe(this) {
            binding.banner.refreshData(it)
        }

        parentVm?.activityConfigCache?.observe(this) { config ->
            // 悬浮窗：始终显示
            handleFloatWindow(config.getFloatWindowInfo(ActivityConfigData.TAB_HOME))
            // 弹窗逻辑
            val popup = config.getPopupInfo(ActivityConfigData.TAB_HOME)
            // 关键拦截：只有首充没弹，才处理活动弹窗
            if (parentVm?.isFirstRechargeLogicActive == true) {
                Logger.d("Dialog", "拦截活动弹窗，等待下次回到首页再弹")
            } else {
                handlePopup(popup)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isFragmentVisible) {
            resumePopup()
        }
    }

    private var isFragmentVisible = false // 记录当前 Tab 是否是选中状态

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        isFragmentVisible = !hidden
        if (isFragmentVisible) {
            resumePopup()
        }
    }

    fun resumePopup(){
        if (parentVm?.isFirstRechargeLogicActive == false && parentVm?.hasActivityConfig == true) {
            handlePopup(parentVm?.activityConfigCache?.value?.getPopupInfo(ActivityConfigData.TAB_HOME))
            parentVm?.hasActivityConfig = false
        }
    }

    private fun handlePopup(popup: PopupInfo?) {
        popup?.let {
            if (popup.isShow()) {
                if (popup.businessType == 1) {
                    EventManager.onAppTrack(AppEvent.SF_SHOW_COURSE_ENROLL_POPUP)
                }
                ActivityDialog.show(popup)
                Config.get().activityPopupShowMark(popup.id)
            }
        }
    }

    private fun handleFloatWindow(float: FloatWindowInfo?) {
        if (float != null && float.isShow()) {
            binding.floatingBall.isVisible = true
            loadGifUrl(binding.ivFloating, float.imageUrl, R.drawable.img_default_cover_1_1.getResDrawable())
            Config.get().activityFloatShowMark(float.id)
            binding.floatingBall.setOnMfClickListener {
                AppRouter.jump(float.jumpUrl)
            }
        } else {
            binding.floatingBall.visibility = View.GONE
        }
    }
}
package com.kuaigeglobal.epnova.feature.home

import android.view.View
import android.widget.ImageView
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.core.databinding.loadUri
import com.kuaigeglobal.epnova.core.network.apis.entity.BannerInfo
import com.kuaigeglobal.epnova.databinding.FragmentHomeBinding
import com.kuaigeglobal.epnova.feature.home.search.SearchActivity
import com.kuaigeglobal.epnova.feature.main.MainViewModel
import com.kuaigeglobal.epnova.router.AppRouter
import com.kuaigeglobal.epnova.widget.decoration.StaggeredGridSpaceItemDecoration
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.setOnSingleClickListener
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
    private val titleAdapter by lazy { WaterfallTitleAdapter() }
    private val shelfContainerAdapter by lazy { ShelfContainerAdapter() }
    private lateinit var bannerAdapter: BaseBannerAdapter<BannerInfo>
    private val parentVm by lazy {
        getViewModelOfActivity<MainViewModel>()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
        initListener()
    }

    private fun initListener() {
        binding.btnSearch.setOnSingleClickListener {
            SearchActivity.start(mActivity)
        }
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
            val waterfallWithFooter = homeAdapter.withLoadStateFooter(
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
            val concatAdapter = ConcatAdapter(shelfContainerAdapter, titleAdapter, waterfallWithFooter)
            adapter = concatAdapter
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
                if (mRealVM.bookShelfData.value == null) {
                    mRealVM.loadData()
                }
                homeAdapter.refresh()
            }
        }
    }

    fun dataUpdate() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mRealVM.feedFlow.collectLatest { pagingData ->
                    homeAdapter.submitData(pagingData)
                }
            }
        }

        mRealVM.bannerData.observe(this) {
            binding.banner.refreshData(it)
        }

        mRealVM.bookShelfData.observe(this) { data ->
            // 安全处理：如果 data 或 sections 为 null，给一个默认的空列表
            val sections = data?.sections ?: emptyList()

            // 1. 横滑书架：
            // 如果后端没下发 type == 1，filter 出来的就是空列表
            // ListAdapter 收到 emptyList() 后会自动把书架模块的高度变为 0
            val horizontalShelves = sections.filter { it.bookshelfType == 1 }
            shelfContainerAdapter.submitList(horizontalShelves)

            // 2. 瀑布流：
            // 寻找 type == 2 的配置
            val waterfallSection = sections.find { it.bookshelfType == 2 }
            titleAdapter.isVisible = waterfallSection != null
            // 如果没找到，这里会传 "" (空字符串)
            // ViewModel 监听到空字符串，就会通过 flowOf(PagingData.empty()) 瞬间清空底部的瀑布流
            mRealVM.currentWaterfallId.value = waterfallSection?.bookshelfId ?: ""
        }
    }
}
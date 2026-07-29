package com.kuaigeglobal.epnova.feature.creation.childs

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.adapter.QuickAdapter
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.ext.onUserSignOut
import com.kuaigeglobal.epnova.core.network.apis.entity.InspirationInfo
import com.kuaigeglobal.epnova.databinding.FragmentCreationInspirationBinding
import com.kuaigeglobal.epnova.databinding.LayoutCreationTabHeaderBinding
import com.kuaigeglobal.epnova.feature.creation.CreationFragment.Companion.KEY_MODEL
import com.kuaigeglobal.epnova.feature.creation.CreationFragment.Companion.MODEL_INSPIRATION
import com.kuaigeglobal.epnova.feature.creator.CreationHotCaseStateAdapter
import com.kuaigeglobal.epnova.feature.creator.CreationPageActionDelegate
import com.kuaigeglobal.epnova.feature.creator.CreationTabHeaderAdapter
import com.kuaigeglobal.epnova.feature.creator.CreationViewModel
import com.kuaigeglobal.epnova.feature.creator.ItemInspirationPromptVM
import com.kuaigeglobal.epnova.feature.creator.hotcase.CreationHotCaseEvent
import com.kuaigeglobal.epnova.feature.creator.hotcase.CreationHotCaseInfo
import com.kuaigeglobal.epnova.feature.creator.hotcase.CreationHotCasePagingAdapter
import com.kuaigeglobal.epnova.feature.creator.set.VideoSetActivity
import com.kuaigeglobal.epnova.router.AppNavigator
import com.kuaigeglobal.epnova.widget.decoration.BottomSpaceDecoration
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.item.BaseItemEventsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CreationInspirationFragment :
    BaseViewModelFragment<FragmentCreationInspirationBinding, CreationViewModel>() {

    companion object {
        fun newInstance() = CreationInspirationFragment().apply {
            arguments = Bundle().apply { putInt(KEY_MODEL, MODEL_INSPIRATION) }
        }
    }

    private var hotCaseRefreshState: LoadState = LoadState.Loading
    private var hotCaseObserverRegistered = false
    private val hotCaseEventVm by lazy {
        BaseItemEventsViewModel<CreationHotCaseEvent, CreationHotCaseInfo>()
    }
    private val pageDelegate by lazy {
        CreationPageActionDelegate(
            fragment = this,
            viewModel = mRealVM,
            isInspirationPage = true,
            inputViewProvider = { headerAdapter.binding?.inputView },
            onInputChanged = ::updateInputActions
        )
    }
    private val hotCaseLoadStateListener: (CombinedLoadStates) -> Unit = { states ->
        hotCaseRefreshState = states.refresh
        updateHotCaseState()
    }
    private val hotCaseDataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() = updateHotCaseState()
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = updateHotCaseState()
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = updateHotCaseState()
    }
    private val hotCaseAdapter by lazy { CreationHotCasePagingAdapter(hotCaseEventVm) }
    private val headerAdapter by lazy {
        CreationTabHeaderAdapter(mRealVM, viewLifecycleOwner, ::initHeaderView)
    }
    private val inspirationPromptAdapter by lazy {
        QuickAdapter<ItemInspirationPromptVM>(R.layout.item_inspiration_prompt, emptyList())
    }
    private val hotCaseStateAdapter by lazy {
        CreationHotCaseStateAdapter { hotCaseAdapter.refresh() }
    }
    private val hotCaseListAdapter: RecyclerView.Adapter<out RecyclerView.ViewHolder> by lazy {
        hotCaseAdapter.withLoadStateFooter(
            MyMultiStatePagingFooterAdapter(
                hostAdapter = hotCaseAdapter,
                noMoreThreshold = 4
            ) { hotCaseAdapter.retry() }
        )
    }
    private val creationConcatAdapter by lazy {
        ConcatAdapter(headerAdapter, hotCaseStateAdapter, hotCaseListAdapter)
    }

    override fun onPrepareView() {
        super.onPrepareView()
        mRealVM.enableCreationTabUi()
        initHotCaseList()
        observeData()
    }

    private fun initHeaderView(headerBinding: LayoutCreationTabHeaderBinding) {
        pageDelegate.bindInput(headerBinding.inputView)
        headerBinding.lyricPrompts.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = inspirationPromptAdapter
            itemAnimator = null
        }
        submitInspirationPrompts(mRealVM.inspirationPrompts.value)
        updateInputActions()
    }

    private fun initHotCaseList() {
        binding.rvHotCases.apply {
            addItemDecoration(BottomSpaceDecoration(20.dp))
            isFocusable = false
            itemAnimator = null
            hotCaseAdapter.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            adapter = creationConcatAdapter
        }
        hotCaseAdapter.addLoadStateListener(hotCaseLoadStateListener)
        if (!hotCaseObserverRegistered) {
            hotCaseAdapter.registerAdapterDataObserver(hotCaseDataObserver)
            hotCaseObserverRegistered = true
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mRealVM.creationHotCaseFlow.collectLatest { pagingData ->
                    // 加载创作同款列表
                    hotCaseAdapter.submitData(pagingData)
                }
            }
        }
    }

    private fun observeData() {
        pageDelegate.observeCommon(viewLifecycleOwner)
        mRealVM.inspirationPrompts.observe(viewLifecycleOwner, ::submitInspirationPrompts)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                hotCaseEventVm.events.collect { event ->
                    when (event) {
                        is CreationHotCaseEvent.CreateSame -> {
                            event.item.data.get()?.let { info ->
                                onCreateSame(info)
                            }
                        }
                    }
                }
            }
        }
        observeEvents {
            onUserSignOut { mRealVM.input.value = "" }
            on<Events.ScriptContentGenerateSuccessEvent> {
                if (it.type == Events.ScriptContentGenerateSuccessEvent.TYPE_INSPIRATION) {
                    mRealVM.input.value = ""
                }
            }
        }
    }

    private fun submitInspirationPrompts(prompts: List<InspirationInfo>?) {
        inspirationPromptAdapter.submitList(
            prompts.orEmpty()
                .filter { !it.configCode.isNullOrBlank() }
                .map { info ->
                    ItemInspirationPromptVM(info) { prompt ->
                        if (!mRealVM.isStreaming()) mRealVM.selectInspirationPrompt(prompt)
                    }
                }
        )
        headerAdapter.binding?.lyricPrompts?.scrollToPosition(0)
    }

    private fun updateInputActions() {
        val header = headerAdapter.binding ?: return
        val hasInput = !mRealVM.input.value.isNullOrEmpty()
        header.btnRandomPrompt.isVisible = !hasInput
        header.btnOptimize.isVisible = hasInput
        header.btnStoryUpload.isVisible = false
        header.btnPaste.isVisible = false
        header.btnDelete.isVisible = hasInput
    }

    private fun updateHotCaseState() {
        hotCaseStateAdapter.update(hotCaseRefreshState, hotCaseAdapter.itemCount == 0)
    }

    private fun onCreateSame(info: CreationHotCaseInfo) {
        if (mRealVM.isStreaming()) {
            return
        }
        if (AppNavigator.checkLoginIntercepted(true)) {
            return
        }
        mRealVM.sameStylePrefill(info.id) { prefill ->
            val sameStory = prefill?.inspirationContent
            if (sameStory.isNullOrBlank()) {
                getString(R.string.page_load_error).showToast()
                return@sameStylePrefill
            }
            val workTitle = prefill?.workTitle?.takeIf { it.isNotBlank() }
            mRealVM.videoSetConfig(true) {
                showPackageDialogForHotCase(workTitle, sameStory, info.id, info.title)
            }
        }
    }

    private fun showPackageDialogForHotCase(workTitle: String?, sameStory: String, hotCaseId: String?, hotCaseTitle: String?) {
        //
        VideoSetActivity.start(
            context = context,
            workTitle = workTitle,
            sameStory = sameStory,
            list = mRealVM.videoSetConfig.value,
            hotCaseId = hotCaseId,
            hotCaseTitle = hotCaseTitle
//            creationPackage = packageInfo
        )
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnOptimize -> pageDelegate.optimizeInspiration()
            R.id.randomInspirations -> mRealVM.randomInspirations(true)
            R.id.btnRandomPrompt -> pageDelegate.randomPrompt()
            R.id.btnDelete -> pageDelegate.deleteInput()
            R.id.btnNext -> pageDelegate.nextStep()
        }
    }

    override fun onDestroy() {
        hotCaseAdapter.removeLoadStateListener(hotCaseLoadStateListener)
        if (hotCaseObserverRegistered) {
            hotCaseAdapter.unregisterAdapterDataObserver(hotCaseDataObserver)
            hotCaseObserverRegistered = false
        }
        binding.rvHotCases.adapter = null
        headerAdapter.clearBinding()
        pageDelegate.clear()
        super.onDestroy()
    }
}

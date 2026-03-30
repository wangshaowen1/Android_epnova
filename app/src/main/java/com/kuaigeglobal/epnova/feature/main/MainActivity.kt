package com.kuaigeglobal.epnova.feature.main

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SparseArray
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.core.util.getOrDefault
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.permissions.requestRxPermissions
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.manager.AppEvent
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.UpgradeManager
import com.kuaigeglobal.epnova.databinding.ActivityMainBinding
import com.kuaigeglobal.epnova.feature.aicompose.generating.GeneratingViewModel
import com.kuaigeglobal.epnova.feature.creator.CreationRootActivity
import com.kuaigeglobal.epnova.feature.home.HomeFragment
import com.kuaigeglobal.epnova.feature.mine.MineFragment
import com.kuaigeglobal.epnova.feature.splash.newer.FirstRechargeDialog
import com.kuaigeglobal.epnova.router.AppNavigator
import com.kuaigeglobal.epnova.router.AppRouter
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.getThemeColor
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.ext.setupTranslucentStatus
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.ext.switchFragment
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2025/4/17  9:25
 * @Version: 1.0
 * *****************************************
 */
class MainActivity : BaseViewModelActivity<ActivityMainBinding, MainViewModel>() {
    companion object {
        val clicks = SparseArray<Long>()
        const val CLICK_INTERVAL_TIME: Long = 500
        private const val KEY_SCHEME = "KEY_SCHEME"
        const val FROM_NEWER = "Newer"

        fun start(activity: Activity?, scheme: String?) {
            if (activity == null) return
            val intent = Intent(activity, MainActivity::class.java)
            scheme?.let {
                intent.putExtra(KEY_SCHEME, scheme)
            }
            activity.startActivity(intent)
        }

        fun startFromNewer(activity: Activity?) {
            if (activity == null) return
            val intent = Intent(activity, MainActivity::class.java)
            intent.putExtra(Constants.Extra.FROM_PAGE, FROM_NEWER)
            activity.startActivity(intent)
        }
    }

    private var exitTime: Long = 0
    private var needRefreshUserInfo: Boolean = false
    private val upgradeManager by lazy { UpgradeManager(this) }

    //当前用户id
    private var userId: String? = null

    //当前打开的时间，用与跟注册时间对比来判断这一次登录的是否是刚注册的账号
    private var registerTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerTime = System.currentTimeMillis()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                dalvik.system.ZipPathValidator.clearCallback()
            } catch (e: Exception) {
            }
        }
        userId = User.get().id
        //初始化
        onAgreePolicy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (User.get().requestNotificationsPermissionEnable) {
                binding.root.postDelayed({
                    requestRxPermissions(
                        R.string.request_notification_permission_reason.getResString(),
                        POST_NOTIFICATIONS
                    ) { granted ->
                        if (!granted) {
                            User.get().setRequestNotificationsPermissionDisable()
                        }
                    }
                }, 1200)
            }
        }
        mRealVM.startCreating.observe(this) {
            if (it) {
                toHome()
                mRealVM.startCreating.value = false
            }
        }
    }


    /**
     * 遍历查询是否初始化完成
     */
    private fun waitApiSecurityInit() {
        MainScope().launchNoCrash {
            while (!App.instance.isApiSecurityInit) {
                delay(100)
            }
            initialization()
            mRealVM.initialization()
        }
    }

    /**
     * 初始化
     */
    private fun initialization() {
        observeEvents()
        initView()
        //App检测更新
        checkUpgrade()
        handleIntent(intent)
    }

    private fun initView() {
        binding.mrg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbHome -> {
                    //首页
                    EventManager.onAppTrack(AppEvent.SF_VIEW_HOME_PAGE)
                    setKeepScreenOn(false)
                    mRealVM.showTab.set(0)
                    supportFragmentManager.switchFragment(
                        R.id.fragment,
                        HomeFragment::class.java
                    )
                    lifecycleScope.launch {
                        delay(100)
                        displayPopUpBox(MainBulletFrameEnum.COMPLETE_PERSONAL_INFO)
                    }
                }

                R.id.rbWorks -> {
                    //热门
                    EventManager.onAppTrack(AppEvent.SF_VIEW_WORKS_TAB_PAGE)
                    setKeepScreenOn(false)
                    mRealVM.showTab.set(1)
                    CreationRootActivity.start(this)
                }


                R.id.rbMine -> {
                    //我的
                    EventManager.onAppTrack(AppEvent.SF_VIEW_MINE_TAB_PAGE)
                    setKeepScreenOn(false)
                    mRealVM.showTab.set(2)
                    supportFragmentManager.switchFragment(R.id.fragment, MineFragment::class.java)
                }
            }
        }
        when (Config.get().appConfig.openAppLoadTab) {
            0 -> binding.mrg.check(R.id.rbHome)
            1 -> binding.mrg.check(R.id.rbWorks)
            2 -> binding.mrg.check(R.id.rbMine)
            else -> binding.mrg.check(R.id.rbHome)
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (App.instance.hasAgree()) {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.hasExtra(KEY_SCHEME)) {
            val schemeUri = intent.getStringExtra(KEY_SCHEME)
            AppRouter.jump(schemeUri)
        }
    }

    override fun onStart() {
        super.onStart()
        if (needRefreshUserInfo) {
            mRealVM.fetchUserInfo()
            needRefreshUserInfo = false
        }
    }

    override fun onPause() {
        super.onPause()
        GeneratingViewModel.visible = false
    }

    override fun onResume() {
        super.onResume()
        GeneratingViewModel.visible = false
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.tabHome -> {
                clickTab(
                    tabId = R.id.rbHome,
                    null
                ) {
                    binding.mrg.check(R.id.rbHome) // 真正执行切换
                }
//                binding.mrg.check(R.id.rbHome)
//                clickTab(tabId = R.id.tabHome) { diff ->
//                    if (diff < CLICK_INTERVAL_TIME) {
//                        FlowEventBus.postSync(Events.MainTabDoubleClickEvent(Events.MainTabDoubleClickEvent.TAB_HOME))
//                        true
//                    } else false
//                }
            }

            R.id.tabWorks -> {
                clickTab(
                    tabId = R.id.rbWorks,
                    interceptor = { AppNavigator.checkLoginIntercepted(true) } // 传入拦截逻辑
                ) {
                    CreationRootActivity.start(this)
                    //binding.mrg.check(R.id.rbWorks) // 真正执行切换
                }
//                if (AppNavigator.checkLoginIntercepted(true)) {
//                    return
//                }
//                binding.mrg.check(R.id.rbWorks)
//                clickTab(tabId = R.id.tabWorks) { diff ->
//                    if (diff < CLICK_INTERVAL_TIME) {
//                        FlowEventBus.postSync(Events.MainTabDoubleClickEvent(Events.MainTabDoubleClickEvent.TAB_WORKS))
//                        true
//                    } else false
//                }
            }

            R.id.tabMine -> {
                clickTab(
                    tabId = R.id.tabMine,
                    interceptor = { AppNavigator.checkLoginIntercepted(true) } // 传入拦截逻辑
                ) {
                    binding.mrg.check(R.id.rbMine) // 真正执行切换
                }
//                if (AppNavigator.checkLoginIntercepted(true)) {
//                    return
//                }
//                clickTab(tabId = R.id.tabMine) { diff ->
//                    if (diff < CLICK_INTERVAL_TIME) {
//                        FlowEventBus.postSync(Events.MainTabDoubleClickEvent(Events.MainTabDoubleClickEvent.TAB_MINE))
//                        true
//                    } else false
//                }
//                binding.mrg.check(R.id.rbMine)
            }
        }
    }

    private fun clickTab(tabId: Int, clickAction: (diff: Long) -> Boolean) {
        val current = System.currentTimeMillis()

        if (!clickAction(current - clicks.getOrDefault(tabId, 0))) {
            clicks.put(tabId, current)
        }
    }

    /**
     * @param tabId 对应 R.id
     * @param interceptor 拦截逻辑，返回 true 表示被拦截（不更新时间戳，不执行 action）
     * @param action 正常点击逻辑
     */
    private fun clickTab(
        tabId: Int,
        interceptor: (() -> Boolean)? = null,
        action: () -> Unit
    ) {
        val current = System.currentTimeMillis()
        val lastClick = clicks.getOrDefault(tabId, 0L)
        val diff = current - lastClick

        // 无论是否登录，短时间内（如 500ms）这段代码只会往下走一次
        if (diff < CLICK_INTERVAL_TIME) {
            when (tabId) {
                R.id.tabMine -> {
                    FlowEventBus.postSync(Events.MainTabDoubleClickEvent(Events.MainTabDoubleClickEvent.TAB_MINE))
                }
                R.id.tabHome -> {
                    FlowEventBus.postSync(Events.MainTabDoubleClickEvent(Events.MainTabDoubleClickEvent.TAB_HOME))
                }
                else -> {
                    FlowEventBus.postSync(Events.MainTabDoubleClickEvent(Events.MainTabDoubleClickEvent.TAB_WORKS))
                }
            }
            return // 连续点击直接拦截，不再执行拦截器 check
        }

        // 只有过了防抖期，才更新时间，保证下一次连击被拦截
        clicks.put(tabId, current)

        if (interceptor?.invoke() == true) {
            return
        }

        // 4. 【执行切换】
        action()
    }

    // 添加常亮状态管理
    private var isScreenOnKeep = false

    /**
     * 设置屏幕常亮状态
     */
    private fun setKeepScreenOn(keepOn: Boolean) {
        if (keepOn == isScreenOnKeep) return

        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            isScreenOnKeep = true
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            isScreenOnKeep = false
        }
    }

    /**
     * 跳转到首页页面
     */
    fun toHome() {
        binding.mrg.check(R.id.rbHome)
    }


    override fun setTranslucentStatus() {
        setupTranslucentStatus(false, getThemeColor(R.attr.bgColor2))
    }

    private fun checkUpgrade() {
        lifecycleScope.launchNoCrash {
            delay(1000)
            upgradeManager.upgrade(false)
        }
    }

    private fun observeEvents() {
        mRealVM.firstRechargeCache.observe(this) { data ->
            data?.let {
                EventManager.onAppTrack(AppEvent.SF_VIEW_HOME_NEWCOMER_GIFTPACK_PAGE)
                FirstRechargeDialog.show(FirstRechargeDialog.SCENE_HOME, it)?.apply {
                    setOnDismissListener {
                        this@MainActivity.mRealVM.isFirstRechargeLogicActive = false
                    }
                }
                Config.get().markFirstRechargeShown(FirstRechargeDialog.SCENE_HOME)
            }
        }
        observeEvents {
            on<Events.ApkRenewal> { event ->
                //用户日活记录
                mRealVM.daily()
                if (event.apkRenewal) {
                    //需要更新apk，那其他弹框就不需要弹了
                    return@on
                }
            }
            on<Events.MainTabEvent> { event ->
                when (event.type) {
                    Events.MainTabEvent.TAB_HOME -> {
                        toHome()
                    }

                    Events.MainTabEvent.TAB_MY_CREATIONS -> {
                        binding.mrg.check(R.id.rbWorks)
                    }

                    Events.MainTabEvent.TAB_MINE -> {
                        binding.mrg.check(R.id.rbMine)
                    }
                }
            }
            on<Events.UserChangedEvent> { event ->
                if (event.eventType == Events.UserChangedEvent.TOKEN_EXPIRED) {
                    AppNavigator.forceLogoutAndRestart()
                } else {
                    mRealVM.fetchUserInfo()
                    if (User.get().isLogged) {
                        //displayPopUpBox(type = MainBulletFrameEnum.COMPLETE_PERSONAL_INFO)
                    } else {
                        lifecycleScope.launch {
                            delay(100)
                            if (isActive) {
                                toHome()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 弹框管理
     * @param type MainBulletFrameEnum 弹框类型
     * @param nextStep Boolean 是否需要下一步
     * @param start Boolean true 首次启动的
     */
    private fun displayPopUpBox(
        type: MainBulletFrameEnum,
        nextStep: Boolean = true,
        start: Boolean = false
    ) {
        when (type) {
            MainBulletFrameEnum.DEFAULT -> return

            MainBulletFrameEnum.COMPLETE_PERSONAL_INFO -> {
            }

            MainBulletFrameEnum.BIND_PHONE_NUMBER_POP_UP_BOX -> {
                //绑定手机号
            }
        }
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
            if (mRealVM.showTab.get() == 1 && mRealVM.isEditing.get()) {
                //在我的页面如果是编辑状态，返回应该是修改状态
                mRealVM.setIsEditing?.invoke(true)
                return true
            }
            if (System.currentTimeMillis() - exitTime > 2000) {
                getString(R.string.press_again_to_exit).showToast()
                exitTime = System.currentTimeMillis()
            } else {
                finishAffinity()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


    fun onAgreePolicy() {
        Config.get().markAppOpenedWithNotify()
        App.instance.initSdkAfterAgreeLicense()
        //遍历查询是否初始化完成
        waitApiSecurityInit()
    }
}
package com.kuaigeglobal.epnova

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import com.alibaba.android.alpha.Task
import com.blankj.utilcode.util.ProcessUtils
import com.blankj.utilcode.util.Utils
import com.github.gzuliyujiang.oaid.DeviceID
import com.google.android.gms.ads.MobileAds
import com.kuaige.oss.OssType
import com.kuaigeglobal.epnova.common.Cache
import com.kuaigeglobal.epnova.common.SP
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.manager.AdStrategyManager
import com.kuaigeglobal.epnova.core.manager.AppsFlyerManager
import com.kuaigeglobal.epnova.core.manager.OssManager
import com.kuaigeglobal.epnova.core.manager.PlaceManager
import com.kuaigeglobal.epnova.feature.splash.SplashActivity
import com.kuaigeglobal.epnova.kit.SignUtils
import com.kuaigeglobal.epnova.widget.CommonDialog
import com.kuaigeglobal.epnova.widget.CustomizedRefreshHeader
import com.kuaigeglobal.epnova.widget.LoadingDialog2
import com.kuaigeglobal.epnova.widget.MyLoadViewFactory
import com.nhsd.basic.BaseApp
import com.nhsd.basic.manager.LoadingConfig
import com.nhsd.basic.utils.Logger
import com.nhsd.mvc.MVCHelper
import com.nhsd.permisson.DefaultRationaleFactory
import com.nhsd.permisson.PermissionX
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.LinkedList
import java.util.Locale
import java.util.Random

class App : BaseApp() {

    private lateinit var mainThreadHandler: Handler

    // 使用 lateinit 延迟初始化，避免空安全检查
    lateinit var config: Config
        private set
    lateinit var user: User
        private set

    var cache: Cache? = null
        private set

    lateinit var placeManager: PlaceManager
        private set

    var defaultProvince: String? = null
    var defaultCity: String? = null

    var ran: String? = null
        private set

    private lateinit var random: Random

    var isApiSecurityInit: Boolean = false
        private set

    private lateinit var appStatusTracker: AppStatusTracker

    // 使用 object 表达式替代匿名内部类
    private val sdkConfigTask = object : Task("sdkConfigTask") {
        override fun run() {
            initSdkAfterAgreeLicense()
            fileManager.providerName = "fileprovider"

            if (ProcessUtils.isMainProcess()) {
                val releaseEnv = listOf("production", "preview")
                val releaseDomain = listOf("http://120.24.240.28:9010", "https://kuaichupianjk.kuaigeai.cn")

                val apiSecurityStartTime = System.currentTimeMillis()
                // 注意：Kotlin 中访问静态/伴生对象通常直接用类名，或者 companion 里的常量
                // 假设 BuildConfig.FLAVOR_env 是生成的静态常量
//                APISecurity.init(
//                    this@App,
//                    releaseEnv.contains(BuildConfig.FLAVOR_env) || releaseDomain.contains(Config.get().baseUrlCustom)
//                )
                isApiSecurityInit = true
                Logger.i("starttime,api security use =" + (System.currentTimeMillis() - apiSecurityStartTime))
                config.initAppConfig()
            }

            PermissionX.setRationaleFactory(object : DefaultRationaleFactory() {
                var positiveCheck = false

                @NonNull
                override fun createDeniedForeverRationale(
                    @NonNull context: Context,
                    @NonNull permissions: Array<String>,
                    @NonNull positive: Runnable,
                    @NonNull negative: Runnable
                ): Dialog {
                    val namesString = permissionNameStr(permissions, context)
                    positiveCheck = false
                    val commonDialog = CommonDialog(
                        context,
                        context.getString(
                            R.string.permissionx_default_denied_forever_rationale_message,
                            namesString
                        )
                    )
                    commonDialog.setPositiveButtonText(getString(R.string.allow))
                    commonDialog.onPositiveButtonClickListener = { _ ->
                        positive.run()
                        positiveCheck = true
                    }
                    commonDialog.setOnDismissListener { _ ->
                        if (!positiveCheck) {
                            negative.run()
                        }
                    }
                    return commonDialog
                }
            })
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

//                StrictMode.setVmPolicy(
//            StrictMode.VmPolicy.Builder()
//                .detectActivityLeaks()//Activity是否内存泄漏
//                .detectLeakedSqlLiteObjects()//数据库是否未关闭
//                .detectLeakedClosableObjects()//文件是否未关闭
//                .setClassInstanceLimit(MainActivity::class.java,1) //某个类在内存中实例上限
//                .detectLeakedRegistrationObjects()//对象是否被正确关闭
//                .penaltyLog()//打印日志，可以通过 StrictMode 来过滤
//                //.penaltyDeath()//直接Crash掉当前应用程序
//                .build()
//        )

        val start = System.currentTimeMillis()
        Logger.i("App_", "starttime:$start")
        // 使用属性访问 isDebug
        Logger.setDebug(isDebug)

        SP.init(this)
        MMKV.initialize(this@App)
        // AppsFlyer 初始化（subscribeForDeepLink 必须在 start 前注册）
        AppsFlyerManager.init(this)
        config = Config()
        config.init(this@App)
        user = User()
        user.init(this@App)
        appStatusTracker = AppStatusTracker()
        registerActivityLifecycleCallbacks(appStatusTracker)

        mainThreadHandler = Handler(Looper.getMainLooper())

        val objectId = AdStrategyManager.getDefaultObjectId()
        config.storeObjectId(objectId)

        MVCHelper.setLoadViewFactory(MyLoadViewFactory())

        placeManager = PlaceManager()
        placeManager.asynInitRegion()

        random = Random()
        ran = "100000"

        Logger.i("App_", "starttime use:" + (System.currentTimeMillis() - start))

        builder.add(sdkConfigTask)
        isApiSecurityInit = false
        startTasks(builder)

        // SAM 转换：直接使用 Lambda
        LoadingConfig.setFactory { LoadingDialog2() }
        OssManager.init(ossType = OssType.ALIYUN, ossFileNameProvider = { ossPathName, suffix ->
            val timestamp = System.currentTimeMillis()
            "ENApp/$ossPathName/${User.get().userId}/0$timestamp.$suffix"
        })
        // 初始化加载刷新控件
        initSmartRefreshLayoutView()
    }

    class AppStatusTracker : ActivityLifecycleCallbacks {
        /**
         * 当前是否有activity在运行
         */
        var activityCount = 0

        /**
         * 是否是在后台
         */
        var isBackground = false
            private set

        private var topActivityRef: WeakReference<Activity>? = null
        private val activityStacks = LinkedList<String>()

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            instance.updateActivityLanguage(activity)
            topActivityRef?.clear()
            activityStacks.addLast(activity.javaClass.name)
            topActivityRef = WeakReference(activity)
            Logger.i("activities=$activityStacks")
        }

        override fun onActivityStarted(activity: Activity) {
            Logger.i(activity.javaClass.name)
            activityCount++
            if (activity is SplashActivity) {
                isColdStart = false // 冷启动状态结束
            }
        }

        override fun onActivityResumed(activity: Activity) {
            isBackground = false
        }

        override fun onActivityPaused(activity: Activity) {
        }

        override fun onActivityStopped(activity: Activity) {
            activityCount--
            if (activityCount == 0) {
                Logger.i("AppStatusTracker", "run in background")
                isBackground = true
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (activityStacks.last == activity.javaClass.name) {
                activityStacks.removeLast()
            } else {
                activityStacks.remove(activity.javaClass.name)
            }
            Logger.i("activities=$activityStacks")
        }

        val topActivity: Activity?
            get() = topActivityRef?.get()
    }

    private fun updateActivityLanguage(activity: Activity) {
        val lang = config.getSavedLanguage() // 从 SP 读取 "th", "id" 等
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val resources = activity.resources
        val config = resources.configuration
        config.setLocale(locale)

        // 兼容旧版本和新版本的更新方式
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun isLocalServer(): Boolean {
        return DEV_ENV.contains(BuildConfig.FLAVOR_env)
    }

    fun hasAgree(): Boolean {
        return !Config.get().isFirstOpenApp
    }

    fun resetRan() {
        ran = random.nextInt(100000).toString()
    }

    fun setAlias(id: String) {
    }

    fun deleteAlias() {
    }

    fun post(r: Runnable) {
        mainThreadHandler.post(r)
    }

    fun postDelay(r: Runnable, delayMillis: Int) {
        mainThreadHandler.postDelayed(r, delayMillis.toLong())
    }

    override fun isDebug(): Boolean {
       return isLocalServer()
    }

    fun initSdkAfterAgreeLicense() {
        if (hasAgree()) {
            if (User.get().isVisitor) {
                Global.guestLogin({
                    User.get().updateFrom(it)
                    SignUtils.signIn()
                })
            }
            Utils.init(this)
            DeviceID.register(this)
            // AppsFlyer 开始数据采集（用户已同意隐私协议）
            AppsFlyerManager.start(this)
            // 初始化 AdMob SDK（在主进程中执行，异步不阻塞启动）
            if (ProcessUtils.isMainProcess()) {
                ioScope.launch {
                    MobileAds.initialize(instance){
                    }
                }
            }
        }
    }

    /**
     * 初始化加载刷新控件
     */
    private fun initSmartRefreshLayoutView() {
        ClassicsFooter.REFRESH_FOOTER_NOTHING = resources.getString(R.string.no_more_data)
        ClassicsFooter.REFRESH_FOOTER_FINISH = ""
        // 刷新头
        SmartRefreshLayout.setDefaultRefreshHeaderCreator { context, _ -> CustomizedRefreshHeader(context) }
        // 加载尾
        SmartRefreshLayout.setDefaultRefreshFooterCreator { context, _ ->
            val footer = ClassicsFooter(context)
            footer.setTextSizeTitle(12f)
            footer
        }
    }

    companion object {
        @JvmStatic
        lateinit var instance: App
        const val TAG = "Epnova_APP"

        // 相当于 Java 的 static boolean
        var isColdStart = true

        private val DEV_ENV = listOf("develop", "preview")
    }
}
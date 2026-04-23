package com.kuaigeglobal.epnova.feature.web

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.view.isVisible
import com.blankj.utilcode.util.GsonUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.BuildConfig
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.downloadFile
import com.kuaigeglobal.epnova.common.ext.selectSinglePhotoFromAlbums
import com.kuaigeglobal.epnova.common.jsbridge.WebViewJavascriptBridge
import com.kuaigeglobal.epnova.common.jsbridge.data.DownLoadImageData
import com.kuaigeglobal.epnova.common.permissions.PermissionsUtils
import com.kuaigeglobal.epnova.common.permissions.requestImages2VideosPermissions
import com.kuaigeglobal.epnova.common.permissions.requestRead2WritePermissions
import com.kuaigeglobal.epnova.common.permissions.requestRxPermissions
import com.kuaigeglobal.epnova.common.utils.ImageSaveHelper
import com.kuaigeglobal.epnova.common.utils.IntentUtils
import com.kuaigeglobal.epnova.common.utils.WXUtils
import com.kuaigeglobal.epnova.core.CookieUtils
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.onUserStatusChanged
import com.kuaigeglobal.epnova.core.ext.openWithBrowser
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.model.H5PayInfo
import com.kuaigeglobal.epnova.core.model.PermissionEntity
import com.kuaigeglobal.epnova.core.model.PreviewImageEntity
import com.kuaigeglobal.epnova.core.model.ResultEntity
import com.kuaigeglobal.epnova.core.model.UriEntity
import com.kuaigeglobal.epnova.core.network.ObjectTypeAdapter
import com.kuaigeglobal.epnova.core.network.apis.AppHeader
import com.kuaigeglobal.epnova.databinding.FragmentBrowserBinding
import com.kuaigeglobal.epnova.feature.PhotoPagerActivity
import com.kuaigeglobal.epnova.feature.recharge.PaySuccessDialog
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity.Companion.launch
import com.kuaigeglobal.epnova.router.AppNavigator
import com.kuaigeglobal.epnova.router.AppRouter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.Session
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.paddingStatusBar
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import com.nhsd.mattise.Matisse
import org.json.JSONObject

class BrowserFragment : BaseViewModelFragment<FragmentBrowserBinding, BrowserViewModel>() {

    companion object {
        private const val REQUEST_CODE_PHOTO_PICKER = 0x999
        private val TAG: String = BrowserFragment::class.java.simpleName

        fun newInstance(url: String?): BrowserFragment {
            var url = url
            Logger.i("http", "url=$url")
            if (url != null && !url.contains("ran=")) {
                url = if (url.contains("?")) {
                    url + "&ran=" + App.instance.ran
                } else {
                    url + "?ran=" + App.instance.ran
                }
            }
            val args = Bundle()
            args.putString(Constants.Extra.URL, url)
            val browserFragment = BrowserFragment()
            browserFragment.arguments = args
            Logger.d(TAG, "url:$url")
            return browserFragment
        }
    }

    //private val mShareManager by lazy { ShareManager(mActivity) }
    private var bridge: WebViewJavascriptBridge? = null
    private var gson: Gson? = null

    private var mUploadCallbackBelow: ValueCallback<Uri>? = null
    private var mUploadCallbackAboveL: ValueCallback<Array<Uri?>?>? = null

    private val mWebViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            mRealVM.isLoading.value = false
            binding.viewAnimator.displayedChild = if (mRealVM.isLoadError.value == true) 1 else 0
            binding.progressbar.isVisible = false
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if (url.isNullOrEmpty()) return true
            if (url.contains("newView")) {
                launch(mActivity, url, mRealVM.interceptBack)
                return true
            }
            return !url.startsWith("http") || super.shouldOverrideUrlLoading(view, url)
        }

        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
            mRealVM.isLoadError.value = true
            mRealVM.isLoading.value = false
            binding.viewAnimator.displayedChild = 1
            binding.progressbar.isVisible = false
            binding.circularButton.isVisible = true
            binding.netPromptView.setText(R.string.page_network_error)
        }

        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            handler?.cancel()
        }
    }

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private val mWebChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            if (newProgress == 100) {
                binding.progressbar.isVisible = false
            } else {
                if (!binding.progressbar.isVisible) binding.progressbar.isVisible = true
                binding.progressbar.progress = newProgress
            }
            super.onProgressChanged(view, newProgress)
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            if (Session.getInstance().appName != title && !title.isNullOrEmpty() && view?.url?.contains(title) == false && mRealVM.title?.isNotEmpty() == true) {
                mRealVM.mToolbarVM.mTitle.set(title)
            }
        }

        override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri?>?>?, fileChooserParams: FileChooserParams?): Boolean {
            mUploadCallbackAboveL = filePathCallback
            choosePic()
            return true
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            if (customView != null) {
                callback?.onCustomViewHidden()
                return
            }

            customView = view
            customViewCallback = callback

            // 1. 隐藏 WebView，显示视频容器
            binding.webview.visibility = View.GONE
            binding.videoContainer.visibility = View.VISIBLE

            // 2. 将视频视图添加到容器中
            binding.videoContainer.addView(view)

            // 3. (可选) 强制横屏
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        // 退出全屏时触发
        override fun onHideCustomView() {
            if (customView == null) return

            // 1. 移除视频视图
            binding.videoContainer.removeView(customView)
            binding.videoContainer.visibility = View.GONE

            // 2. 重新显示 WebView
            binding.webview.visibility = View.VISIBLE

            customView = null
            customViewCallback?.onCustomViewHidden()

            // 3. 恢复竖屏
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        //mRealVM.shareManagerFactory = this
        //mRealVM.getFragmentManager = { childFragmentManager }
        initUI()
        setupObservers()
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun initUI() {
        binding.progressLayout.paddingStatusBar()

        if (mRealVM.darkStyle) {
            binding.root.setBackgroundColor(resources.getColor(R.color.bg_color_1_dark))
        }

        binding.circularButton.setOnClickListener {
            if (mRealVM.isLoading.value != true) {
                mRealVM.isLoadError.value = false
                mRealVM.isLoading.value = true
                binding.webview.loadUrl("javascript:window.location.reload(true)")
                binding.progressbar.isVisible = true
                binding.circularButton.isVisible = false
            }
        }

        binding.webview.apply {
            settings.javaScriptEnabled = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }

            if (App.instance.hasAgree()) {
                val userAgent =
                    "${settings.userAgentString} Networking Gang/${Session.getInstance().versionName}"
                settings.apply {
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    userAgentString = userAgent
                    cacheMode = WebSettings.LOAD_DEFAULT
                    domStorageEnabled = true
                    allowFileAccess = true
                }
            }

            setBackgroundColor(0)
            background?.alpha = 0

            setDownloadListener { url, _, _, _, _ ->
                requestRead2WritePermissions(
                    R.string.sdcard_permission_description
                ) { allGranted ->
                    if (!allGranted) return@requestRead2WritePermissions
                    mActivity.downloadFile(url)
                }
            }
        }

        binding.webview.webChromeClient = mWebChromeClient
        bridge = WebViewJavascriptBridge(
            mActivity,
            binding.webview,
            UserServerHandler()
        ).apply {
            setCustomWebViewClient(mWebViewClient)
        }

        if (App.instance.hasAgree()) {
            mRealVM.url?.let { CookieUtils.syncCookie(it, "userInfo", User.get().userInfoStr) }
        }

        loadingUrl()
        registerHandle()
    }

    private fun setupObservers() {
//        mRealVM.showWaitingDialog.observe(this) { trackId ->
//            trackId?.let {
//                WaitingDialog.newInstance(it).apply {
//                    setOnDismissListener { /*ignore*/ }
//                    show(childFragmentManager)
//                }
//            }
//        }

        mRealVM.openWebUrl.observe(this) { url ->
            url?.openWithBrowser()
        }

        mRealVM.payStatusCallback.observe(this) { info ->
            info?.let { bridge?.callHandler("payStatusCallback", GsonUtils.toJson(it)) }
        }
        observeEvents {
            onUserStatusChanged {
                mRealVM.url?.let { CookieUtils.syncCookie(it, "userInfo", User.get().userInfoStr) }
                binding.webview.reload()
            }
        }
    }

//    override fun onStart(share_media: SHARE_MEDIA?) {}
//    override fun onResult(share_media: SHARE_MEDIA?) = "分享成功！".showToast()
//    override fun onError(share_media: SHARE_MEDIA?, throwable: Throwable?) = "分享失败！".showToast()
//    override fun onCancel(share_media: SHARE_MEDIA?) {}

    inner class UserServerHandler : WebViewJavascriptBridge.WVJBHandler {
        override fun handle(data: String?, jsCallback: WebViewJavascriptBridge.WVJBResponseCallback?) {
            Logger.d(TAG, "Received message from javascript: $data")
            jsCallback?.callback("Java said:Right back atcha")
            bridge?.send("I expect a response!") { responseData ->
                Logger.d("test", "Got response! $responseData")
            }
            bridge?.send("Hi")
        }
    }

    private fun registerHandle() {
        bridge?.apply {
            registerHandler("login") { _, _ -> AppNavigator.checkLoginIntercepted(true) }
            registerHandler("city") { _, jsCallback ->
                if (!IntentUtils.isLocationServiceEnable(mActivity)) {
                    IntentUtils.startOpenGPSSetting(mActivity)
                    jsCallback?.callback("")
                    return@registerHandler
                }
                requestLocationPermission(jsCallback, false)
            }
            registerHandler("saveImage") { data, _ ->
                val src = Gson().fromJson(data, DownLoadImageData::class.java)
                if (!TextUtils.isEmpty(src.src)) {
                    cacheImage(src.src)
                } else {
                    getString(R.string.download_image_failed).showToast()
                }
            }
//            registerHandler("webShare") { data, _ ->
//                val webEntity = Gson().fromJson(data, WebEntity::class.java)
//                WechatShareDialog.shareWebUrl(webEntity)
//            }
            registerHandler("schemeJump") { data, _ ->
                val uriEntity = Gson().fromJson(data, UriEntity::class.java)
                AppRouter.jump(uriEntity.uri)
            }
            registerHandler("getAppVersion") { _, jsCallback ->
                jsCallback?.callback(BuildConfig.VERSION_NAME)
            }
            registerHandler("getAppHeader") { _, jsCallback ->
                if (gson == null) {
                    gson = GsonBuilder().registerTypeAdapter(
                        Object::class.java,
                        ObjectTypeAdapter()
                    ).create()
                }
                jsCallback?.callback(gson!!.toJson(AppHeader()))
            }
            registerHandler("previewImage") { data, _ ->
                val entity = Gson().fromJson(data, PreviewImageEntity::class.java)
                PhotoPagerActivity.launch(
                    mActivity,
                    ArrayList(entity.urls),
                    entity.current
                )
            }
            registerHandler("openPermission") { data, jsCallback ->
                val entity = Gson().fromJson(data, PermissionEntity::class.java)
                when (entity.name) {
                    "location" -> {
                        requestRxPermissions(
                            getString(R.string.location_permission_description), Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) { granted ->
                            val result =
                                ResultEntity(if (granted) ResultEntity.SUCCESS else ResultEntity.FAILED)
                            jsCallback?.callback(Gson().toJson(result))
                        }
                    }

                    "album" -> requestImages2VideosPermissions(
                        R.string.sdcard_permission_description.getResString(),
                        withVideo = true,
                        openSelect = true
                    ) { granted ->
                        val result =
                            ResultEntity(if (granted) ResultEntity.SUCCESS else ResultEntity.FAILED)
                        jsCallback?.callback(Gson().toJson(result))
                    }

                    "camera" -> {
                        requestRxPermissions(
                            getString(R.string.sdcard_permission_description), *PermissionsUtils.getCameraPermission()
                        ) { granted ->
                            val result =
                                ResultEntity(if (granted) ResultEntity.SUCCESS else ResultEntity.FAILED)
                            jsCallback?.callback(Gson().toJson(result))
                        }
                    }
                }
            }
//            registerHandler("getCityCode") { _, jsCallback ->
//                val result = ResultEntity(ResultEntity.SUCCESS, User.get().cityCode)
//                jsCallback?.callback(toJson(result))
//            }
//            registerHandler("getLocation") { data, jsCallback ->
//                val getLocationEntity = Gson().fromJson(data, GetLocationEntity::class.java)
//                if (getLocationEntity.isOpenPermission) {
//                    if (!IntentUtils.isLocationServiceEnable(mActivity)) {
//                        IntentUtils.startOpenGPSSetting(mActivity)
//                        jsCallback?.callback(toJson(ResultEntity(ResultEntity.FAILED)))
//                        return@registerHandler
//                    }
//                    requestLocationPermission(jsCallback, true)
//                } else {
//                    val result = ResultEntity(
//                        ResultEntity.SUCCESS,
//                        "${User.get().longitude},${User.get().latitude}"
//                    )
//                    jsCallback?.callback(toJson(result))
//                }
//            }
//            registerHandler("startComposeChristmasMusic") { data, jsCallback ->
//                val contentData = Gson().fromJson(data, ContentData::class.java)
//                contentData?.let { it.content?.let { it1 -> mRealVM.composeChristmasMusic(it1) } }
//            }
//            registerHandler("startComposeDebutMusic") { data, jsCallback ->
//                val stageInfo = Gson().fromJson(data, StageInfo::class.java)
//                mRealVM.composeDebutMusic(stageInfo)
//            }
//            registerHandler("startComposeCreativeMusic") { data, jsCallback ->
//                val creativeBody = Gson().fromJson(data, CreativeContentBody::class.java)
//                mRealVM.composeCreativeMusic(creativeBody) {
//                    jsCallback.callback("200")
//                }
//            }
//            registerHandler("inviteUserClick") { data, _ ->
//                val entity = Gson().fromJson(data, InviteUrlEntity::class.java)
//                mShareManager.doShare(
//                    "只需一句话，为你生成歌",
//                    "来快歌创作你的专属歌曲，还有各种写歌现金奖励等你哦",
//                    getBitmap(R.mipmap.ic_launcher),
//                    R.mipmap.ic_launcher,
//                    entity.inviteUrl,
//                    SHARE_MEDIA.WEIXIN,
//                    this@BrowserFragment
//                )
//                EventManager.onAppEvent(
//                    AppEvent.INVITE_FRIEND_INVITE_BUTTON_CLICK,
//                    null,
//                    "{\"isWeb\":true}"
//                )
//            }
            registerHandler("webPay") { data, jsCallback ->
                try {
                    mRealVM.orderNo = null
                    val info = GsonUtils.fromJson(data, H5PayInfo::class.java)
                    mRealVM.startPay(info)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            registerHandler("openWebUrl") { data, _ ->
                try {
                    val uriEntity = GsonUtils.fromJson(data, UriEntity::class.java)
                    uriEntity.uri.openWithBrowser()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            //跳转微信客服
            //{
            //customerServiceJmpUrl: 跳转地址，
            //enterpriseId： id
            //}
            registerHandler("openWeChatService") { data, _ ->
                try {
                    val json = JSONObject(data)
                    WXUtils.customerServiceChat(activity, json.optString("customerServiceJmpUrl"), json.optString("enterpriseId"))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            registerHandler("showCustomerGuideDialog") { data, _ ->
                PaySuccessDialog.checkAndShow(PaySuccessDialog.SCENE_OTHER)
            }

            //埋点上报
            // {
            //  "event_name": 埋点名称,
            //  "params": 埋点参数（json）
            //}
            registerHandler("trackEvent") { data, _ ->
                try {
                    val json = JSONObject(data)
                    val code = json.getString("event_name")
                    val params = json.optJSONObject("params")?.let {
                        val map = mutableMapOf<String, String?>()
                        it.keys().forEach { key -> map[key] = it.optString(key) }
                        map
                    }
                    EventManager.onAppTrack(code, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun <T> toJson(result: T): String = Gson().toJson(result)

    private fun requestLocationPermission(
        jsCallback: WebViewJavascriptBridge.WVJBResponseCallback?,
        useJson: Boolean
    ) {
        requestRxPermissions(
            getString(R.string.location_permission_description), Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) { granted ->
            if (granted) {
                // TODO: Start location if needed
            } else {
                if (useJson) {
                    jsCallback?.callback(toJson(ResultEntity(ResultEntity.FAILED)))
                } else {
                    jsCallback?.callback("")
                }
            }
        }
    }

    private fun cacheImage(url: String) {
        ImageSaveHelper.saveImage(mActivity, url)
    }

    private fun choosePic() {
        requestImages2VideosPermissions(
            getString(R.string.sdcard_permission_description),
            withVideo = true,
            openSelect = true
        ) { granted ->
            if (granted) {
                selectSinglePhotoFromAlbums(
                    fragment = this@BrowserFragment,
                    selectedUri = null,
                    requestCode = REQUEST_CODE_PHOTO_PICKER,
                    capture = true,
                    useEmbeddedCamera = true
                )
            }
        }
    }

    private fun loadingUrl() {
        mRealVM.url?.let {
            binding.webview.loadUrl(it)
            mRealVM.isLoading.value = true
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_PHOTO_PICKER -> handlePhotoPickerResult(resultCode, data)
        }
//        ShareManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun handlePhotoPickerResult(resultCode: Int, data: Intent?) {
        if (mUploadCallbackBelow == null && mUploadCallbackAboveL == null) return

        if (mUploadCallbackBelow != null) {
            val result = if (resultCode != RESULT_OK) null else Matisse.obtainResult(data)
            mUploadCallbackBelow?.onReceiveValue(result?.getOrNull(0))
            mUploadCallbackBelow = null
        } else if (mUploadCallbackAboveL != null) {
            val result = if (resultCode != RESULT_OK) null else Matisse.obtainResult(data)
            mUploadCallbackAboveL?.onReceiveValue(result?.toTypedArray())
            mUploadCallbackAboveL = null
        }
    }


    override fun onResume() {
        super.onResume()
        binding.webview.apply {
            onResume()
            resumeTimers()
            isVisible = false
            post { isVisible = true }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.webview.apply {
            onPause()
            pauseTimers()
        }
    }

    override fun onDestroy() {
        binding.webview.destroy()
        super.onDestroy()
    }

//    override fun getShareManager(): ShareManager {
//        return mShareManager
//    }
//
//    override fun getShareListener(): UMShareListener? {
//        return this
//    }
//
//    override fun getStatusHolder(): DownloadingViewModel.DownloadStatusHolder? {
//        return if (isActive) mRealVM else null
//    }

}
package com.kuaigeglobal.epnova.core

import android.os.Build
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import com.kuaigeglobal.epnova.common.utils.Utils
import java.net.MalformedURLException
import java.net.URL
import java.util.regex.Pattern

object CookieUtils {


    @JvmStatic
    fun syncCookie(url: String?, key: String?, value: String?) {
        val cookies = HashMap<String?, String?>()
        cookies[key] = value
        syncCookie(url, cookies)
    }

    /**
     * 同步cookie
     *
     * @param urlStr     地址
     * @param cookieList 需要添加的Cookie值,以键值对的方式:key=value
     */
    @JvmStatic
    private fun syncCookie(urlStr: String?, cookieList: java.util.HashMap<String?, String?>?) {
        var url: URL? = null
        try {
            url = URL(urlStr)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            return
        }
        var domainList = Config.get().appConfig.clientLegalH5Domain
        if (Utils.isEmpty(domainList)) {
            val domain = getDomainName(url)
            domainList = arrayListOf()
            domainList.add(domain)
        }

        //        CookieSyncManager.createInstance(App.getInstance());
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        if (cookieList != null && !cookieList.isEmpty()) {
            for (domain in domainList!!) {
                for (cookie in cookieList.entries) {
                    cookieManager.setCookie(domain, cookie.key.toString() + "=" + cookie.value)
                }
            }
        }
        //        cookieManager.setCookie(url, "Domain=.zyb.com");
//        cookieManager.setCookie(url, "Path=/");
//        String cookies = cookieManager.getCookie(domain);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.flush()
        } else {
            CookieSyncManager.getInstance().sync()
        }
    }

    private val IP_PATTERN = Pattern.compile("""(\d{1,3}\.){3}(\d{1,3})""")

    private val publicSuffixSet = setOf(
        "com", "org", "net", "gov", "edu", "co", "tv", "mobi", "info", "asia", "xxx", "onion",
        "cn", "com.cn", "edu.cn", "gov.cn", "net.cn", "org.cn",
        "jp", "kr", "tw", "com.hk", "hk", "org.hk", "se", "com.se", "org.se"
    )

    fun getDomainName(url: URL): String {
        var host = url.host
        if (host.endsWith(".")) {
            host = host.substring(0, host.length - 1)
        }
        if (IP_PATTERN.matcher(host).matches()) {
            return host
        }

        var candidate = host
        while (true) {
            val index = candidate.indexOf('.')
            if (index == -1) break
            candidate = candidate.substring(index + 1)
            if (candidate in publicSuffixSet) {
                return host.substring(host.lastIndexOf(candidate))
            }
        }
        return candidate
    }

}
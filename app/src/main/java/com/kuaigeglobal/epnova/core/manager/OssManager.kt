package com.kuaigeglobal.epnova.core.manager

import com.kuaige.oss.OssConfigProvider
import com.kuaige.oss.OssStsConfig
import com.kuaige.oss.OssType

/**
 * 使用TosV2时主moudle注意脱糖 不然低版本Android会崩
 *
 *   compileOptions {
 *         coreLibraryDesugaringEnabled true
 *     }
 *
 *     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.1.4'
 */
object OssManager {

    const val TOS_NET_IMAGE_URL: String = "https://kuaige.tos-cn-guangzhou.volces.com/" //OSS 图片基础地址（桶域名:kuaige.tos-cn-guangzhou.volces.com）
    const val ALIYUN_NET_IMAGE_URL = "https://kuaichupian.oss-cn-shenzhen.aliyuncs.com/" //OSS 图片基础地址

    @JvmStatic
    fun init(ossType: OssType, ossFileNameProvider: (ossPathName: String,suffix: String) -> String) {
        val ossConfig = when (ossType) {
            OssType.TOS_V2 -> {
                getTosOssStsConfig()
            }
            OssType.ALIYUN -> {
                getALiYunOssStsConfig()
            }
            else -> {
                getTencentOssStsConfig()
            }
        }
        OssConfigProvider.init(ossType, ossConfig, ossFileNameProvider)
    }

    private fun getTosOssStsConfig(): OssStsConfig {
        return OssStsConfig(
            accessKeyId = "AKLTYzFjZjJhMDQ2YTRkNGZiMDhmN2UyNGJkYzE4MzM0ODY",
            accessKeySecret = "TWpZMlpUbGlOVGxtWldabE5HUm1PR0ZpTldKa1pHRmhPVEEzTm1Oall6VQ==",
            bucket = "kuaige",
            endpoint = "tos-cn-guangzhou.volces.com",
            region = "cn-guangzhou",
            netImageUrl = TOS_NET_IMAGE_URL,
        )
    }

    private fun getALiYunOssStsConfig(): OssStsConfig {
        return OssStsConfig(
            accessKeyId = "LTAI5tBeSRMZvDBq9xKFJsnQ",
            accessKeySecret = "c5er42rEKyENOoGdRU6mZ41kyEoVQx",
            bucket = "kuaichupian",
            endpoint = "oss-cn-shenzhen.aliyuncs.com",
            region = null,
            netImageUrl = ALIYUN_NET_IMAGE_URL,
        )
    }

    private fun getTencentOssStsConfig(): OssStsConfig {
        return OssStsConfig(
            accessKeyId = "AKIDXwBF6lCSWN1ReeR1FMHC2ulSxHNGyTmG",
            accessKeySecret = "LSM3Xr44rUuDIA1jOuELFvOdfWIuUr0w",
            bucket = "jinguo-1357264972",
            endpoint = "",
            region = "ap-guangzhou",
            netImageUrl = "",
        )
    }
}
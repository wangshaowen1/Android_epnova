package com.kuaigeglobal.epnova.core.manager
import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaDrm
import android.media.NotProvisionedException
import android.media.UnsupportedSchemeException
import android.provider.Settings
import android.util.Log
import com.google.android.gms.appset.AppSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

/**
 * 出海（海外）专用的设备指纹向量数据模型
 */
data class OverseasDeviceFingerprint(
    val drmId: String?,      // 硬件级：防重装、防刷机 (核心锚点)
    val androidId: String?,  // 系统级：同签名防重装 (辅助锚点)
    val appSetId: String?,   // 生态级：Google 官方合规 ID，同开发者账号下共享不变 (出海主力)
    val cloudUuid: String?   // 应用级：依赖 Google Drive 的 Auto Backup 机制 (兜底锚点)
)

object OverseasFingerprintManager {

    private const val TAG = "OverseasFingerprint"
    // Widevine DRM 的标准 UUID
    private val WIDEVINE_UUID = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)

    // 内存缓存，避免 App 生命周期内重复进行耗时的底层通信
    @Volatile
    private var cachedFingerprint: OverseasDeviceFingerprint? = null

    // 协程互斥锁，确保在多协程并发请求时，底层的硬件读取逻辑只执行一次
    private val mutex = Mutex()

    /**
     * 获取设备指纹向量（挂起函数）
     * [安全保证]: 内部已自动切换到 IO 线程池，可放心地在 UI 线程（Dispatchers.Main）中直接调用
     */
    suspend fun getFingerprint(context: Context): OverseasDeviceFingerprint = withContext(Dispatchers.IO) {
        // [第 1 层拦截]：如果缓存已存在，直接返回（无锁读取，性能最高）
        cachedFingerprint?.let {
            Log.d(TAG, "命中内存缓存，直接返回指纹向量")
            return@withContext it
        }

        // 加锁，挂起等待其他可能正在初始化的协程
        mutex.withLock {
            // [第 2 层拦截]：获取到锁后再次检查（双重检查锁模式 DCL），防止排队等待的协程重复执行
            cachedFingerprint?.let { return@withContext it }

            Log.i(TAG, "首次采集指纹，开始读取各项底层硬件与系统标识...")

            // 强制使用 ApplicationContext，彻底阻断传入 Activity 导致的上下文内存泄漏
            val appContext = context.applicationContext

            // 串行/并行采集各个维度的标识符
            val drmId = getMediaDrmId()
            val androidId = getAndroidId(appContext)
            val cloudUuid = getFallbackUUID(appContext)
            val appSetId = getGoogleAppSetId(appContext)

            // 组装最终的指纹向量，对暴露真实硬件/系统的 ID 进行 SHA-256 脱敏处理
            val fingerprint = OverseasDeviceFingerprint(
                drmId = hashSha256(drmId),
                androidId = hashSha256(androidId),
                appSetId = appSetId,   // ASID 原生是 UUID 格式，受 Google 严格隐私保护，无需 Hash
                cloudUuid = cloudUuid  // 客户端随机生成的 UUID，不包含设备信息，无需 Hash
            )

            // 写入内存缓存
            cachedFingerprint = fingerprint
            Log.i(TAG, "海外合规指纹采集完成: $fingerprint")

            return@withContext fingerprint
        }
    }

    /**
     * 采集 Google App Set ID (ASID)
     * 说明：出海应用防刷量、分析归因的最核心官方指标
     */
    private suspend fun getGoogleAppSetId(context: Context): String? {
        return try {
            Log.d(TAG, "--> 正在通过 GMS 获取 AppSetID...")
            val client = AppSet.getClient(context)
            // 使用协程 .await() 优雅处理异步回调，代替老式的 OnCompleteListener
            val appSetIdInfo = client.appSetIdInfo.await()
            Log.d(TAG, "AppSetID 获取成功: ${appSetIdInfo.id}")
            appSetIdInfo.id
        } catch (e: Exception) {
            // 典型场景：海外用户的手机刷了没有 GMS 框架的第三方 ROM，或被强行剥夺了相关权限
            Log.w(TAG, "无法获取 AppSetID (设备可能缺失 Google Play 服务)", e)
            null
        }
    }

    /**
     * 采集硬件底层的 Widevine DRM ID
     * 说明：唯一卸载不掉、甚至极难被恢复出厂设置抹除的物理级 ID
     */
    private fun getMediaDrmId(): String? {
        var mediaDrm: MediaDrm? = null
        return try {
            Log.d(TAG, "--> 尝试初始化硬件 MediaDrm 模块...")
            mediaDrm = MediaDrm(WIDEVINE_UUID)
            val deviceIdBytes = mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)

            if (deviceIdBytes.isEmpty()) {
                Log.w(TAG, "MediaDrm 初始化成功，但硬件返回了空数据 (可能为黑产的改机 ROM)")
                null
            } else {
                Log.d(TAG, "MediaDrm ID 读取成功")
                deviceIdBytes.joinToString("") { "%02x".format(it) }
            }
        } catch (e: UnsupportedSchemeException) {
            Log.w(TAG, "当前设备硬件不支持 Widevine DRM")
            null
        } catch (e: NotProvisionedException) {
            Log.e(TAG, "DRM 证书未初始化或已损坏 (发生了降级漂移)", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "MediaDrm 发生未知底层 C++ 异常", e)
            null
        } finally {
            // 必须强制释放底层 C++ 资源，否则会导致下游其他视频播放器 (如 ExoPlayer) 初始化报错
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    mediaDrm?.close()
                } else {
                    @Suppress("DEPRECATION")
                    mediaDrm?.release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "释放 MediaDrm 资源时发生轻微异常", e)
            }
        }
    }

    /**
     * 采集系统层的 Android ID
     * 说明：Android 8.0 之后，此 ID 绑定了 [设备+开发者签名]。同签名下卸载重装不会变。
     */
    @SuppressLint("HardwareIds")
    private fun getAndroidId(context: Context): String? {
        return try {
            Log.d(TAG, "--> 正在读取系统 Android ID...")
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            // 过滤掉所有厂商都可能出现的已知 "脏数据" (Bug 固件残留)
            if (androidId == "9774d56d682e549c" || androidId.isNullOrBlank()) {
                Log.w(TAG, "读取到无效的脏数据 Android ID ($androidId)，已主动废弃")
                null
            } else {
                Log.d(TAG, "Android ID 读取成功")
                androidId
            }
        } catch (e: Exception) {
            Log.e(TAG, "跨进程读取 Settings 失败", e)
            null
        }
    }

    /**
     * 采集基于本地存储的兜底 UUID
     * 说明：配合 Manifest 的 android:allowBackup="true"，在真实海外网络环境下，由 Google Drive 提供卸载恢复
     */
    private fun getFallbackUUID(context: Context): String? {
        return try {
            Log.d(TAG, "--> 正在检查 SharedPreferences 兜底 UUID...")
            val prefs = context.getSharedPreferences("overseas_device_id_prefs", Context.MODE_PRIVATE)
            var uuid = prefs.getString("fallback_uuid", null)

            if (uuid == null) {
                uuid = UUID.randomUUID().toString()
                prefs.edit().putString("fallback_uuid", uuid).apply()
                Log.d(TAG, "本地无记录，已生成全新兜底 UUID: $uuid")
            } else {
                Log.d(TAG, "从本地存储 (或云端 Auto Backup 恢复) 读取到兜底 UUID: $uuid")
            }
            uuid
        } catch (e: Exception) {
            Log.e(TAG, "读写 SharedPreferences 失败 (可能是极端机型的沙盒权限异常)", e)
            null
        }
    }

    /**
     * 工具方法：SHA-256 单向加密散列
     * 目的：符合 GDPR 等海外隐私法案，坚决不向服务器传输设备原始物理串号
     */
    private fun hashSha256(input: String?): String? {
        if (input.isNullOrBlank()) return null
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "系统缺少 SHA-256 摘要算法", e)
            input // 极端异常时退化为明文 (理论上不应发生)
        }
    }
}